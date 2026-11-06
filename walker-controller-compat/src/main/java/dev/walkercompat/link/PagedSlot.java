package dev.walkercompat.link;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** A frequency slot that is only shown while its page of the Pilot Controls screen is open. */
public class PagedSlot extends SlotItemHandler {
    /** Page the Pilot Controls screen is showing (client only; 0 = Send). */
    public static int shownPage = 0;

    private final int page;
    private final boolean clientSide;

    public PagedSlot(IItemHandler handler, int index, int x, int y, int page, boolean clientSide) {
        super(handler, index, x, y);
        this.page = page;
        this.clientSide = clientSide;
    }

    @Override
    public boolean isActive() {
        return !clientSide || shownPage == page;
    }
}
