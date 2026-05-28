package com.hydravisual;

import com.hydravisual.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HydraVisualClient implements ClientModInitializer {
    public static final String MOD_ID = "hydravisual";
    public static final String MOD_NAME = "HydraVisual";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static HydraVisualClient INSTANCE;
    private ModuleManager moduleManager;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("🐉 {} v1.0.0 загружен!", MOD_NAME);

        // Initialize module system
        moduleManager = new ModuleManager();
        moduleManager.init();

        // Register keybindings
        KeyBindManager.init();

        // Register HUD render callback (Fabric API)
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            moduleManager.onRender(drawContext);
        });

        // Register tick callback
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            moduleManager.onTick();
        });

        LOGGER.info("✅ Загружено {} модулей", moduleManager.getModuleCount());
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
