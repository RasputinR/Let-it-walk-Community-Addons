package dev.walkercompat.link;

/**
 * Things the Pilot Controls can be told to do over a Create frequency.
 * {@code bit} is Let it Walk!'s own key bit for that control (see PilotInput), or 0 for actions we handle ourselves.
 */
public enum ReceiveAction {
    // page "Move"
    FORWARD("forward", 1),
    BACK("back", 2),
    TURN_LEFT("turn_left", 4),
    TURN_RIGHT("turn_right", 8),
    // page "Body"
    RAISE("raise", 16),
    LOWER("lower", 32),
    JUMP("jump", 64),
    SPRINT("sprint", 128),
    // page "Abilities"
    DASH("dash", 65536),
    GRAB("grab", 131072),
    FLIGHT("flight", 32768),
    CRAB("crab", 262144),   // toggles, like the keyboard key
    // page "Gears"
    GEAR_UP("gear_up", 0),
    GEAR_DOWN("gear_down", 0);

    public static final ReceiveAction[] ALL = values();
    public static final int COUNT = ALL.length;
    public static final int PER_PAGE = 4;
    /** Receive pages, after the original "Send" page 0. */
    public static final int PAGES = (COUNT + PER_PAGE - 1) / PER_PAGE;
    /** Pilot Controls' own frequency slots (4 redstone keys x 2). */
    public static final int SEND_SLOTS = 8;
    public static final int RECEIVE_SLOTS = COUNT * 2;

    public final String id;
    public final int bit;

    ReceiveAction(String id, int bit) {
        this.id = id;
        this.bit = bit;
    }

    public String shortKey() {
        return "walker_controller_compat.action." + id + ".short";
    }

    public String nameKey() {
        return "walker_controller_compat.action." + id;
    }

    /** Action shown in column {@code column} of receive page {@code page} (1-based), or null. */
    public static ReceiveAction at(int page, int column) {
        int i = (page - 1) * PER_PAGE + column;
        return page >= 1 && column >= 0 && column < PER_PAGE && i < COUNT ? ALL[i] : null;
    }
}
