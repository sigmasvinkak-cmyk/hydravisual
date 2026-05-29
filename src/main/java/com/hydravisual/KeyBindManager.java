package com.hydravisual;

import com.hydravisual.gui.HydraScreen;
import com.hydravisual.module.modules.HudOverlayModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers keybindings for the mod.
 * Right Shift — open/close the menu.
 * Also processes per-module keybinds on key press.
 */
public class KeyBindManager {
    private static KeyBinding menuKey;

    public static void init() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Open Menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "SeladalaVisual"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Menu toggle
            while (menuKey.wasPressed()) {
                if (client.currentScreen instanceof HydraScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new HydraScreen());
                }
            }
        });

        // Register raw key callback for module binds
        // This is done once the window is available
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() == null) return;
            // We handle keybinds through the InputUtil polling approach in onTick
        });
    }

    /**
     * Called from a GLFW key callback or tick to check module binds.
     * We poll the keyboard state each tick instead.
     */
    private static final boolean[] keyStates = new boolean[512];

    public static void pollKeyBinds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        if (client.currentScreen != null) return; // Don't trigger binds in GUIs

        long handle = client.getWindow().getHandle();
        var mgr = HydraVisualClient.INSTANCE.getModuleManager();

        for (var module : mgr.getModules()) {
            int bind = module.getKeyBind();
            if (bind <= 0 || bind >= keyStates.length) continue;

            boolean pressed = GLFW.glfwGetKey(handle, bind) == GLFW.GLFW_PRESS;
            if (pressed && !keyStates[bind]) {
                module.toggle();
            }
            keyStates[bind] = pressed;
        }
    }
}
