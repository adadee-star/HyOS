import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HyOS extends Application {

    private Pane desktop;
    private StackPane root;
    private HBox dock;
    private VBox loginScreen;
    private Label systemClock;

    private boolean isRoot = false;
    private String currentUser = "admin";
    private String osName = "hyOS Apex v10.2";
    private List<String> virtualDisk = new ArrayList<>(List.of("kernel.sys", "config.cfg", "media_cache.tmp", "readme.txt"));

    @Override
    public void start(Stage primaryStage) {
        root = new StackPane();
        root.setStyle("-fx-background-color: #0f172a;"); 

        desktop = new Pane();
        desktop.setVisible(false);

        dock = new HBox(15);
        dock.setAlignment(Pos.CENTER);
        dock.setPadding(new Insets(10, 25, 10, 25));
        dock.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-background-radius: 20; -fx-border-color: #334155;");
        dock.setMaxHeight(65);
        dock.setVisible(false);

        systemClock = new Label();
        systemClock.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-weight: bold;");
        startClockUpdate();

        refreshLauncher();
        createLoginScreen();

        VBox mainLayout = new VBox(desktop, dock);
        VBox.setVgrow(desktop, Priority.ALWAYS);
        StackPane.setMargin(dock, new Insets(0, 0, 15, 0));

        root.getChildren().addAll(mainLayout, loginScreen);

        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle(osName);
        primaryStage.show();
    }

    private void refreshLauncher() {
        dock.getChildren().clear();
        dock.getChildren().addAll(
            createLauncher("🐚", "Terminal", () -> spawnWindow("Terminal", createTerminal())),
            createLauncher("🌐", "Browser", () -> spawnWindow("Web Browser", createWebBrowser("https://www.google.com"))),
            createLauncher("📂", "hyDisk", () -> spawnWindow("Files", createFileManager())),
            createLauncher("🎬", "Media", () -> spawnWindow("hyMedia Player", createMediaPlayer())),
            createLauncher("🎨", "Paint", () -> spawnWindow("hyPaint", createPaintApp())),
            createLauncher("🛒", "Store", () -> spawnWindow("App Store", createAppStore())),
            new Separator(javafx.geometry.Orientation.VERTICAL),
            systemClock
        );
    }

    private Node createTerminal() {
        TextArea ta = new TextArea(osName + " Shell\nType 'help' for available commands.\n\n" + currentUser + "@hyos:~$ ");
        ta.setStyle("-fx-control-inner-background: black; -fx-text-fill: #10b981; -fx-font-family: 'Consolas', monospace;");
        ta.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String prompt = isRoot ? "root@hyos:# " : currentUser + "@hyos:~$ ";
                String[] lines = ta.getText().split("\n");
                String input = lines[lines.length - 1].replace(prompt, "").trim().toLowerCase();
                ta.appendText("\n");

                if (input.equals("help")) {
                    ta.appendText("Commands: help, sudo su\nLocked: ls, neofetch, clear, open [app], sudo rm -rf /");
                } else if (input.equals("sudo su")) {
                    isRoot = true; 
                    ta.setStyle("-fx-control-inner-background: #1a0000; -fx-text-fill: #ef4444;");
                    ta.appendText("SUPERUSER PRIVILEGES ENABLED.");
                } else if (!isRoot) {
                    ta.appendText("Access Denied: You must be root. Type 'sudo su'.");
                } else {
                    if (input.equals("neofetch")) {
                        ta.appendText("      hyOS\n    _______     OS: " + osName + "\n   /  ___  \\    Kernel: 10.2.0-hy-stable\n  |  /   \\  |   Shell: hyBash\n   \\_______/    Resolution: 1280x800\n");
                    } else if (input.equals("sudo rm -rf /")) {
                        virtualDisk.clear();
                        ta.setStyle("-fx-control-inner-background: #450a0a; -fx-text-fill: white;");
                        ta.appendText("ERASING ROOT... SYSTEM FAILURE IMMINENT.");
                        Platform.runLater(() -> spawnWindow("RECOVERY", createWebBrowser("https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1")));
                    } else if (input.startsWith("open ")) {
                        handleShellLaunch(input.substring(5));
                    } else if (input.equals("ls")) {
                        ta.appendText(virtualDisk.isEmpty() ? "fs error: empty" : String.join("  ", virtualDisk));
                    } else if (input.equals("clear")) {
                        ta.setText(isRoot ? "root@hyos:# " : currentUser + "@hyos:~$ "); e.consume(); return;
                    } else {
                        ta.appendText("sh: unknown command: " + input);
                    }
                }
                ta.appendText("\n" + (isRoot ? "root@hyos:# " : currentUser + "@hyos:~$ "));
                ta.positionCaret(ta.getText().length());
                e.consume();
            }
        });
        return ta;
    }

    private void handleShellLaunch(String app) {
        if (app.contains("snake")) spawnWindow("Snake", createSnakeGame());
        else if (app.contains("media")) spawnWindow("hyMedia", createMediaPlayer());
        else if (app.contains("browser")) spawnWindow("Browser", createWebBrowser("https://www.google.com"));
        else if (app.contains("paint")) spawnWindow("hyPaint", createPaintApp());
        else if (app.contains("stat")) spawnWindow("hyStat", createStatsApp());
    }

    class HyWindow extends VBox {
        private double x = 0, y = 0;
        private double oldX, oldY, oldW, oldH;
        private boolean isMaximized = false;

        public HyWindow(String title, Node content) {
            this.setPrefSize(800, 600);
            this.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-background-radius: 12;");
            this.setEffect(new DropShadow(20, Color.BLACK));
            
            HBox titleBar = new HBox(10);
            titleBar.setStyle("-fx-background-color: #0f172a; -fx-padding: 10; -fx-cursor: move; -fx-background-radius: 12 12 0 0;");
            Label l = new Label(title); l.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Button maxBtn = new Button("▢");
            maxBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 10;");
            maxBtn.setOnAction(e -> toggleMaximize());

            Button closeBtn = new Button("✕"); 
            closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 50; -fx-font-size: 10;");
            closeBtn.setOnAction(e -> desktop.getChildren().remove(this));
            
            titleBar.getChildren().addAll(l, spacer, maxBtn, closeBtn);
            titleBar.setOnMousePressed(e -> { x = e.getX(); y = e.getY(); this.toFront(); });
            titleBar.setOnMouseDragged(e -> { 
                if(!isMaximized) {
                    this.setLayoutX(e.getScreenX() - x - getScene().getWindow().getX());
                    this.setLayoutY(e.getScreenY() - y - getScene().getWindow().getY()); 
                }
            });
            
            VBox.setVgrow(content, Priority.ALWAYS);
            Rectangle resizeHandle = new Rectangle(15, 15, Color.web("#475569"));
            resizeHandle.setCursor(javafx.scene.Cursor.SE_RESIZE);
            resizeHandle.setOnMouseDragged(e -> {
                if(!isMaximized) this.setPrefSize(e.getSceneX() - this.getLayoutX(), e.getSceneY() - this.getLayoutY());
            });
            HBox footer = new HBox(resizeHandle); footer.setAlignment(Pos.BOTTOM_RIGHT);

            this.getChildren().addAll(titleBar, content, footer);
            this.setLayoutX(100 + (new Random().nextInt(50))); 
            this.setLayoutY(50 + (new Random().nextInt(50)));
            this.setOnMousePressed(e -> this.toFront());
        }

        private void toggleMaximize() {
            if (!isMaximized) {
                oldX = getLayoutX(); oldY = getLayoutY(); oldW = getWidth(); oldH = getHeight();
                setLayoutX(0); setLayoutY(0); setPrefSize(desktop.getWidth(), desktop.getHeight());
                isMaximized = true;
            } else {
                setLayoutX(oldX); setLayoutY(oldY); setPrefSize(oldW, oldH);
                isMaximized = false;
            }
        }
    }

    private Node createSnakeGame() {
        int w = 800, h = 550, size = 25;
        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();
        final int[] speed = {7};
        final String[] dir = {"RIGHT"};
        final List<int[]> snake = new ArrayList<>();
        snake.add(new int[]{5, 5});
        final int[] food = {new Random().nextInt(w/size), new Random().nextInt(h/size)};
        final boolean[] running = {true};

        c.setFocusTraversable(true);
        Platform.runLater(c::requestFocus);

        c.setOnKeyPressed(e -> {
            if ((e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP) && !dir[0].equals("DOWN")) dir[0] = "UP";
            if ((e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN) && !dir[0].equals("UP")) dir[0] = "DOWN";
            if ((e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) && !dir[0].equals("RIGHT")) dir[0] = "LEFT";
            if ((e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) && !dir[0].equals("LEFT")) dir[0] = "RIGHT";
        });

        new AnimationTimer() {
            long last = 0;
            public void handle(long now) {
                if (!running[0]) return;
                if (now - last < 1000_000_000 / speed[0]) return;
                last = now;

                int[] head = snake.get(0);
                int nX = head[0], nY = head[1];
                if (dir[0].equals("UP")) nY--;
                else if (dir[0].equals("DOWN")) nY++;
                else if (dir[0].equals("LEFT")) nX--;
                else if (dir[0].equals("RIGHT")) nX++;

                if (nX < 0 || nY < 0 || nX >= w/size || nY >= h/size) running[0] = false;
                for(int[] p : snake) if(p[0] == nX && p[1] == nY) running[0] = false;

                if (running[0]) {
                    snake.add(0, new int[]{nX, nY});
                    if (nX == food[0] && nY == food[1]) {
                        food[0] = new Random().nextInt(w/size); food[1] = new Random().nextInt(h/size);
                        speed[0]++;
                    } else snake.remove(snake.size()-1);
                }

                gc.setFill(Color.BLACK); gc.fillRect(0,0,w,h);
                gc.setFill(Color.RED); gc.fillOval(food[0]*size, food[1]*size, size, size);
                gc.setFill(Color.LIME);
                for(int[] p : snake) gc.fillRect(p[0]*size, p[1]*size, size-1, size-1);
                if(!running[0]) { gc.setFill(Color.WHITE); gc.fillText("GAME OVER", w/2 - 30, h/2); }
            }
        }.start();
        return c;
    }

    private Node createAppStore() {
        VBox s = new VBox(15); s.setPadding(new Insets(20)); s.setStyle("-fx-background-color: #0f172a;");
        Button b1 = new Button("Install 🕹️ Snake");
        b1.setOnAction(e -> { dock.getChildren().add(1, createLauncher("🕹️", "Snake", () -> spawnWindow("Snake", createSnakeGame()))); b1.setDisable(true); });
        Button b2 = new Button("Install 📝 Notepad");
        b2.setOnAction(e -> { dock.getChildren().add(1, createLauncher("📝", "Notepad", () -> spawnWindow("Notepad", new TextArea()))); b2.setDisable(true); });
        Button b3 = new Button("Install 📊 hyStat");
        b3.setOnAction(e -> { dock.getChildren().add(1, createLauncher("📊", "hyStat", () -> spawnWindow("System Monitor", createStatsApp()))); b3.setDisable(true); });
        s.getChildren().addAll(new Label("Software Store"), b1, b2, b3); return s;
    }

    private Node createStatsApp() {
        VBox stats = new VBox(10); stats.setPadding(new Insets(20));
        stats.getChildren().addAll(new Label("CPU Usage: 4%"), new Label("RAM: 850MB / 16GB"));
        return stats;
    }

    private Node createMediaPlayer() {
        VBox p = new VBox(5); p.setStyle("-fx-background-color: #020617;");
        WebView w = new WebView(); w.getEngine().load("https://www.youtube.com/embed/jfKfPfyJRdk");
        VBox.setVgrow(w, Priority.ALWAYS); p.getChildren().add(w); return p;
    }

    private Node createPaintApp() {
        Canvas c = new Canvas(800, 600); GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.WHITE); gc.fillRect(0,0,800,600);
        c.setOnMouseDragged(e -> { gc.setFill(Color.BLACK); gc.fillOval(e.getX(), e.getY(), 5, 5); });
        return c;
    }

    private Node createFileManager() {
        ListView<String> lv = new ListView<>(); lv.getItems().addAll(virtualDisk);
        lv.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: #38bdf8;");
        return lv;
    }

    private void createLoginScreen() {
        loginScreen = new VBox(20); loginScreen.setAlignment(Pos.CENTER);
        loginScreen.setStyle("-fx-background-color: #020617;");
        PasswordField pf = new PasswordField(); pf.setMaxWidth(200); pf.setPromptText("Password (admin)");
        Button b = new Button("LOG IN");
        b.setOnAction(e -> { if (pf.getText().equals("admin")) { loginScreen.setVisible(false); desktop.setVisible(true); dock.setVisible(true); } });
        loginScreen.getChildren().addAll(new Label("hyOS APEX"), pf, b);
    }

    private void startClockUpdate() {
        Thread t = new Thread(() -> { while (true) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Platform.runLater(() -> systemClock.setText(time));
            try { Thread.sleep(30000); } catch (Exception e) {}
        }}); t.setDaemon(true); t.start();
    }

    private Node createWebBrowser(String url) {
        VBox v = new VBox(); WebView w = new WebView();
        w.getEngine().load(url); VBox.setVgrow(w, Priority.ALWAYS);
        v.getChildren().addAll(w); return v;
    }

    private Button createLauncher(String i, String n, Runnable r) {
        Button b = new Button(i); b.setTooltip(new Tooltip(n));
        b.setStyle("-fx-font-size: 24; -fx-background-color: transparent; -fx-cursor: hand;");
        b.setOnAction(e -> r.run()); return b;
    }

    private void spawnWindow(String t, Node c) {
        HyWindow win = new HyWindow(t, c); desktop.getChildren().add(win); win.toFront();
    }

    public static void main(String[] args) { launch(args); }
}