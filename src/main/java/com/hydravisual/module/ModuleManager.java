package com.hydravisual.module;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.modules.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all HydraVisual modules
 */
public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // === VISUAL ===
        register(new Fullbright());

        // === HUD ===
        register(new FPSDisplay());
        register(new CoordsDisplay());

        HydraVisualClient.LOGGER.info("ModuleManager initialized with {} modules", modules.size());
    }

    public void register(Module module) {
        modules.add(module);
        HydraVisualClient.LOGGER.info("Registered module: {}", module.getName());
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Module> getModulesByCategory(Module.Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .toList();
    }

    public int getModuleCount() {
        return modules.size();
    }

    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    public void onRender(float tickDelta) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender(tickDelta);
            }
        }
    }
}
