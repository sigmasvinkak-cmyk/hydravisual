package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * FPS Display — показывает FPS на экране
 */
public class FPSDisplay extends Module {
    public FPSDisplay() {
        super("FPS", "Показывает FPS на экране", Category.HUD);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    @Override
    public void onRender(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // FPS is rendered via HUD mixin
    }

    public String getFPSText() {
        MinecraftClient client = MinecraftClient.getInstance();
        int fps = client.getCurrentFps();
        return "FPS: " + fps;
    }
}
