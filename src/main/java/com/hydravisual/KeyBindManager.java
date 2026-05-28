package com.hydravisual;

import com.hydravisual.gui.HydraScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers keybindings for HydraVisual
 * Right Shift — open/close the menu
 */
public class KeyBindManager {
    private static KeyBinding menuKey;

    public static void init() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Open Menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "HydraVisual"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen instanceof HydraScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new HydraScreen());
                }
            }
        });
    }
}
