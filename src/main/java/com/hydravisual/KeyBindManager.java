package com.hydravisual;

import com.hydravisual.module.Module;
import com.hydravisual.module.ModuleManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers keybindings for HydraVisual modules
 */
public class KeyBindManager {
    private static KeyBinding fullbrightKey;
    private static KeyBinding hudKey;

    public static void init(ModuleManager manager) {
        fullbrightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Fullbright",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "HydraVisual"
        ));

        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Toggle HUD",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "HydraVisual"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (fullbrightKey.wasPressed()) {
                Module fb = manager.getModule("Fullbright");
                if (fb != null) fb.toggle();
            }
            while (hudKey.wasPressed()) {
                Module fps = manager.getModule("FPS");
                Module coords = manager.getModule("Coords");
                if (fps != null) fps.toggle();
                if (coords != null) coords.toggle();
            }
        });
    }
}
