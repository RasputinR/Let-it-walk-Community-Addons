package dev.walkercompat.link;

/** Per-Pilot-Controls runtime state for frequency driving (server side, not saved). */
public final class LinkState {
    /** WalkerBrain.KEY_LATCH in Let it Walk! 0.1.5+. */
    private static final int KEY_LATCH = 524288;

    /** Walker key bits currently held over frequencies (no gear bits). */
    public int linkKeys;
    /** Whether a frequency-driven turn (analog or key) is active. */
    public boolean linkTurning;
    /** Gear chosen with the Gear Up / Gear Down frequencies. */
    public int gear = 1;
    public boolean crab;
    public boolean crabWasOn;
    public boolean gearUpWasOn;
    public boolean gearDownWasOn;

    /** Last keys the seated pilot sent, and when. */
    public int pilotKeys;
    public long pilotTime = Long.MIN_VALUE / 2;

    public boolean pilotRecent(long now) {
        return now - pilotTime <= 10L;
    }

    /** Combine pilot keyboard input with frequency input. */
    public int merge(int pilot, boolean pilotActive) {
        int keys = pilotActive ? pilot : 0;
        keys |= linkKeys;
        if (linkKeys != 0) {
            // frequency input takes over from Let it Walk!'s cruise-control latch (KEY_LATCH, bit 19),
            // which would otherwise make the walker ignore everything but the latched walk keys
            keys &= ~KEY_LATCH;
        }
        if ((linkKeys & 0xFF) != 0) {
            // driving over frequencies: use the frequency gear
            keys = (keys & ~(7 << 8)) | (gear << 8);
        }
        return keys;
    }
}
