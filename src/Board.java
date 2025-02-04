import java.util.ArrayList;
import java.util.List;

public class Board {
    // Pre-initiated constants that represent the aspects of the board. HEIGHT and WIDTH are in tiles
    public static final int HEIGHT = 3;
    public static final int WIDTH = 3;
    public static final List<Car> CARS = new ArrayList<>() {
        {
            add(new Car(0, 0, "redCar.png"));
            add(new Car(WIDTH-1, HEIGHT -1, "yellowCar.png"));
        }
    };
}
