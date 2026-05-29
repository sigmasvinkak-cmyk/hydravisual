package com.hydravisual;

import com.hydravisual.module.ModuleManager;
import com.hydravisual.module.modules.HudOverlayModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HydraVisualClient implements ClientModInitializer {
    public static final String MOD_ID = "hydravisual";
    public static final String MOD_NAME = "SeladalaVisual";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static HydraVisualClient INSTANCE;
    private ModuleManager moduleManager;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("🐉 {} v1.3.0 загружен!", MOD_NAME);

        moduleManager = new ModuleManager();
        moduleManager.init();

        KeyBindManager.init();

        // HUD rendering
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            moduleManager.onRender(drawContext);
        });

        // Tick — modules + keybind polling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            moduleManager.onTick();
            KeyBindManager.pollKeyBinds();
            // HUD drag in chat
            var hud = moduleManager.getHudModule();
            if (hud != null) hud.tickDrag();
        });

        LOGGER.info("✅ Загружено {} модулей", moduleManager.getModuleCount());
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
