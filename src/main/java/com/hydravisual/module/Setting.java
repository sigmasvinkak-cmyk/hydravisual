package com.hydravisual.module;

import java.util.List;

/**
 * Module setting — slider, toggle, or enum (list of options)
 */
public class Setting {
    public enum Type { SLIDER, TOGGLE, ENUM }

    private final String name;
    private final Type type;

    // Slider
    private double value;
    private double min, max, step;

    // Toggle
    private boolean enabled;

    // Enum
    private List<String> options;
    private int selectedIndex;

    private Runnable onChange;

    /** Slider */
    public Setting(String name, double value, double min, double max, double step) {
        this.name = name; this.type = Type.SLIDER;
        this.value = value; this.min = min; this.max = max; this.step = step;
    }

    /** Toggle */
    public Setting(String name, boolean enabled) {
        this.name = name; this.type = Type.TOGGLE;
        this.enabled = enabled;
        this.value = 0; this.min = 0; this.max = 1; this.step = 1;
    }

    /** Enum */
    public Setting(String name, List<String> options, int selectedIndex) {
        this.name = name; this.type = Type.ENUM;
        this.options = options; this.selectedIndex = Math.max(0, Math.min(selectedIndex, options.size()-1));
        this.value = 0; this.min = 0; this.max = options.size()-1; this.step = 1;
    }

    public String getName() { return name; }
    public Type getType() { return type; }

    // Slider
    public double getValue() { return value; }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public void setValue(double v) {
        this.value = Math.max(min, Math.min(max, v));
        if (step > 0) this.value = Math.round(this.value / step) * step;
        if (onChange != null) onChange.run();
    }

    // Toggle
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; if (onChange != null) onChange.run(); }
    public void toggle() { setEnabled(!enabled); }

    // Enum
    public List<String> getOptions() { return options; }
    public int getSelectedIndex() { return selectedIndex; }
    public String getSelected() { return options != null ? options.get(selectedIndex) : ""; }
    public void setSelectedIndex(int i) {
        this.selectedIndex = Math.max(0, Math.min(i, options.size()-1));
        if (onChange != null) onChange.run();
    }
    public void nextOption() { setSelectedIndex((selectedIndex + 1) % options.size()); }
    public void prevOption() { setSelectedIndex((selectedIndex - 1 + options.size()) % options.size()); }

    public Setting onChange(Runnable r) { this.onChange = r; return this; }
}
