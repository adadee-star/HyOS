import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.*;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HyOS extends Application {

    private Pane desktop;
    private StackPane root;
    private HBox dock;
    private VBox loginScreen;
    private Label systemClock;

    private String currentTheme = "dark";

    @Override
    public void start(Stage stage) {
        root = new StackPane();
        applyTheme("dark");

        desktop = new Pane();
        desktop.setVisible(false);

        dock = new HBox(15);
        dock.setAlignment(Pos.CENTER);
        dock.setPadding(new Insets(10));
        dock.setVisible(false);

        systemClock = new Label();
        startClockUpdate();

        refreshLauncher();
        createLoginScreen();

        VBox layout = new VBox(desktop, dock);
        VBox.setVgrow(desktop, Priority.ALWAYS);

        root.getChildren().addAll(layout, loginScreen);

        stage.setScene(new Scene(root, 1200, 750));
        stage.setTitle("hyOS Apex");
        stage.show();
    }

    // ================= START MENU =================
    private Button createStartMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem shutdown = new MenuItem("Shut Down");
        shutdown.setOnAction(e -> Platform.exit());

        MenuItem restart = new MenuItem("Restart");
        restart.setOnAction(e -> {
            desktop.getChildren().clear();
            loginScreen.setVisible(true);
            desktop.setVisible(false);
            dock.setVisible(false);
        });

        menu.getItems().addAll(shutdown, restart);

        Button start = new Button("🟦");
        start.setOnAction(e -> menu.show(start, Side.TOP, 0, 0));
        return start;
    }

    private void refreshLauncher() {
        dock.getChildren().setAll(
                createStartMenu(),
                createLauncher("🐚","Terminal",()->spawn("Terminal",createTerminal())),
                createLauncher("🌐","Browser",()->spawn("Browser",createBrowser())),
                createLauncher("📂","Files",()->spawn("Files",createFiles())),
                createLauncher("🎬","Media",()->spawn("Media",createMedia())),
                createLauncher("🛒","Store",()->spawn("Store",createStore())),
                new Separator(),
                systemClock
        );
    }

    // ================= THEME =================
    private void applyTheme(String t){
        currentTheme=t;
        if(t.equals("light")){
            root.setStyle("-fx-background-color:#e2e8f0;");
            dock.setStyle("-fx-background-color:#ccc;");
        } else {
            root.setStyle("-fx-background-color:#0f172a;");
            dock.setStyle("-fx-background-color:#1e293b;");
        }
    }

    private Node createThemeApp(){
        VBox v=new VBox(10);
        Button dark=new Button("Dark");
        Button light=new Button("Light");
        dark.setOnAction(e->applyTheme("dark"));
        light.setOnAction(e->applyTheme("light"));
        v.getChildren().addAll(new Label("Themes"),dark,light);
        return v;
    }

    // ================= SNAKE =================
    private Node createSnake(){
        Canvas c=new Canvas(600,400);
        GraphicsContext g=c.getGraphicsContext2D();

        List<int[]> s=new ArrayList<>();
        s.add(new int[]{5,5});

        String[] dir={"RIGHT"}, next={"RIGHT"};
        int[] food={10,10};

        c.setFocusTraversable(true);
        Platform.runLater(c::requestFocus);

        c.setOnKeyPressed(e->{
            switch(e.getCode()){
                case W: case UP: if(!dir[0].equals("DOWN")) next[0]="UP"; break;
                case S: case DOWN: if(!dir[0].equals("UP")) next[0]="DOWN"; break;
                case A: case LEFT: if(!dir[0].equals("RIGHT")) next[0]="LEFT"; break;
                case D: case RIGHT: if(!dir[0].equals("LEFT")) next[0]="RIGHT"; break;
            }
        });

        new AnimationTimer(){
            long last=0;
            public void handle(long now){
                if(now-last<100_000_000)return;
                last=now;

                dir[0]=next[0];

                int[] h=s.get(0);
                int x=h[0],y=h[1];
                if(dir[0].equals("UP"))y--;
                if(dir[0].equals("DOWN"))y++;
                if(dir[0].equals("LEFT"))x--;
                if(dir[0].equals("RIGHT"))x++;

                s.add(0,new int[]{x,y});
                s.remove(s.size()-1);

                g.clearRect(0,0,600,400);
                g.setFill(Color.LIME);
                for(int[] p:s) g.fillRect(p[0]*20,p[1]*20,18,18);
            }
        }.start();

        return c;
    }

    // ================= HYPAINT =================
    private Node createPaint(){
        BorderPane root=new BorderPane();
        Canvas c=new Canvas(800,500);
        GraphicsContext g=c.getGraphicsContext2D();

        Color[] colors={Color.BLACK,Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW,Color.PURPLE};
        final Color[] current={Color.BLACK};

        HBox palette=new HBox(5);
        for(Color col:colors){
            Button b=new Button();
            b.setStyle("-fx-background-color:"+toHex(col));
            b.setOnAction(e->current[0]=col);
            palette.getChildren().add(b);
        }

        c.setOnMouseDragged(e->{
            g.setFill(current[0]);
            g.fillOval(e.getX(),e.getY(),6,6);
        });

        root.setTop(palette);
        root.setCenter(c);
        return root;
    }

    private String toHex(Color c){
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()*255),
                (int)(c.getGreen()*255),
                (int)(c.getBlue()*255));
    }

    // ================= HYTEXT (SAVE) =================
    private Node createText(){
        VBox v=new VBox(5);
        TextArea ta=new TextArea();

        Button save=new Button("Save");
        save.setOnAction(e->{
            FileChooser fc=new FileChooser();
            File f=fc.showSaveDialog(null);
            if(f!=null){
                try(PrintWriter out=new PrintWriter(f)){
                    out.print(ta.getText());
                }catch(Exception ex){ex.printStackTrace();}
            }
        });

        v.getChildren().addAll(save,ta);
        return v;
    }

    // ================= HYSTAT =================
    private Node createStat(){
        VBox v=new VBox(10);
        Label cpu=new Label();
        Label ram=new Label();

        new AnimationTimer(){
            public void handle(long now){
                cpu.setText("CPU: "+(int)(Math.random()*100)+"%");
                ram.setText("RAM: "+(int)(Math.random()*8000)+"MB");
            }
        }.start();

        v.getChildren().addAll(cpu,ram);
        return v;
    }

    // ================= STORE =================
    private Node createStore(){
        VBox s=new VBox(10);

        Button snake=new Button("Install Snake");
        snake.setOnAction(e->dock.getChildren().add(createLauncher("🕹️","Snake",()->spawn("Snake",createSnake()))));

        Button paint=new Button("Install hyPaint");
        paint.setOnAction(e->dock.getChildren().add(createLauncher("🎨","Paint",()->spawn("Paint",createPaint()))));

        Button text=new Button("Install hyText");
        text.setOnAction(e->dock.getChildren().add(createLauncher("📝","Text",()->spawn("Text",createText()))));

        Button stat=new Button("Install hyStat");
        stat.setOnAction(e->dock.getChildren().add(createLauncher("📊","Stat",()->spawn("Stat",createStat()))));

        Button theme=new Button("Install Themes");
        theme.setOnAction(e->dock.getChildren().add(createLauncher("🎨","Themes",()->spawn("Themes",createThemeApp()))));

        s.getChildren().addAll(new Label("App Store"),snake,paint,text,stat,theme);
        return s;
    }

    // ================= BASICS =================
    private Node createTerminal(){ return new TextArea("Terminal"); }
    private Node createBrowser(){ return new WebView(); }
    private Node createMedia(){ return new WebView(); }
    private Node createFiles(){ return new ListView<>(); }

    private void createLoginScreen(){
        loginScreen=new VBox(10);
        loginScreen.setAlignment(Pos.CENTER);

        PasswordField pf=new PasswordField();
        Button b=new Button("Login");

        b.setOnAction(e->{
            if(pf.getText().equals("admin")){
                loginScreen.setVisible(false);
                desktop.setVisible(true);
                dock.setVisible(true);
            }
        });

        loginScreen.getChildren().addAll(new Label("hyOS"),pf,b);
    }

    private void startClockUpdate(){
        Thread t=new Thread(()->{
            while(true){
                String time= LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                Platform.runLater(()->systemClock.setText(time));
                try{Thread.sleep(30000);}catch(Exception ignored){}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private Button createLauncher(String i,String n,Runnable r){
        Button b=new Button(i);
        b.setTooltip(new Tooltip(n));
        b.setOnAction(e->r.run());
        return b;
    }

    private void spawn(String t,Node c){
        VBox w=new VBox(new Label(t),c);
        w.setStyle("-fx-background-color:#1e293b; -fx-padding:10;");
        desktop.getChildren().add(w);
    }

    public static void main(String[] args){ launch(args); }
}
