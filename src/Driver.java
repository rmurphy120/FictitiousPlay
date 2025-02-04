import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class Driver extends Application {
    // Directory where the images are pulled
    public final static File[] IMAGE_FILES = new File("images").listFiles();
    // Size of the app. Number represents pixel length of one tile
    public static final int SIZE = 128;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage window) {
        Pane root = new Pane();

        createBoard(root);

        // Create a timeline that updates every second
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // Currently just makes a random move
            for (Car each : Board.CARS)
                each.moveRandomly();
        }));

        // Run timeline indefinitely
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Scene scene = new Scene(root, Board.WIDTH * SIZE, Board.HEIGHT * SIZE);

        window.setTitle("Car Game");
        window.setScene(scene);
        window.show();
    }

    private void createBoard(Pane root) {
        Group boardTiles = new Group();

        // Creates and draws the checkered grid based off of HEIGHT and WIDTH
        for (int y = 0; y < Board.HEIGHT; y++)
            for (int x = 0; x < Board.WIDTH; x++) {
                Rectangle tile = new Rectangle(x * SIZE, y * SIZE, SIZE, SIZE);

                tile.setFill((x + y) % 2 == 0 ? Color.BISQUE : Color.SADDLEBROWN);
                tile.setStroke(Color.BLACK);
                tile.setStrokeWidth(1);

                boardTiles.getChildren().add(tile);
            }

        root.getChildren().add(boardTiles);

        // Draws cars
        for (Car each : Board.CARS) {
            ImageView sprite = each.getImageView();

            sprite.setX(each.getX() * SIZE);
            sprite.setY(each.getY() * SIZE);

            sprite.setFitHeight(SIZE);
            sprite.setFitWidth(SIZE);

            root.getChildren().add(sprite);
        }
    }
}
