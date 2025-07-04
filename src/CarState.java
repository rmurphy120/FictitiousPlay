public class CarState extends State {
    public static State[] getAllStates() {
        CarState[] states = new CarState[(int)Math.pow(WIDTH * HEIGHT, NUM_AGENTS)];

        for (int i = 0; i < states.length; i++)
            states[i] = indexToState(i);

        return states;
    }

    private static CarState indexToState(int index) {
        int[] s = new int[2*NUM_AGENTS];

        for (int i = 0; i < s.length; i++) {
            if (i % 2 == 0) {
                s[i] = index % WIDTH;
                index /= WIDTH;
            }
            else {
                s[i] = index % HEIGHT;
                index /= HEIGHT;
            }
        }

        return new CarState(s, false);
    }

    public static int getStateIndex(int[] state) {
        int index = 0;
        int base = 1;

        for (int i = 0; i < state.length; i++) {
            index += state[i] * base;

            if (i % 2 == 0)
                base *= WIDTH;
            else
                base *= HEIGHT;
        }

        return index;
    }


    public CarState(int[] state, boolean offBoard) {
        this.state = state;
        this.offBoard = offBoard;
        calculateReward();
        expectedValues = reward.clone();
    }


    public State transition(ActionSpace[] actions) {
        int[] newState = state.clone();
        for (int a = 0; a < NUM_AGENTS; a++)
            switch (actions[a]) {
                case LEFT -> newState[2*a]--;
                case RIGHT -> newState[2*a]++;
                case UP -> newState[2*a+1]--;
                case DOWN -> newState[2*a+1]++;
            }

        // Off the board, so not a part of states
        for (int a = 0; a < NUM_AGENTS; a++)
            if (newState[2*a] < 0 || newState[2*a] >= WIDTH ||
                    newState[2*a+1] < 0 || newState[2*a+1] >= HEIGHT)
                return new CarState(newState, true);

        return states[getStateIndex(newState)];
    }

    public void calculateReward() {
        reward = new double[NUM_AGENTS];

        // Off map punishment (If both run off the map, the rewards don't sum up to 0, but okay since neither will take
        // this action)
        if (offBoard) {
            boolean[] isOffMap = new boolean[NUM_AGENTS];
            for (int a = 0; a < NUM_AGENTS; a++)
                if (state[2*a] < 0 || state[2*a] >= WIDTH ||
                        state[2*a+1] < 0 || state[2*a+1] >= HEIGHT)
                    isOffMap[a] = true;

            if (isOffMap[0] && isOffMap[1]){
                reward[0] = -100;
                reward[1] = -100;
            }
            else if (isOffMap[0]) {
                reward[0] = -100;
                reward[1] = 100;
            }
            else if (isOffMap[1]) {
                reward[0] = 100;
                reward[1] = -100;
            }
            /*if (isOffMap[0] && isOffMap[1] && isOffMap[2]) {
                reward[0] = -2000;
                reward[1] = -1000;
                reward[2] = -1000;
            } else if (isOffMap[0]) {
                reward[0] = -2000;
                reward[1] = 1000;
                reward[2] = 1000;
            } else {
                // One of the evaders is off the map
                reward[0] = 2000;
                reward[1] = -1000;
                reward[2] = -1000;
            }*/

            return;
        }

        int[] centerOfMap = new int[]{WIDTH / 2, HEIGHT / 2};

        // Calculate evader's reward
        for (int a = 0; a < NUM_AGENTS; a++) {
            int pursuerMult = a == 0 ? -1 : 1;
            // Base tile reward for first agent (Calculated using the Manhattan distance from the center)
            reward[1] += pursuerMult * (centerOfMap[0] + centerOfMap[1] - Math.abs(centerOfMap[0] - state[2*a]) -
                    Math.abs(centerOfMap[1] - state[2*a+1]));

            // Crash punishment for first agent
            for (int i = a + 1; i < reward.length; i++)
                if (i != a && state[2*a] == state[2*i] && state[2*a+1] == state[2*i+1])
                    reward[1] -= 10;
        }

        // reward[2] = reward[1];

        // Make reward for second agent so that it's a 0 sum game
        // reward[0] = -2 * reward[1];
        reward[0] = -reward[1];
    }


}
