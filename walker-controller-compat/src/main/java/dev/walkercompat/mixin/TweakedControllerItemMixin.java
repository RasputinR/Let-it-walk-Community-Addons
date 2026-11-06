package dev.walkercompat.mixin;

import dev.walkercompat.WalkerControllerCompat;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-clicking Pilot Controls while holding a Tweaked Controller used to toggle the controller
 * (usually switching it OFF) before piloting started. Now the click goes straight to the
 * Pilot Controls and the controller keeps whatever state it was in.
 */
@Mixin(targets = "com.getitemfromblock.create_tweaked_controllers.item.TweakedLinkedControllerItem", remap = false)
public abstract class TweakedControllerItemMixin {

    @Inject(method = "onItemUseFirst", at = @At("HEAD"), cancellable = true)
    private void walkercompat$passToPilotControls(ItemStack stack, UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = ctx.getPlayer();
        if (player != null && !player.isShiftKeyDown()
                && WalkerControllerCompat.isPilotControls(ctx.getLevel().getBlockState(ctx.getClickedPos()))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
