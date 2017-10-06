package me.keegan.maze;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import java.util.*;

public class Maze extends ApplicationAdapter {
    private static final int BLOCK_SIZE = 10;

    private boolean[][] board;
    private int maxSteps = 0;
    private int playerX;
    private int playerY;
    private int targetX;
    private int targetY;

    private LinkedList<Integer> directions = new LinkedList<Integer>();
    private Thread mazeThread;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;

    @Override
    public void create() {
        // Add the four directions
        directions.add(0);
        directions.add(1);
        directions.add(2);
        directions.add(3);
        Collections.shuffle(directions);

        // Initialize the orthographic camera, y-down is false
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Create the shape renderer and pass it our camera
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Initialize the board, separate it into blocks
        board = new boolean[Gdx.graphics.getWidth() / BLOCK_SIZE][Gdx.graphics.getHeight() / BLOCK_SIZE];

        // Create a mazeThread for the maze generation and solution
        new Thread() {
            public void run() {
                while (true) {
                    if (mazeThread == null || !mazeThread.isAlive()) {
                        mazeThread = makeMazeThread();
                        mazeThread.start();
                    }
                }

            }
        }.start();
    }

    Thread makeMazeThread() {
        return new Thread() {
            public void run() {
                // Empty the board
                for (int i = 0; i < board.length; i++) {
                    for (int j = 0; j < board[0].length; j++) {
                        board[i][j] = false;
                    }
                }

                // Ensure that the board is solvable
                try {
                    solvable(board.length / 2, board[0].length / 2, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                assert (board[board.length / 2][board[0].length / 2]);

                // Solve
                solve(board.length / 2, board[0].length / 2, -1);

                // When solved, interrupt the thread
                this.interrupt();
            }

            public boolean exists(int w, int g) {
                return (board.length > w && board[0].length > g);
            }

            boolean solve(int x, int y, int lastMove) {
                int oldPlayerX = playerX;
                int oldPlayerY = playerY;
                playerX = x;
                playerY = y;

                if (x == targetX && y == targetY) {
//                    System.out.println("Won!");
                    this.interrupt();
                    return true;
                }

                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (Integer i : directions) {
                    switch (i) {
                        // Right
                        case 0:
                            if (exists(playerX + 1, playerY) && board[playerX + 1][playerY]
                                    && lastMove != 1) {
                                if (solve(x + 1, y, 0)) {
                                    return true;
                                }
                            }
                            break;
                        // Left
                        case 1:
                            if (exists(playerX - 1, playerY) && board[playerX - 1][playerY]
                                    && lastMove != 0) {
                                // playerX -=1;
                                if (solve(x - 1, y, 1)) {
                                    return true;
                                }
                            }
                            break;
                        // Up
                        case 2:
                            if (exists(playerX, playerY + 1) && board[playerX][playerY + 1]
                                    && lastMove != 3) {
                                if (solve(x, y + 1, 2)) {
                                    return true;
                                }
                            }
                            break;
                        // Down
                        case 3:
                            if (exists(playerX, playerY - 1) && board[playerX][playerY - 1]
                                    && lastMove != 2) {
                                if (solve(x, y - 1, 3)) {
                                    return true;
                                }
                            }
                            break;
                    }
                }

                playerX = oldPlayerX;
                playerY = oldPlayerY;
                return false;
            }

            void solvable(int x, int y, int steps) throws InterruptedException {
                if (x == board.length - 1 || y == board[x].length - 1) {
                    return;
                }

                List<Integer> directions = new ArrayList<Integer>();

                // Check which neighbors are passable
                int numSpaces = 0;

                if (x > 0) {
                    directions.add(0);
                    if (board[x - 1][y])
                        numSpaces++;
                }

                if (x < board.length - 1) {
                    directions.add(1);
                    if (board[x + 1][y])
                        numSpaces++;
                }

                if (y > 0) {
                    directions.add(2);
                    if (board[x][y - 1])
                        numSpaces++;
                }

                if (y < board[x].length - 1) {
                    directions.add(3);
                    if (board[x][y + 1])
                        numSpaces++;
                }

                if (numSpaces > 1) {
                    return;
                }

                board[x][y] = true;

                if (steps > maxSteps) {
                    targetX = x;
                    targetY = y;
                    maxSteps = steps;
                }

                Thread.sleep(0);

                Collections.shuffle(directions);

                for (Integer neighbor : directions) {
                    switch (neighbor) {
                        case 0:
                            if (x - 2 > 0) {
                                solvable(x - 1, y, steps + 1);
                            }
                            break;
                        case 1:
                            if (x + 2 < board.length) {
                                solvable(x + 1, y, steps + 1);
                            }
                            break;
                        case 2:
                            if (y - 2 > 0) {
                                solvable(x, y - 1, steps + 1);
                            }
                            break;
                        case 3:
                            if (y + 2 < board[x].length) {
                                solvable(x, y + 1, steps + 1);
                            }
                            break;
                    }

                }
            }
        };

    }

    @Override
    public void render() {
        // If escape is pressed, exit the application
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        // Update the camera
        this.camera.update();

        // Clear the screen
        Gdx.gl.glClearColor(0, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Begin rendering shapes
        shapeRenderer.begin(ShapeType.Filled);

        // For each element in the board, if it is activated, draw it
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j]) {
                    shapeRenderer.setColor(0, 0, 0, 1);
                    shapeRenderer.rect(i * BLOCK_SIZE, j * BLOCK_SIZE, 10, 10);
                }
            }
        }

        // Draw the player
        shapeRenderer.setColor(255, 255, 255, 1);
        shapeRenderer.rect(playerX * BLOCK_SIZE, playerY * BLOCK_SIZE, 10, 10);

        // Draw the target
        shapeRenderer.setColor(255, 0, 255, 1);
        shapeRenderer.rect(targetX * BLOCK_SIZE, targetY * BLOCK_SIZE, 10, 10);

        // Push it to the camera and finish drawing
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.end();
    }
}
