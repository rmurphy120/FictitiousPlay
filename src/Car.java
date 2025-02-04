import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.util.NoSuchElementException;
import java.util.Random;

public class Car {
    private enum MoveType {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    private static final Random r = new Random();
    private int x;
    private int y;
    private ImageView imageView;

    public Car(int x, int y, String spriteName) {
        this.x = x;
        this.y = y;

        // Parses image file to type ImageView
        try {
            for (File imageFile : Driver.IMAGE_FILES)
                if (imageFile.getName().equals(spriteName)) {
                    FileInputStream inStream = new FileInputStream(imageFile);
                    imageView = new ImageView(new Image(inStream));
                }
        } catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
        }

        if (imageView == null)
            throw new NoSuchElementException("Sprite not initialized");
    }

    public ImageView getImageView() {
        return imageView;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }

    /**
     * Call this method to move a car. DO NOT CHANGE VALUES INDIVIDUALLY
     *
     * @param action attempted action - NOTE will have to change method to match terminology - State Transition
     *               function??
     * @return true if successfully moved
     */
    private boolean move(MoveType action) {
        // Converts action to new x and y coordinates
        int nx = -1;
        int ny = -1;
        switch (action) {
            case UP -> { nx = x; ny = y + 1; }
            case DOWN -> { nx = x; ny = y - 1; }
            case LEFT -> { nx = x + 1; ny = y; }
            case RIGHT -> { nx = x - 1; ny = y; }
        }

        // Checks if move is legal - NOTE will probably have to change this since all moves will be legal
        if (!(nx >= 0 && nx < Board.WIDTH && ny >= 0 && ny < Board.HEIGHT))
            return false;

        x = nx;
        y = ny;
        imageView.setX(Driver.SIZE * nx);
        imageView.setY(Driver.SIZE * ny);

        return true;
    }

    public void moveRandomly() {
        boolean hasMoved = false;
        while (!hasMoved)
            hasMoved = move(MoveType.values()[r.nextInt(4)]);
    }
}
