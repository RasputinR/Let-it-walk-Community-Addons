package dev.walkercompat.mixin;

import dev.letitwalk.content.WalkerCoreBlockEntity;
import dev.letitwalk.network.WalkerControlPayload;
import dev.walkercompat.link.ReceiveHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1. Limbs: Let it Walk! packs the held limb into 3 bits and caps it at 4. We add a 4th bit
 *    (bit 31, unused by the up/forward counters) so limbs 1-10 fit, and lift the cap to 10.
 * 2. Frequencies: merge the seated pilot's keys with keys held over Create frequencies.
 */
@Mixin(value = WalkerControlPayload.class, remap = false)
public abstract class WalkerControlPayloadMixin {
    @Unique
    private static final int WALKERCOMPAT_MAX_LIMB = 10;

    @Inject(method = "limbs", at = @At("HEAD"), cancellable = true)
    private static void walkercompat$packTenLimbs(int limb, int up, int fwd, CallbackInfoReturnable<Integer> cir) {
        int packed = limb & 7 | (up & 16383) << 3 | (fwd & 16383) << 17 | ((limb >> 3) & 1) << 31;
        cir.setReturnValue(packed);
    }

    @Redirect(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Ldev/letitwalk/content/WalkerCoreBlockEntity;receiveLimb(III)V"))
    private static void walkercompat$unpackTenLimbs(WalkerCoreBlockEntity core, int cappedLimb, int up, int fwd,
                                                    IPayloadContext ctx, WalkerControlPayload msg) {
        int limb = (msg.limbs() & 7) | ((msg.limbs() >>> 31) << 3);
        core.receiveLimb(Math.min(WALKERCOMPAT_MAX_LIMB, limb), up, fwd);
    }

    @Redirect(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Ldev/letitwalk/content/WalkerCoreBlockEntity;receiveInput(IJ)V"))
    private static void walkercompat$mergeFrequencyKeys(WalkerCoreBlockEntity core, int keys, long time,
                                                        IPayloadContext ctx, WalkerControlPayload msg) {
        BlockEntity be = ctx.player().level().getBlockEntity(msg.controls());
        if (be instanceof ReceiveHolder holder) {
            var state = holder.walkercompat$state();
            state.pilotKeys = keys;
            state.pilotTime = time;
            keys = state.merge(keys, true);
        }
        core.receiveInput(keys, time);
    }
}
