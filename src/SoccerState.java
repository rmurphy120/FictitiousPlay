import java.util.HashSet;
import java.util.Set;

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

    public Set<State> transition(ActionSpace[] actions) {
        Set<State> nextStates = new HashSet<>();
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

                if (a == 0 && newState[0] != moveOrder[a] && newState[1] == newState[3] && newState[2] == newState[4])
                    newState[0] = moveOrder[a];
            }

            boolean isOffBoard = false;
            for(int a = 0; a < NUM_AGENTS; a++)
                isOffBoard |= isOffBoard(newState[2 * a + 1], newState[2 * a + 2]);

            if (isOffBoard)
                nextStates.add(new SoccerState(newState, true));
            else
                nextStates.add(states[getStateIndex(newState)]);
        }

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
