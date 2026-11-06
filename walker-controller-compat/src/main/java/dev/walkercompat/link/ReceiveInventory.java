package dev.walkercompat.link;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The receive-frequency slots stored on a Pilot Controls block entity. */
public final class ReceiveInventory extends ItemStackHandler {
    private final BlockEntity owner;

    public ReceiveInventory(BlockEntity owner) {
        super(ReceiveAction.RECEIVE_SLOTS);
        this.owner = owner;
    }

    @Override
    protected void onContentsChanged(int slot) {
        owner.setChanged();
    }
}
