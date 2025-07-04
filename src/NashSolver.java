import java.io.*;
import java.util.Random;

public class NashSolver {
    // Hyperparameters for fictitious play
    public static final double DISCOUNT_FACTOR = 0.9;
    private static final int NUM_FORGET = 1000;
    private static final double CONVERGENCE_BOUND = 0.001;
    private static final int LENGTH_TO_UPDATE_VALS = 50;

    public static int numEpisodes;
    private static final Random r = new Random();

    // private static final List<double[]> avgQLossPerIteration = new ArrayList<>();



    public static void manager() {
        long startTime = System.nanoTime();
        fictitiousPlay();

        System.out.println("Converged after " + numEpisodes + " episodes and "  +
                ((System.nanoTime() - startTime) / 1_000_000_000.0) + " seconds");

        for (State each : State.states)
            each.calculateNE();
    }

    public static void fictitiousPlay() {
        for (State each : State.states)
            each.initializeForFictitiousPlay();

        // Used to track the convergence of Q
        // double[] avgQLoss = new double[State.states.length];
        double avgAvgQDiff = 0;
        double avgQDiff;

        boolean hasConverged = false;

        for (numEpisodes = ActionSpace.values().length; !hasConverged && numEpisodes < 25000; numEpisodes++) {
            // Only check convergence when Q table is updated
            hasConverged = numEpisodes % LENGTH_TO_UPDATE_VALS == 1;

            for (State each : State.states) {
                each.updatePastActions();
                each.updateExpectedValues();
            }

            for (State each : State.states) {
                avgQDiff = each.updateQTable();
                if (avgQDiff > CONVERGENCE_BOUND)
                    hasConverged = false;
                if (numEpisodes % LENGTH_TO_UPDATE_VALS == 1)
                    avgAvgQDiff += avgQDiff;
            }

            if (numEpisodes % LENGTH_TO_UPDATE_VALS == 0) {
                // avgQLossPerIteration.add(avgQLoss);
                for (State each : State.states)
                    each.updateTargetQTable();
            }

            if (numEpisodes + 1 == NUM_FORGET)
                for (State each : State.states)
                    each.initializeFirstActions();

            if (numEpisodes % 500 == 0) {
                System.out.println("Episode " + numEpisodes + ". Avg state diff: " +
                        avgAvgQDiff / State.states.length / 500 * LENGTH_TO_UPDATE_VALS);
                avgAvgQDiff = 0;
            }
        }

        for (State each : State.states)
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
            oos.writeObject(State.states);
            System.out.println("NE successfully saved to file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to retrieve NE from a file
    public static void loadStatesAsObject(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            State.states = (State[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Method to save states to CSV
    public static void saveStatesToCSV(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            String header = "X_1,Y_1,X_2,Y_2:";
            for (int a = 0; a < State.NUM_AGENTS; a++)
                header += "U_" + a + "," + "D_" + a + "," + "L_" + a + "," + "R_" + a + "," /*+ "S_" + a + ","*/;

            writer.write(header.substring(0, header.length()-1));
            writer.newLine();

            for (State s : State.states) {
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
    /*public static void saveQIterationToCSV(String filename) {
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
    }*/
}
