package dev.walkercompat.client;

import dev.walkercompat.WalkerControllerCompat;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = WalkerControllerCompat.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CompatKeys {
    /** Limbs 1-9 use the hotbar keys like Let it Walk! does; limb 10 gets its own key (default 0). */
    public static final KeyMapping LIMB_10 =
            new KeyMapping("key.walker_controller_compat.limb_10", GLFW.GLFW_KEY_0, "key.categories.let_it_walk");

    private CompatKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(LIMB_10);
    }
}
