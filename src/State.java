import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class State implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public static final int GAME = 1;
    public static final int HEIGHT = 4;
    public static final int WIDTH = 7;
    public static final int NUM_AGENTS = 2;

    public static State[] states = GAME == 0 ? CarState.getAllStates() : SoccerState.getAllStates();

    public int[] state;
    protected boolean offBoard;
    public double[][] NE;
    public double[] reward;

    private Map<ActionSpace[], double[]> QTable;
    private Map<ActionSpace[], double[]> TargetQTable;
    protected double[] expectedValues;
    private int[][] pastActions;
    private int[][] firstActions;

    public abstract Map<State, Double> transition(ActionSpace[] var1);

    public abstract void calculateReward();

    public void initializeForFictitiousPlay() {
        if (offBoard)
            return;

        QTable = new HashMap<>();
        for (ActionSpace[] each : ActionSpace.ALL_ACTIONS) {
            Map<State, Double> nextStates = transition(each);

            double[] initValues = new double[NUM_AGENTS];
            for (State nextState : nextStates.keySet())
                for (int a = 0; a < NUM_AGENTS; a++)
                    initValues[a] += nextStates.get(nextState) * nextState.reward[a];

            QTable.put(each, initValues);
        }
        TargetQTable = new HashMap<>(QTable);

        pastActions = new int[NUM_AGENTS][ActionSpace.values().length];
        for (int a = 0; a < NUM_AGENTS; a++)
            for (int i = 0; i < ActionSpace.values().length; i++)
                pastActions[a][i]++;
    }

    public void updatePastActions() {
        if (offBoard)
            return;

        ActionSpace[] actions = bestResponse();
        for (int a = 0; a < NUM_AGENTS; a++)
            pastActions[a][actions[a].ordinal()]++;
    }

    public void initializeFirstActions() {
        if (offBoard)
            return;

        firstActions = new int[NUM_AGENTS][ActionSpace.values().length];
        for (int a = 0; a < NUM_AGENTS; a++)
            firstActions[a] = pastActions[a].clone();
    }

    public void forgetFirstActions() {
        if (offBoard)
            return;

        for (int a = 0; a < NUM_AGENTS; a++)
            for (int i = 0; i < ActionSpace.values().length; i++)
                pastActions[a][i] -= firstActions[a][i];
    }

    private double getPercentAction(int agent, int action) {
        return pastActions[agent][action] / (double) NashSolver.numEpisodes;
    }

    public double updateQTable() {
        if (offBoard)
            return 0;

        double avgDifference = 0;
        for (ActionSpace[] each : ActionSpace.ALL_ACTIONS) {
            Map<State, Double> nextStates = transition(each);

            double[] newValues = new double[NUM_AGENTS];
            for (State nextState : nextStates.keySet())
                for (int a = 0; a < NUM_AGENTS; a++)
                    newValues[a] += nextStates.get(nextState) * nextState.expectedValues[a];

            double[] oldValues = QTable.replace(each, newValues);

            for (int a = 0; a < NUM_AGENTS; a++)
                avgDifference += Math.abs(oldValues[a] - newValues[a]);
        }

        return avgDifference / (double) (NUM_AGENTS * ActionSpace.ALL_ACTIONS.length);
    }

    public void updateTargetQTable() {
        if (offBoard)
            return;

        TargetQTable = new HashMap<>(QTable);
    }

    private ActionSpace[] bestResponse() {
        double[][] expectedValues = new double[NUM_AGENTS][ActionSpace.values().length];

        for (ActionSpace[] each : ActionSpace.ALL_ACTIONS) {
            double[] QValues = TargetQTable.get(each);
            double mult = 1;

            for (int a = 0; a < NUM_AGENTS; a++)
                mult *= getPercentAction(a, each[a].ordinal());

            for (int a = 0; a < NUM_AGENTS; a++) {
                int currAgentAction = each[a].ordinal();
                expectedValues[a][currAgentAction] += QValues[a] * mult / getPercentAction(a, currAgentAction);
            }
        }

        ActionSpace[] bestResponses = new ActionSpace[NUM_AGENTS];

        for (int a = 0; a < NUM_AGENTS; a++) {
            double maxVal = Integer.MIN_VALUE;

            for (int i = 0; i < ActionSpace.values().length; i++)
                if (expectedValues[a][i] > maxVal) {
                    maxVal = expectedValues[a][i];
                    bestResponses[a] = ActionSpace.values()[i];
                }
        }

        return bestResponses;
    }

    public void updateExpectedValues() {
        if (offBoard)
            return;

        expectedValues = reward.clone();

        for (ActionSpace[] each : ActionSpace.ALL_ACTIONS) {
            double[] QValues = TargetQTable.get(each);
            double mult = NashSolver.DISCOUNT_FACTOR;

            for (int a = 0; a < NUM_AGENTS; a++)
                mult *= getPercentAction(a, each[a].ordinal());

            for (int a = 0; a < NUM_AGENTS; a++)
                expectedValues[a] += QValues[a] * mult;
        }
    }

    public void calculateNE() {
        if (offBoard)
            return;

        NE = new double[NUM_AGENTS][ActionSpace.values().length];

        for (int a = 0; a < NUM_AGENTS; a++)
            for (int i = 0; i < ActionSpace.values().length; i++)
                NE[a][i] = getPercentAction(a, i);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof State ? Arrays.equals(state, ((State) o).state) : false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(state);
    }
}
