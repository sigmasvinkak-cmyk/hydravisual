package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import com.hydravisual.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Fullbright — настраиваемая яркость (гамма)
 */
public class Fullbright extends Module {
    private double previousGamma = 1.0;
    private final Setting brightness;

    public Fullbright() {
        super("Fullbright", "Макс. яркость", Category.VISUAL);
        brightness = addSetting(new Setting("Яркость", 5.0, 1.0, 10.0, 0.5)
                .onChange(this::applyBrightness));
    }

    private void applyBrightness() {
        if (!isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            client.options.getGamma().setValue(brightness.getValue());
        }
    }

    @Override
    public void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            SimpleOption<Double> gamma = client.options.getGamma();
            previousGamma = gamma.getValue();
            gamma.setValue(brightness.getValue());
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            client.options.getGamma().setValue(previousGamma);
        }
    }
}
