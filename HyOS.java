    public void createSnakeGame() {
        // Set to track key states
        Set<Integer> keyStates = new HashSet<>();

        // Wraparound movement logic
        snake.setOnKeyPressed(event -> {
            keyStates.add(event.getCode().getCode());
        });

        snake.setOnKeyReleased(event -> {
            keyStates.remove(event.getCode().getCode());
        });

        // Game loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (keyStates.contains(KeyCode.UP.getCode())) {
                    snake.moveUp();
                } else if (keyStates.contains(KeyCode.DOWN.getCode())) {
                    snake.moveDown();
                } else if (keyStates.contains(KeyCode.LEFT.getCode())) {
                    snake.moveLeft();
                } else if (keyStates.contains(KeyCode.RIGHT.getCode())) {
                    snake.moveRight();
                }

                // Implement wraparound movement
                if (snake.getHead().getX() >= gridWidth) {
                    snake.getHead().setX(0);
                } else if (snake.getHead().getX() < 0) {
                    snake.getHead().setX(gridWidth - 1);
                }
                if (snake.getHead().getY() >= gridHeight) {
                    snake.getHead().setY(0);
                } else if (snake.getHead().getY() < 0) {
                    snake.getHead().setY(gridHeight - 1);
                }

                // Update the score display
                scoreDisplay.setText("Score: " + snake.getLength() + " | High Score: " + highScore);

                // Check win condition
                if (snake.getLength() == gridWidth * gridHeight) {
                    this.stop();
                    showWinMessage();
                }
            }
        };

        // Start the game
        timer.start();
    }

    private void showWinMessage() {
        Label winLabel = new Label("YOU WIN!");
        winLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: green;");
        // Center the message on screen
        // Add winLabel to scene or appropriate pane
    }

