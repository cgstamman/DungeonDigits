package com.example.adventuregamedungeondigits;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Random;

public class AdventureGame extends Application {

    private static final int MAP_SIZE = 10;

    private final Random random = new Random();

    private final Room[][] dungeon = new Room[MAP_SIZE][MAP_SIZE];
    private final Player hero = new Player();

    private int x = 0, y = 0;

    private final GridPane mapGrid = new GridPane();
    private final TextArea log = new TextArea();
    private final Label statsLabel = new Label();

    // ----------------- MAIN -----------------
    public static void main(String[] args) {
        launch(args);
    }

    // ----------------- JavaFX START -----------------
    @Override
    public void start(Stage stage) {

        createDungeon();
        buildMap();

        // --- Navigation buttons
        Button north = new Button("↑");
        Button south = new Button("↓");
        Button west  = new Button("←");
        Button east  = new Button("→");

        north.setOnAction(e -> move(0,-1));
        south.setOnAction(e -> move(0, 1));
        west.setOnAction(e -> move(-1,0));
        east.setOnAction(e -> move(1, 0));

        GridPane nav = new GridPane();
        nav.setHgap(5);
        nav.setVgap(5);
        nav.add(north, 1, 0);
        nav.add(west, 0, 1);
        nav.add(east, 2, 1);
        nav.add(south, 1, 2);
        nav.setAlignment(Pos.CENTER);

        // --- Action buttons
        Button fight  = new Button("Fight");
        Button run    = new Button("Run");
        Button search = new Button("Search");
        Button sleep  = new Button("Sleep");

        fight.setOnAction(e -> fight());
        run.setOnAction(e -> flee());
        search.setOnAction(e -> search());
        sleep.setOnAction(e -> sleep());

        VBox actions = new VBox(10, fight, run, search, sleep);
        actions.setAlignment(Pos.CENTER);

        log.setEditable(false);
        log.setPrefHeight(150);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(statsLabel);
        root.setCenter(mapGrid);
        root.setLeft(nav);
        root.setRight(actions);
        root.setBottom(log);

        updateStats();
        inspectRoom();

        stage.setTitle("Dungeon Digits – D&D Edition");
        stage.setScene(new Scene(root, 700, 500));
        stage.show();
    }

    // ----------------- Dungeon -----------------
    private void createDungeon() {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                dungeon[i][j] = new Room();
                dungeon[i][j].blocked = random.nextInt(8) == 0;
            }
        }
        dungeon[0][0].blocked = false;
    }

    private void buildMap() {
        mapGrid.getChildren().clear();

        for (int j = 0; j < MAP_SIZE; j++) {
            for (int i = 0; i < MAP_SIZE; i++) {

                Label cell = new Label("   ");
                cell.setMinSize(25,25);
                cell.setStyle("-fx-border-color:black;");

                if (dungeon[i][j].blocked)
                    cell.setStyle(cell.getStyle()+"-fx-background-color:gray;");

                if (i == x && j == y)
                    cell.setStyle(cell.getStyle()+"-fx-background-color:gold;");

                if (dungeon[i][j].hasMonster())
                    cell.setStyle(cell.getStyle()+"-fx-background-color:orange;");

                mapGrid.add(cell,i,j);
            }
        }
    }

    // ----------------- Movement -----------------
    private void move(int dx, int dy) {

        int nx = x + dx;
        int ny = y + dy;

        if(nx<0 || ny<0 || nx>=MAP_SIZE || ny>=MAP_SIZE){
            log.appendText("You hit the dungeon wall.\n");
            return;
        }

        if (dungeon[nx][ny].blocked) {
            log.appendText("Path blocked!\n");
            return;
        }

        x=nx; y=ny;

        buildMap();
        inspectRoom();
    }

    private void inspectRoom(){
        if(!dungeon[x][y].hasMonster() && random.nextBoolean()){
            dungeon[x][y].spawnMonster();
            log.appendText("A wild "+dungeon[x][y].monster.name+" appears!\n");
        }
    }

    // ----------------- Combat -----------------
    private void fight(){
        Room room = dungeon[x][y];

        if(!room.hasMonster()){
            log.appendText("Nothing to fight.\n");
            return;
        }

        Monster m = room.monster;

        if(d20() >= m.armorClass){
            int dmg = Math.max(1, hero.strength/3);
            m.hp -= dmg;
            log.appendText("You hit for "+dmg+"!\n");
        } else
            log.appendText("You miss.\n");

        if(m.hp<=0){
            log.appendText("Monster slain!\n");
            room.monster=null;
            buildMap();
            return;
        }

        monsterAttack(m);
        updateStats();
    }

    private void flee(){
        log.appendText("You flee!\n");
        move(random.nextInt(3)-1, random.nextInt(3)-1);
    }

    private void search(){
        if(dungeon[x][y].hasMonster()){
            log.appendText("Kill it first!\n");
            return;
        }

        if(d20()<hero.intelligence){
            int gold=random.nextInt(30)+5;
            hero.gold+=gold;
            log.appendText("Found "+gold+" gold!\n");
        }
        else log.appendText("No treasure.\n");

        updateStats();
    }

    private void sleep(){
        hero.hitPoints=20;
        log.appendText("HP restored.\n");

        if(random.nextInt(6)==0){
            dungeon[x][y].spawnMonster();
            log.appendText("Ambushed in your sleep!\n");
        }
        updateStats();
    }

    private void monsterAttack(Monster m){
        if(d20() >= hero.armorClass){
            int dmg=Math.max(1,m.strength/3);
            hero.hitPoints-=dmg;
            log.appendText("Monster hits for "+dmg+"!\n");
        }
        else log.appendText("Monster misses.\n");

        if(hero.hitPoints<=0)
            log.appendText("\n💀 YOU DIED 💀\n");
    }

    // ----------------- Dice -----------------
    private int d20(){ return random.nextInt(20)+1; }
    private int d6(){ return random.nextInt(6)+1; }

    // ----------------- UI -----------------
    private void updateStats(){
        statsLabel.setText(
                "HP: "+hero.hitPoints+
                        "  STR:"+hero.strength+
                        "  DEX:"+hero.dexterity+
                        "  INT:"+hero.intelligence+
                        "  Gold:"+hero.gold
        );
    }

    // ----------------- Classes -----------------
    private class Player{
        int hitPoints=20;
        int strength=roll3d6();
        int dexterity=roll3d6();
        int intelligence=roll3d6();
        int gold=0;
        int armorClass = 10+dexterity/3;

        private int roll3d6(){
            return d6()+d6()+d6();
        }
    }

    private class Room{
        boolean blocked=false;
        Monster monster=null;

        boolean hasMonster(){
            return monster!=null;
        }

        void spawnMonster(){
            monster=new Monster();
        }
    }

    private class Monster{
        String name;
        int hp,strength,dexterity,intelligence,armorClass;

        Monster(){
            String[] names={"Goblin","Orc","Skeleton","Kobold","Rat","Cultist"};
            name=names[random.nextInt(names.length)];

            int base=d6();
            hp=base*2;
            strength=base*2;
            dexterity=base*2;
            intelligence=base*2;

            armorClass=10 + dexterity/3;
        }
    }
}
