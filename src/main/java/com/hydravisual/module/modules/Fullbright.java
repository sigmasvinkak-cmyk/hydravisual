package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import com.hydravisual.module.Setting;

/**
 * Fullbright — настраиваемая яркость через GammaOptionMixin.
 * Mixin перехватывает SimpleOption.getValue() и возвращает наше значение.
 */
public class Fullbright extends Module {
    private final Setting brightness;

    public Fullbright() {
        super("Fullbright", "Настр. яркость", Category.VISUAL);
        brightness = addSetting(new Setting("Яркость", 5.0, 1.0, 10.0, 0.5));
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}

    /** Called by GammaOptionMixin to get the current brightness */
    public double getBrightness() {
        return brightness.getValue();
    }
}
