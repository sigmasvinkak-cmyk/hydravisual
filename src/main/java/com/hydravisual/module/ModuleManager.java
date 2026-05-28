package com.hydravisual.module;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.modules.*;
import net.minecraft.client.gui.DrawContext;

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

    public void onRender(DrawContext context) {
        int y = 4;

        for (Module module : modules) {
            if (module.isEnabled() && module instanceof HudModule hudModule) {
                String text = hudModule.getHudText();
                if (text != null && !text.isEmpty()) {
                    context.drawText(
                        net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                        text, 4, y, hudModule.getColor(), true
                    );
                    y += 12;
                }
            }
        }
    }
}
