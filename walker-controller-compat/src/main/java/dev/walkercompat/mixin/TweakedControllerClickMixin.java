package dev.walkercompat.mixin;

import dev.letitwalk.client.PilotInput;
import dev.walkercompat.ClientCompat;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaked Controllers kicks you out of a Tweaked Lectern on ANY click. Don't do that when the click
 * is on Pilot Controls (to start piloting) or while you're already piloting.
 */
@Mixin(targets = "com.getitemfromblock.create_tweaked_controllers.input.ModInputEvents", remap = false)
public abstract class TweakedControllerClickMixin {

    @Inject(method = "onClickInput", at = @At("HEAD"), cancellable = true)
    private static void walkercompat$stayInLectern(InputEvent.InteractionKeyMappingTriggered event, CallbackInfo ci) {
        if (PilotInput.isActive() || (event.isUseItem() && ClientCompat.lookingAtPilotControls())) {
            ci.cancel();
        }
    }
}
