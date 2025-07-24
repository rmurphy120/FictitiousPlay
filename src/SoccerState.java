import java.util.*;

public class SoccerState extends State {
    public static SoccerState[] getAllStates() {
        int numStates = 2 * (int)Math.pow(WIDTH * HEIGHT, NUM_AGENTS);
        SoccerState[] states = new SoccerState[numStates];

        for(int i = 0; i < states.length; i++)
            states[i] = indexToState(i);

        return states;
    }

    private static SoccerState indexToState(int index) {
        int[] state = new int[5];

        state[0] = index % 2;
        index /= 2;

        for(int i = 1; i < state.length; i++)
            if (i % 2 == 1) {
                state[i] = index % WIDTH;
                index /= WIDTH;
            } else {
                state[i] = index % HEIGHT;
                index /= HEIGHT;
            }

        boolean isOffBoard = false;

        for(int a = 0; a < NUM_AGENTS; a++)
            isOffBoard |= isOffBoard(state[2 * a + 1], state[2 * a + 2]);

        return new SoccerState(state, isOffBoard);
    }

    public static int getStateIndex(int[] state) {
        int index = 0;
        int base = 1;

        index += state[0];
        base *= 2;

        for(int i = 1; i < state.length; i++) {
            index += state[i] * base;
            if (i % 2 == 1)
                base *= WIDTH;
            else
                base *= HEIGHT;
        }

        return index;
    }

    private static boolean isOffBoard(int x, int y) {
        return !isInGoal(x, y) && (x < 1 || x >= WIDTH-1 || y < 0 || y >= HEIGHT);
    }

    private static boolean isInGoal(int x, int y) {
        int[] goalYPos = new int[]{HEIGHT / 2 - 1, HEIGHT / 2};
        return (x == 0 || x == WIDTH - 1) && (y == goalYPos[0] || y == goalYPos[1]);
    }

    public SoccerState(int[] state, boolean offBoard) {
        this.state = state;
        this.offBoard = offBoard;

        calculateReward();
        expectedValues = reward.clone();
    }

    private boolean samePosition(int x1, int y1, int x2, int y2) {
        return x1 == x2 && y1 == y2;
    }

    private State getState(int[] state) {
        boolean isOffBoard = false;
        for(int a = 0; a < NUM_AGENTS; a++)
            isOffBoard |= isOffBoard(state[2 * a + 1], state[2 * a + 2]);

        if (isOffBoard)
            return new SoccerState(state, true);
        else
            return states[getStateIndex(state)];
    }

    /*private void addNewStates(Map<State, Double> nextStates, int[] priorState, int[] newState,
                              double probDefenderSteals) {
        // Generates all possible next states based on ball possession and collision avoidance
        for (int i = 0; i < 2; i++) {
            int[] state = new int[2*NUM_AGENTS+1];
            state[0] = i % 2;
            for (int j = 1; j < 2*NUM_AGENTS+1; j++)
                if (j < NUM_AGENTS+1)
                    state[j] = i < 2 ? priorState[j] : newState[j];
                else
                    state[j] = i < 2 ? newState[j] : priorState[j];

            State s = getState(state);
            double basePercent = nextStates.getOrDefault(s, 0.);
            nextStates.put(s, 0.5 * (state[0] == priorState[0] ? 1-probDefenderSteals : probDefenderSteals) + basePercent);
        }
    }*/

    public Map<State, Double> transition(ActionSpace[] actions) {
        Map<State, Double> nextStates = new HashMap<>();

        int[][] moveOrders = new int[][]{{0, 1}, {1, 0}};

        for(int[] moveOrder : moveOrders) {
            int[] newState = state.clone();

            for(int a = 0; a < NUM_AGENTS; a++) {
                switch (actions[moveOrder[a]]) {
                    case LEFT -> newState[2*moveOrder[a]+1]--;
                    case RIGHT -> newState[2*moveOrder[a]+1]++;
                    case UP -> newState[2*moveOrder[a]+2]--;
                    case DOWN -> newState[2*moveOrder[a]+2]++;
                }

                // Agent runs into other agent
                if (samePosition(newState[1], newState[2], newState[3], newState[4])) {
                    switch (actions[moveOrder[a]]) {
                        case LEFT -> newState[2*moveOrder[a]+1]++;
                        case RIGHT -> newState[2*moveOrder[a]+1]--;
                        case UP -> newState[2*moveOrder[a]+2]++;
                        case DOWN -> newState[2*moveOrder[a]+2]--;
                    }
                    newState[0] = moveOrder[a] == 1 ? 0 : 1;
                }
            }

            State s = getState(newState);
            double basePercent = nextStates.getOrDefault(s, 0.);
            nextStates.put(s, basePercent + 1./moveOrders.length);
        }

        /*
        int[] newState = state.clone();

        for(int a = 0; a < NUM_AGENTS; a++)
            switch (actions[a]) {
                case LEFT -> newState[2*a+1]--;
                case RIGHT -> newState[2*a+1]++;
                case UP -> newState[2*a+2]--;
                case DOWN -> newState[2*a+2]++;
            }

        // Ball player
        int b = state[0];
        // Non ball player
        int nb = state[0] == 1 ? 0 : 1;

        // Onto same tile
        if (samePosition(newState[1], newState[2], newState[3], newState[4]))
            addNewStates(nextStates, state, newState, .85);
        // Ball onto non ball
        else if (samePosition(newState[2*b+1], newState[2*b+2], state[2*nb+1], state[2*nb+2]))
            // Non ball didn't move
            if (samePosition(newState[2*b+1], newState[2*b+2], newState[2*nb+1], newState[2*nb+2]))
                addNewStates(nextStates, state, newState, .5);
            // Ball and non ball swapped
            else if (samePosition(state[2*b+1], state[2*b+2], newState[2*nb+1], newState[2*nb+2]))
                addNewStates(nextStates, state, newState, .5);
            // Non ball moved away
            else
                addNewStates(nextStates, state, newState, .25);
        // Non ball onto ball
        else if (samePosition(state[2*b+1], state[2*b+2], newState[2*nb+1], newState[2*nb+2]))
            // Ball didn't move
            if (samePosition(newState[2*b+1], newState[2*b+2], newState[2*nb+1], newState[2*nb+2]))
                addNewStates(nextStates, state, newState, .75);
            // Ball moved away
            else
                addNewStates(nextStates, state, newState, .25);
        // Players don't interact
        else
            nextStates.put(getState(newState), 1.);*/

        return nextStates;
    }

    public void calculateReward() {
        reward = new double[NUM_AGENTS];

        if (offBoard) {
            boolean[] isOffBoard = new boolean[NUM_AGENTS];
            for(int i = 0; i < NUM_AGENTS; i++)
                isOffBoard[i] = isOffBoard(state[2 * i + 1], state[2 * i + 2]);

            if (isOffBoard[0] && isOffBoard[1]) {
                reward[0] = -100.0;
                reward[1] = -100.0;
            } else if (isOffBoard[0]) {
                reward[0] = -100.0;
                reward[1] = 100.0;
            } else if (isOffBoard[1]) {
                reward[0] = 100.0;
                reward[1] = -100.0;
            }
        } else
            for(int a = 0; a < NUM_AGENTS; a++)
                if (state[0] == a && isInGoal(state[2 * a + 1], state[2 * a + 2]))
                    for(int i = 0; i < NUM_AGENTS; i++)
                        reward[i] = 10 * (i == 0 ^ state[2 * a + 1] == 0 ? 1 : -1);
    }
}
