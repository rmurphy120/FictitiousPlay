import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarkovGame {
    // Hyperparameters for the game
    public static final int HEIGHT = 4;
    public static final int WIDTH = 6;
    public static final int NUM_AGENTS = 2;

    // Hyperparameters for fictitious play
    public static final double DISCOUNT_FACTOR = 0.9;
    private static final int NUM_FORGET = 30;
    private static final double CONVERGENCE_BOUND = 0.1;
    private static final int LENGTH_TO_UPDATE_VALS = 1;

    public static int numEpisodes;
    private static final Random r = new Random();

    private static final List<double[]> avgQLossPerIteration = new ArrayList<>();

    public static State[] states;

    public static void manager() {
        states = State.getAllStates();

        long startTime = System.nanoTime();
        fictitiousPlay();

        System.out.println("Converged after " + numEpisodes + " episodes");
        System.out.println("Took " + ((System.nanoTime() - startTime) / 1_000_000.0) + " milliseconds to converge");

        for (State each : states)
            each.calculateNE();
    }

    public static void fictitiousPlay() {
        for (State each : states)
            each.initializeForFictitiousPlay();

        // Used to track the convergence of Q
        double[] avgQLoss;

        boolean hasConverged = false;

        for (numEpisodes = ActionSpace.values().length; !hasConverged; numEpisodes++) {
            // Only check convergence when Q table is updated
            hasConverged = true;
            avgQLoss = new double[states.length];

            for (State each : states) {
                each.updatePastActions();
                each.updateExpectedValues();
            }

            for (int i = 0; i < states.length; i++) {
                avgQLoss[i] = states[i].updateQTable();
                if (avgQLoss[i] > CONVERGENCE_BOUND)
                    hasConverged = false;
            }

            if (numEpisodes % LENGTH_TO_UPDATE_VALS == 0) {
                avgQLossPerIteration.add(avgQLoss);
                for (State each : states)
                    each.updateTargetQTable();
            }

            if (numEpisodes + 1 == NUM_FORGET)
                for (State each : states)
                    each.initializeFirstActions();

            if (numEpisodes % 100 == 0)
                System.out.println("Episode " + numEpisodes);
        }

        for (State each : states)
            each.forgetFirstActions();

        numEpisodes *= 1 - NUM_FORGET / (double)numEpisodes;
    }

    public static ActionSpace getMoveFromPolicy(double[] policy) {
        double choice = r.nextDouble();
        for (int i = 0; i < policy.length; i++) {
            choice -= policy[i];
            if (choice <= 0)
                return ActionSpace.values()[i];
        }

        throw new IllegalArgumentException("Policy doesn't add to at least 1");
    }


    public static void saveStatesAsObject(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(states);
            System.out.println("NE successfully saved to file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to retrieve NE from a file
    @SuppressWarnings("unchecked")
    public static void loadStatesAsObject(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            states = (State[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Method to save states to CSV
    public static void saveStatesToCSV(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            String header = "X_1,Y_1,X_2,Y_2:";
            for (int a = 0; a < NUM_AGENTS; a++)
                header += "U_" + a + "," + "D_" + a + "," + "L_" + a + "," + "R_" + a + "," /*+ "S_" + a + ","*/;

            writer.write(header.substring(0, header.length()-1));
            writer.newLine();

            for (State s : states) {
                StringBuilder line = new StringBuilder();

                // Append state values
                line.append(s.state[1] + "," + s.state[0] + "," + s.state[3] + "," + s.state[2] + ":");

                // Append NE values
                for (double[] row : s.NE)
                    for (double value : row)
                        line.append(value).append(",");

                // Remove trailing comma and write to file
                writer.write(line.substring(0, line.length() - 1));
                writer.newLine();
            }

            System.out.println("States successfully saved to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Saves the average difference in Q value at each state to a CSV file
    public static void saveQIterationToCSV(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            for (double[] each : avgQLossPerIteration) {
                for (int i = 0; i < each.length; i++) {
                    writer.append(String.valueOf(each[i]));

                    if (i < each.length - 1)
                        writer.append(",");
                }

                writer.append("\n");
            }

            System.out.println("File saved successfully: " + filename);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
