package dev.walkercompat.mixin;

import dev.walkercompat.ClientCompat;
import dev.walkercompat.client.CompatKeys;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Let it Walk! swallows every right-click while you pilot (it uses them as redstone keys).
 * This lets right-clicks through when you hold a Tweaked Controller or look at a Tweaked Lectern,
 * so you can switch the controller on/off without leaving the pilot seat.
 */
@Mixin(targets = "dev.letitwalk.client.PilotInput", remap = false)
public abstract class PilotInputMixin {

    @Redirect(method = "onClientTickPre",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private static boolean walkercompat$keepUseClick(KeyMapping key) {
        if (ClientCompat.isUseKey(key) && ClientCompat.letUseThrough()) {
            return false;
        }
        return key.consumeClick();
    }

    @Redirect(method = "onClientTickPre",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
    private static void walkercompat$keepUseHeld(KeyMapping key, boolean down) {
        if (ClientCompat.isUseKey(key) && ClientCompat.letUseThrough()) {
            return;
        }
        key.setDown(down);
    }

    // ---- Limbs 1-10 (Let it Walk! only reads hotbar keys 1-4) ----

    /** The hotbar-key loop in onClientTickPre (swallows the clicks so the hotbar doesn't change): 4 -> 10 keys. */
    @ModifyConstant(method = "onClientTickPre", constant = @Constant(intValue = 4, ordinal = 0))
    private static int walkercompat$swallowTenLimbKeys(int four) {
        return 10;
    }

    /** The limb-selection loop in onClientTick: 4 -> 10 keys. (ordinal 0 is "keys |= 4", the left key) */
    @ModifyConstant(method = "onClientTick", constant = @Constant(intValue = 4, ordinal = 1))
    private static int walkercompat$readTenLimbKeys(int four) {
        return 10;
    }

    /** Hotbar keys 1-9 are limbs 1-9; index 9 (past the hotbar) is our "Limb 10" key. */
    @Redirect(method = {"onClientTickPre", "onClientTick"}, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Options;keyHotbarSlots:[Lnet/minecraft/client/KeyMapping;", args = "array=get"))
    private static KeyMapping walkercompat$limbKey(KeyMapping[] hotbar, int index) {
        return index < hotbar.length ? hotbar[index] : CompatKeys.LIMB_10;
    }

    // ---- Tweaked Controller while piloting ----

    @Inject(method = "onInteraction", at = @At("HEAD"), cancellable = true)
    private static void walkercompat$allowControllerUse(InputEvent.InteractionKeyMappingTriggered event, CallbackInfo ci) {
        if (event.isUseItem() && ClientCompat.letUseThrough()) {
            ci.cancel(); // skip the walker's cancel so the use reaches the controller / lectern
        }
    }
}
