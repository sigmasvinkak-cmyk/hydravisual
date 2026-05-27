package com.hydravisual.module.modules;

import com.hydravisual.module.HudModule;
import com.hydravisual.module.Module;
import net.minecraft.client.MinecraftClient;

/**
 * FPS Display — показывает FPS на экране
 */
public class FPSDisplay extends Module implements HudModule {
    public FPSDisplay() {
        super("FPS", "Показывает FPS на экране", Category.HUD);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    @Override
    public String getHudText() {
        return "FPS: " + MinecraftClient.getInstance().getCurrentFps();
    }

    @Override
    public int getColor() {
        return 0xFF00FF88;
    }
}
