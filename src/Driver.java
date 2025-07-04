import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class Driver extends Application {
    // Directory where the images are pulled
    public final static File[] IMAGE_FILES = new File("images").listFiles();
    // Size of the app. Number represents pixel length of one tile
    public static final int SIZE = 128;
    // If SIM is false, runs Q planning and saves the results. If SIM is true, runs a simulation of the cars
    private static final boolean SIM = true;

    public static final ImageView[] CARS = new ImageView[] {
            getImageView("redCar.png"),
            getImageView("yellowCar.png")/*,
            getImageView("blueCar.png")*/};
    public static final ImageView BALL = getImageView("soccerBall.png");

    public static State state;

    public static void main(String[] args) {
        if (!SIM) {
            NashSolver.manager();

            NashSolver.saveStatesAsObject("nash_equilibrium.ser");
            // MarkovGame.saveStatesToCSV("nash_equilibrium.csv");
            // MarkovGame.saveQIterationToCSV("Q_Iteration.csv");
        } else {
            NashSolver.loadStatesAsObject("nash_equilibrium.ser");

            Application.launch(args);
        }
    }

    @Override
    public void start(Stage window) {
        Pane root = new Pane();
        Timeline timeline;

        if (State.GAME == 0) {
            state = State.states[CarState.getStateIndex(
                    new int[] {/*1, 1, */0, 0, State.WIDTH - 1, State.HEIGHT - 1})];
            createCarBoard(root);
            timeline = getCarTimeline();
        }
        else {
            state = State.states[SoccerState.getStateIndex(new int[] {0, State.WIDTH / 2 - 1, State.HEIGHT / 2,
                    State.WIDTH / 2 + 1, State.HEIGHT / 2 - 1})];
            createSoccerBoard(root);
            timeline = getSoccerTimeline(root);
        }

        // Run timeline indefinitely
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Scene scene = new Scene(root, State.WIDTH * SIZE, State.HEIGHT * SIZE);

        window.setTitle("Markov Game");
        window.setScene(scene);
        window.show();
    }

    private static ImageView getImageView(String spriteName) {
        try {
            for (File imageFile : Driver.IMAGE_FILES)
                if (imageFile.getName().equals(spriteName)) {
                    FileInputStream inStream = new FileInputStream(imageFile);
                    return new ImageView(new Image(inStream));
                }
        } catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
        }

        throw new NoSuchElementException("Sprite not initialized");
    }

    private static void createCarBoard(Pane root) {
        Group boardTiles = new Group();

        // Creates and draws the checkered grid based off of HEIGHT and WIDTH
        for (int y = 0; y < State.HEIGHT; y++)
            for (int x = 0; x < State.WIDTH; x++) {
                Rectangle tile = new Rectangle(x * SIZE, y * SIZE, SIZE, SIZE);

                tile.setFill((x + y) % 2 == 0 ? Color.BISQUE : Color.SADDLEBROWN);
                tile.setStroke(Color.BLACK);
                tile.setStrokeWidth(1);

                boardTiles.getChildren().add(tile);
            }

        root.getChildren().add(boardTiles);

        // Draws the cars based off the state
        for (int a = 0; a < State.NUM_AGENTS; a++) {
            CARS[a].setX(state.state[2*a] * SIZE);
            CARS[a].setY(state.state[2*a+1] * SIZE);
            CARS[a].setFitWidth(SIZE);
            CARS[a].setFitHeight(SIZE);
            root.getChildren().add(CARS[a]);
        }
    }

    private static Timeline getCarTimeline() {
        // Create a timeline that updates every second
        return new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // Get actions
            ActionSpace[] actions = new ActionSpace[State.NUM_AGENTS];
            for (int a = 0; a < State.NUM_AGENTS; a++)
                actions[a] = NashSolver.getMoveFromPolicy(state.NE[a]);

            // Move cars
            state = state.transition(actions);
            for (int a = 0; a < State.NUM_AGENTS; a++) {
                CARS[a].setX(state.state[2 * a] * SIZE);
                CARS[a].setY(state.state[2 * a + 1] * SIZE);
            }

            // Output to console a log of the states and the policies
            String NEString = "State: (";
            for (int each : state.state)
                NEString += each + ",";
            NEString = NEString.substring(0, NEString.length()-1) + "). ";

            for (int a = 0; a < State.NUM_AGENTS; a++) {
                NEString += "Agent " + a + "'s NE: (";
                for (double each : state.NE[a])
                    NEString += String.format("%.2f", each) + ",";
                NEString = NEString.substring(0, NEString.length()-1) + "), ";
            }

            System.out.println(NEString.substring(0, NEString.length()-2));
        }));
    }

    private static void createSoccerBoard(Pane root) {
        Group boardTiles = new Group();
        int[] goalYPos = new int[]{State.HEIGHT / 2 - 1, State.HEIGHT / 2};

        // Creates and draws the checkered grid based off of HEIGHT and WIDTH
        for (int y = 0; y < State.HEIGHT; y++)
            for (int x = 0; x < State.WIDTH; x++) {
                Rectangle tile = new Rectangle(x * SIZE, y * SIZE, SIZE, SIZE);

                if (x == 0 || x == State.WIDTH-1)
                    if (y == goalYPos[0] || y == goalYPos[1])
                        tile.setFill(Color.GHOSTWHITE);
                    else
                        tile.setFill(Color.BLACK);
                else
                    tile.setFill((x + y) % 2 == 0 ? Color.GREEN : Color.YELLOWGREEN);

                boardTiles.getChildren().add(tile);
            }

        root.getChildren().add(boardTiles);

        // Draws the cars based off the state
        for (int a = 0; a < State.NUM_AGENTS; a++) {
            CARS[a].setX(state.state[2*a+1] * SIZE);
            CARS[a].setY(state.state[2*a+2] * SIZE);
            CARS[a].setFitWidth(SIZE);
            CARS[a].setFitHeight(SIZE);
            root.getChildren().add(CARS[a]);
        }

        // Draws the ball with the correct player
        BALL.setX(state.state[2 * state.state[0] + 1] * SIZE);
        BALL.setY(state.state[2 * state.state[0] + 2] * SIZE);
        BALL.setFitWidth(SIZE / 2);
        BALL.setFitHeight(SIZE / 2);
        root.getChildren().add(BALL);
    }

    private static Timeline getSoccerTimeline(Pane root) {
        // Create a timeline that updates every second
        return new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // Get actions
            ActionSpace[] actions = new ActionSpace[State.NUM_AGENTS];
            for (int a = 0; a < State.NUM_AGENTS; a++)
                actions[a] = NashSolver.getMoveFromPolicy(state.NE[a]);

            // Move cars
            state = state.transition(actions);

            // Check if a goal was scored
            if (Math.abs(state.reward[0]) == 10) {
                System.out.println((state.reward[0] > 0 ? "Red" : "Yellow") + " scored");
                root.getChildren().clear();
                state = State.states[SoccerState.getStateIndex(new int[] {0, State.WIDTH / 2 - 1, State.HEIGHT / 2,
                        State.WIDTH / 2 + 1, State.HEIGHT / 2 - 1})];
                createSoccerBoard(root);
                return;
            }

            for (int a = 0; a < State.NUM_AGENTS; a++) {
                CARS[a].setX(state.state[2 * a + 1] * SIZE);
                CARS[a].setY(state.state[2 * a + 2] * SIZE);
            }
            BALL.setX(state.state[2 * state.state[0] + 1] * SIZE);
            BALL.setY(state.state[2 * state.state[0] + 2] * SIZE);

            // Output to console a log of the states and the policies
            String NEString = "State: (";
            for (int each : state.state)
                NEString += each + ",";
            NEString = NEString.substring(0, NEString.length()-1) + "). ";

            for (int a = 0; a < State.NUM_AGENTS; a++) {
                NEString += "Agent " + a + "'s NE: (";
                for (double each : state.NE[a])
                    NEString += String.format("%.2f", each) + ",";
                NEString = NEString.substring(0, NEString.length()-1) + "), ";
            }

            System.out.println(NEString.substring(0, NEString.length()-2));
        }));
    }
}
