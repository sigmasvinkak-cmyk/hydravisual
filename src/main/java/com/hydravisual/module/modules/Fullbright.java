package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Fullbright — максимальная яркость (гамма)
 */
public class Fullbright extends Module {
    private double previousGamma = 1.0;

    public Fullbright() {
        super("Fullbright", "Полная яркость экрана", Category.VISUAL);
    }

    @Override
    public void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            SimpleOption<Double> gamma = client.options.getGamma();
            previousGamma = gamma.getValue();
            gamma.setValue(15.0);
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
