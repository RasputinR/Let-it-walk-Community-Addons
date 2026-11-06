package dev.walkercompat.mixin;

import dev.letitwalk.content.PilotControlsBlockEntity;
import dev.walkercompat.link.LinkDriver;
import dev.walkercompat.link.LinkState;
import dev.walkercompat.link.ReceiveAction;
import dev.walkercompat.link.ReceiveHolder;
import dev.walkercompat.link.ReceiveInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives Pilot Controls a second set of frequency slots: ones it listens to. */
@Mixin(value = PilotControlsBlockEntity.class, remap = false)
public abstract class PilotControlsBlockEntityMixin implements ReceiveHolder {
    @Unique
    private static final String WALKERCOMPAT_TAG = "walker_controller_compat:receive";

    @Unique
    private final ItemStackHandler walkercompat$receive = new ReceiveInventory((PilotControlsBlockEntity) (Object) this);

    @Unique
    private final LinkState walkercompat$state = new LinkState();

    @Override
    public ItemStackHandler walkercompat$receive() {
        return walkercompat$receive;
    }

    @Override
    public LinkState walkercompat$state() {
        return walkercompat$state;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void walkercompat$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.put(WALKERCOMPAT_TAG, walkercompat$receive.serializeNBT(registries));
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void walkercompat$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains(WALKERCOMPAT_TAG)) {
            CompoundTag saved = tag.getCompound(WALKERCOMPAT_TAG);
            // keep our slot count even if an older save had a different one
            saved.putInt("Size", ReceiveAction.RECEIVE_SLOTS);
            walkercompat$receive.deserializeNBT(registries, saved);
        }
        LinkDriver.track((PilotControlsBlockEntity) (Object) this);
    }
}
