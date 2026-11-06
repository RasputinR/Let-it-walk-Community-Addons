package dev.walkercompat.link;

/** Per-Pilot-Controls runtime state for frequency driving (server side, not saved). */
public final class LinkState {
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
        if ((linkKeys & 0xFF) != 0) {
            // driving over frequencies: use the frequency gear
            keys = (keys & ~(7 << 8)) | (gear << 8);
        }
        return keys;
    }
}
