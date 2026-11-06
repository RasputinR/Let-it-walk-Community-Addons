package dev.walkercompat.mixin;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import dev.letitwalk.content.PilotControlsBlockEntity;
import dev.letitwalk.content.PilotControlsMenu;
import dev.walkercompat.link.LinkDriver;
import dev.walkercompat.link.PagedSlot;
import dev.walkercompat.link.ReceiveAction;
import dev.walkercompat.link.ReceiveHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pilot Controls screen: the original 4 send keys become page 0, and the receive actions
 * fill pages 1+, 4 per page, reusing the same slot positions.
 * Ghost inventory layout: [0..7] send frequencies, [8..] receive frequencies (2 per action).
 */
@Mixin(value = PilotControlsMenu.class, remap = false)
public abstract class PilotControlsMenuMixin extends GhostItemMenu<PilotControlsBlockEntity> {

    protected PilotControlsMenuMixin(MenuType<?> type, int id, Inventory inv, PilotControlsBlockEntity be) {
        super(type, id, inv, be);
    }

    @Inject(method = "createGhostInventory", at = @At("RETURN"), cancellable = true)
    private void walkercompat$biggerGhostInventory(CallbackInfoReturnable<ItemStackHandler> cir) {
        ItemStackHandler original = cir.getReturnValue();
        ItemStackHandler inv = new ItemStackHandler(ReceiveAction.SEND_SLOTS + ReceiveAction.RECEIVE_SLOTS);
        for (int i = 0; i < Math.min(original.getSlots(), ReceiveAction.SEND_SLOTS); i++) {
            inv.setStackInSlot(i, original.getStackInSlot(i));
        }
        if (this.contentHolder instanceof ReceiveHolder holder) {
            ItemStackHandler receive = holder.walkercompat$receive();
            for (int i = 0; i < ReceiveAction.RECEIVE_SLOTS; i++) {
                inv.setStackInSlot(ReceiveAction.SEND_SLOTS + i, receive.getStackInSlot(i).copy());
            }
        }
        cir.setReturnValue(inv);
    }

    @Redirect(method = "addSlots", at = @At(value = "NEW", target = "net/neoforged/neoforge/items/SlotItemHandler"))
    private SlotItemHandler walkercompat$sendSlotsOnPageZero(IItemHandler handler, int index, int x, int y) {
        return new PagedSlot(handler, index, x, y, 0, walkercompat$clientSide());
    }

    @Inject(method = "addSlots", at = @At("TAIL"))
    private void walkercompat$addReceiveSlots(CallbackInfo ci) {
        boolean client = walkercompat$clientSide();
        for (ReceiveAction action : ReceiveAction.ALL) {
            int page = action.ordinal() / ReceiveAction.PER_PAGE + 1;
            int column = action.ordinal() % ReceiveAction.PER_PAGE;
            for (int row = 0; row < 2; row++) {
                int index = ReceiveAction.SEND_SLOTS + action.ordinal() * 2 + row;
                // same spots as Let it Walk!'s own columns: x = 12 + 24 * column, y = 34 + 18 * row
                this.addSlot(new PagedSlot(this.ghostInventory, index, 12 + 24 * column, 34 + row * 18, page, client));
            }
        }
    }

    @Inject(method = "saveData", at = @At("HEAD"), cancellable = true)
    private void walkercompat$save(PilotControlsBlockEntity be, CallbackInfo ci) {
        ci.cancel();
        if (be == null) {
            return;
        }
        for (int i = 0; i < ReceiveAction.SEND_SLOTS; i++) {
            be.frequencies.setStackInSlot(i, this.ghostInventory.getStackInSlot(i).copy());
        }
        if (be instanceof ReceiveHolder holder) {
            ItemStackHandler receive = holder.walkercompat$receive();
            for (int i = 0; i < ReceiveAction.RECEIVE_SLOTS; i++) {
                receive.setStackInSlot(i, this.ghostInventory.getStackInSlot(ReceiveAction.SEND_SLOTS + i).copy());
            }
        }
        be.setChanged();
        if (be.getLevel() != null && !be.getLevel().isClientSide) {
            LinkDriver.track(be);
            // let clients see the new frequencies next time they open the screen
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    @Unique
    private boolean walkercompat$clientSide() {
        return this.player != null && this.player.level().isClientSide;
    }
}
