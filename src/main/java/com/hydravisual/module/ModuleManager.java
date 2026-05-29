package com.hydravisual.module;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.modules.*;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all modules
 */
public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // === VISUAL ===
        register(new Fullbright());

        // === HUD ===
        register(new HudOverlayModule());

        // === UTILITY ===
        register(new HitSound());

        HydraVisualClient.LOGGER.info("ModuleManager initialized with {} modules", modules.size());
    }

    public void register(Module module) {
        modules.add(module);
        HydraVisualClient.LOGGER.info("Registered module: {}", module.getName());
    }

    public List<Module> getModules() { return modules; }

    public Module getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public int getModuleCount() { return modules.size(); }

    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) module.onTick();
        }
    }

    /**
     * Called on key press — checks all module keybinds
     */
    public void onKeyPress(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return;
        for (Module module : modules) {
            if (module.getKeyBind() == keyCode) {
                module.toggle();
            }
        }
    }

    /**
     * Render HUD overlay (non-HudOverlayModule rendering removed,
     * HudOverlayModule handles its own rendering)
     */
    public void onRender(DrawContext context) {
        for (Module module : modules) {
            if (module instanceof HudOverlayModule hud) {
                hud.renderHud(context);
            }
        }
    }

    /** Get the HUD module for drag handling */
    public HudOverlayModule getHudModule() {
        for (Module m : modules) {
            if (m instanceof HudOverlayModule h) return h;
        }
        return null;
    }

    /** Get the HitSound module for mixin access */
    public HitSound getHitSoundModule() {
        for (Module m : modules) {
            if (m instanceof HitSound h) return h;
        }
        return null;
    }
}
