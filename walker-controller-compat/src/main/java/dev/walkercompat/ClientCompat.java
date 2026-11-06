package dev.walkercompat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only checks used by the client mixins. */
public final class ClientCompat {
    private ClientCompat() {
    }

    /**
     * While piloting, should a right-click reach Minecraft instead of being swallowed by the walker?
     * True when the player holds a Tweaked Controller or is looking at a Tweaked Lectern Controller.
     */
    public static boolean letUseThrough() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return false;
        }
        if (WalkerControllerCompat.isTweakedController(player.getMainHandItem())
                || WalkerControllerCompat.isTweakedController(player.getOffhandItem())) {
            return true;
        }
        BlockState target = targetedBlock();
        return target != null && WalkerControllerCompat.isTweakedLectern(target);
    }

    public static boolean isUseKey(KeyMapping key) {
        return key == Minecraft.getInstance().options.keyUse;
    }

    public static boolean lookingAtPilotControls() {
        BlockState target = targetedBlock();
        return target != null && WalkerControllerCompat.isPilotControls(target);
    }

    private static BlockState targetedBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return mc.level.getBlockState(hit.getBlockPos());
        }
        return null;
    }
}
