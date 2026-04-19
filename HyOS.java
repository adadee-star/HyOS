import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
    private String currentTheme = "dark";

    private List<String> virtualDisk = new ArrayList<>(List.of(
            "kernel.sys", "config.cfg", "media_cache.tmp", "readme.txt"
    ));

    @Override
    public void start(Stage primaryStage) {
        root = new StackPane();
        applyTheme("dark");

        desktop = new Pane();
        desktop.setVisible(false);

        dock = new HBox(15);
        dock.setAlignment(Pos.CENTER);
        dock.setPadding(new Insets(10, 25, 10, 25));
        dock.setMaxHeight(65);
        dock.setVisible(false);

        systemClock = new Label();
        systemClock.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-weight: bold;");
        startClockUpdate();

        refreshLauncher();
        createLoginScreen();

        VBox mainLayout = new VBox(desktop, dock);
        VBox.setVgrow(desktop, Priority.ALWAYS);

        root.getChildren().addAll(mainLayout, loginScreen);

        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle(osName);
        primaryStage.show();
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

        Button startBtn = new Button("🟦");
        startBtn.setStyle("-fx-font-size: 20; -fx-background-color: #1e293b; -fx-text-fill: white;");
        startBtn.setOnAction(e -> menu.show(startBtn, Side.TOP, 0, 0));

        return startBtn;
    }

    private void refreshLauncher() {
        dock.getChildren().clear();
        dock.getChildren().addAll(
                createStartMenu(),
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

    // ================= THEME SYSTEM =================
    private void applyTheme(String theme) {
        currentTheme = theme;

        if (theme.equals("light")) {
            root.setStyle("-fx-background-color: #e2e8f0;");
            dock.setStyle("-fx-background-color: rgba(200,200,200,0.9); -fx-background-radius: 20;");
        } else {
            root.setStyle("-fx-background-color: #0f172a;");
            dock.setStyle("-fx-background-color: rgba(15,23,42,0.9); -fx-background-radius: 20;");
        }
    }

    private Node createThemeApp() {
        VBox v = new VBox(15);
        v.setPadding(new Insets(20));

        Button dark = new Button("Dark Mode");
        dark.setOnAction(e -> applyTheme("dark"));

        Button light = new Button("Light Mode");
        light.setOnAction(e -> applyTheme("light"));

        v.getChildren().addAll(new Label("Theme Settings"), dark, light);
        return v;
    }

    // ================= SNAKE FIX =================
    private Node createSnakeGame() {
        int w = 800, h = 550, size = 25;
        Canvas c = new Canvas(w, h);
        GraphicsContext gc = c.getGraphicsContext2D();

        final int[] speed = {7};
        final String[] dir = {"RIGHT"};
        final String[] nextDir = {"RIGHT"};

        final List<int[]> snake = new ArrayList<>();
        snake.add(new int[]{5, 5});

        final int[] food = {
                new Random().nextInt(w / size),
                new Random().nextInt(h / size)
        };

        final boolean[] running = {true};

        c.setFocusTraversable(true);
        Platform.runLater(c::requestFocus);

        c.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case W: case UP:
                    if (!dir[0].equals("DOWN")) nextDir[0] = "UP";
                    break;
                case S: case DOWN:
                    if (!dir[0].equals("UP")) nextDir[0] = "DOWN";
                    break;
                case A: case LEFT:
                    if (!dir[0].equals("RIGHT")) nextDir[0] = "LEFT";
                    break;
                case D: case RIGHT:
                    if (!dir[0].equals("LEFT")) nextDir[0] = "RIGHT";
                    break;
            }
        });

        new AnimationTimer() {
            long last = 0;

            public void handle(long now) {
                if (!running[0]) return;
                if (now - last < 1_000_000_000 / speed[0]) return;
                last = now;

                dir[0] = nextDir[0]; // ⚡ instant input

                int[] head = snake.get(0);
                int x = head[0], y = head[1];

                if (dir[0].equals("UP")) y--;
                if (dir[0].equals("DOWN")) y++;
                if (dir[0].equals("LEFT")) x--;
                if (dir[0].equals("RIGHT")) x++;

                if (x < 0 || y < 0 || x >= w / size || y >= h / size) running[0] = false;
                for (int[] p : snake) if (p[0] == x && p[1] == y) running[0] = false;

                if (running[0]) {
                    snake.add(0, new int[]{x, y});
                    if (x == food[0] && y == food[1]) {
                        food[0] = new Random().nextInt(w / size);
                        food[1] = new Random().nextInt(h / size);
                        speed[0]++;
                    } else snake.remove(snake.size() - 1);
                }

                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, w, h);

                gc.setFill(Color.RED);
                gc.fillOval(food[0] * size, food[1] * size, size, size);

                gc.setFill(Color.LIME);
                for (int[] p : snake)
                    gc.fillRect(p[0] * size, p[1] * size, size - 1, size - 1);

                if (!running[0]) {
                    gc.setFill(Color.WHITE);
                    gc.fillText("GAME OVER", w / 2 - 30, h / 2);
                }
            }
        }.start();

        return c;
    }

    // ================= APP STORE =================
    private Node createAppStore() {
        VBox s = new VBox(15);
        s.setPadding(new Insets(20));

        Button b1 = new Button("Install 🕹️ Snake");
        b1.setOnAction(e -> {
            dock.getChildren().add(1, createLauncher("🕹️", "Snake",
                    () -> spawnWindow("Snake", createSnakeGame())));
            b1.setDisable(true);
        });

        Button b2 = new Button("Install 🎨 Themes");
        b2.setOnAction(e -> {
            dock.getChildren().add(1, createLauncher("🎨", "Themes",
                    () -> spawnWindow("Themes", createThemeApp())));
            b2.setDisable(true);
        });

        s.getChildren().addAll(new Label("Software Store"), b1, b2);
        return s;
    }

    // ================= REST (UNCHANGED CORE) =================
    private Node createTerminal() { return new TextArea("Terminal"); }
    private Node createStatsApp() { return new VBox(new Label("CPU Usage")); }
    private Node createMediaPlayer() { return new WebView(); }
    private Node createPaintApp() { return new Canvas(800,600); }
    private Node createFileManager() { return new ListView<>(); }

    private void createLoginScreen() {
        loginScreen = new VBox(20);
        loginScreen.setAlignment(Pos.CENTER);

        PasswordField pf = new PasswordField();
        Button b = new Button("LOG IN");

        b.setOnAction(e -> {
            if (pf.getText().equals("admin")) {
                loginScreen.setVisible(false);
                desktop.setVisible(true);
                dock.setVisible(true);
            }
        });

        loginScreen.getChildren().addAll(new Label("hyOS APEX"), pf, b);
    }

    private void startClockUpdate() {
        Thread t = new Thread(() -> {
            while (true) {
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                Platform.runLater(() -> systemClock.setText(time));
                try { Thread.sleep(30000); } catch (Exception ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private Node createWebBrowser(String url) {
        WebView w = new WebView();
        w.getEngine().load(url);
        return new VBox(w);
    }

    private Button createLauncher(String i, String n, Runnable r) {
        Button b = new Button(i);
        b.setTooltip(new Tooltip(n));
        b.setOnAction(e -> r.run());
        return b;
    }

    private void spawnWindow(String t, Node c) {
        VBox win = new VBox(new Label(t), c);
        desktop.getChildren().add(win);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
