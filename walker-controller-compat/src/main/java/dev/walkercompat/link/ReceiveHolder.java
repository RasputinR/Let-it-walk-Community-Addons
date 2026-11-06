package dev.walkercompat.link;

import net.neoforged.neoforge.items.ItemStackHandler;

/** Added to Let it Walk!'s PilotControlsBlockEntity by mixin. */
public interface ReceiveHolder {
    /** Two frequency items per {@link ReceiveAction}, in order. */
    ItemStackHandler walkercompat$receive();

    LinkState walkercompat$state();
}
