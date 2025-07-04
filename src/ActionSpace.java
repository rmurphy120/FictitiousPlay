public enum ActionSpace {
    UP,
    DOWN,
    LEFT,
    RIGHT/*,
    STOP*/;

    public static final ActionSpace[][] ALL_ACTIONS = calculateAllActions();

    private static ActionSpace[][] calculateAllActions() {
        ActionSpace[][] allActions = new ActionSpace[
                (int)Math.pow(ActionSpace.values().length, State.NUM_AGENTS)][State.NUM_AGENTS];

        for (int i = 0; i < allActions.length; i++)
            allActions[i] = indexToActions(i);

        return allActions;
    }

    private static ActionSpace[] indexToActions(int index) {
        ActionSpace[] actions = new ActionSpace[State.NUM_AGENTS];

        for (int a = 0; a < State.NUM_AGENTS; a++) {
            actions[a] = ActionSpace.values()[index % values().length];
            index /= values().length;
        }

        return actions;
    }
}