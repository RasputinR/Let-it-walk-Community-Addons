package dev.walkercompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import dev.walkercompat.link.LinkDriver;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WalkerControllerCompat.MOD_ID)
public final class WalkerControllerCompat {
    public static final String MOD_ID = "walker_controller_compat";

    // Matched by registry name so this mod doesn't depend on either mod's internals.
    private static final ResourceLocation PILOT_CONTROLS =
            ResourceLocation.fromNamespaceAndPath("let_it_walk", "pilot_controls");
    private static final ResourceLocation TWEAKED_CONTROLLER =
            ResourceLocation.fromNamespaceAndPath("create_tweaked_controllers", "tweaked_linked_controller");
    private static final ResourceLocation TWEAKED_LECTERN =
            ResourceLocation.fromNamespaceAndPath("create_tweaked_controllers", "tweaked_lectern_controller");

    public WalkerControllerCompat() {
        NeoForge.EVENT_BUS.addListener(LinkDriver::onLevelTick);
    }

    public static boolean isPilotControls(BlockState state) {
        return PILOT_CONTROLS.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static boolean isTweakedLectern(BlockState state) {
        return TWEAKED_LECTERN.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static boolean isTweakedController(ItemStack stack) {
        return !stack.isEmpty() && TWEAKED_CONTROLLER.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
