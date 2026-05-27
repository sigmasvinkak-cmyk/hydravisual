package com.hydravisual;

import net.fabricmc.api.ClientModInitializer;
import com.hydravisual.module.ModuleManager;
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

        LOGGER.info("✅ Загружено {} модулей", moduleManager.getModuleCount());
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
