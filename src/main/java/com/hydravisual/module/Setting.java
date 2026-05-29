package com.hydravisual.module;

/**
 * Module setting — slider or toggle
 */
public class Setting {
    public enum Type { SLIDER, TOGGLE }

    private final String name;
    private final Type type;

    // Slider fields
    private double value;
    private double min;
    private double max;
    private double step;

    // Toggle field
    private boolean enabled;

    // Callback
    private Runnable onChange;

    /** Slider constructor */
    public Setting(String name, double value, double min, double max, double step) {
        this.name = name;
        this.type = Type.SLIDER;
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    /** Toggle constructor */
    public Setting(String name, boolean enabled) {
        this.name = name;
        this.type = Type.TOGGLE;
        this.enabled = enabled;
        this.value = 0; this.min = 0; this.max = 1; this.step = 1;
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
        // Snap to step
        if (step > 0) {
            this.value = Math.round(this.value / step) * step;
        }
        if (onChange != null) onChange.run();
    }

    // Toggle
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) {
        this.enabled = e;
        if (onChange != null) onChange.run();
    }
    public void toggle() { setEnabled(!enabled); }

    public Setting onChange(Runnable r) { this.onChange = r; return this; }
}
