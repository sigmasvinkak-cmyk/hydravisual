package com.hydravisual.module;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all modules
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled = false;
    private int keyBind = -1;
    private final List<Setting> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    public abstract void onEnable();
    public abstract void onDisable();

    public void onTick() {}
    public void onRender(float tickDelta) {}

    // Settings
    protected Setting addSetting(Setting s) { settings.add(s); return s; }
    public List<Setting> getSettings() { return settings; }
    public boolean hasSettings() { return !settings.isEmpty(); }

    // Getters / Setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { if (this.enabled != enabled) toggle(); }
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int keyBind) { this.keyBind = keyBind; }

    public enum Category {
        VISUAL("Visual"),
        RENDER("Render"),
        HUD("HUD"),
        UTILITY("Utility");

        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}
