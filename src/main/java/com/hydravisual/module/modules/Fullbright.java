package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import com.hydravisual.module.Setting;
import net.minecraft.client.MinecraftClient;

/**
 * Fullbright — настраиваемая яркость через прямой доступ к полю гаммы.
 * SimpleOption.setValue() ограничивает значение до [0,1], поэтому используем
 * onTick для постоянного применения через reflection.
 */
public class Fullbright extends Module {
    private double previousGamma = 1.0;
    private final Setting brightness;

    // Reflection field cache
    private static java.lang.reflect.Field gammaValueField;

    static {
        try {
            // net.minecraft.client.option.SimpleOption has a field "value"
            gammaValueField = net.minecraft.client.option.SimpleOption.class.getDeclaredField("value");
            gammaValueField.setAccessible(true);
        } catch (Exception e) {
            gammaValueField = null;
        }
    }

    public Fullbright() {
        super("Fullbright", "Настр. яркость", Category.VISUAL);
        brightness = addSetting(new Setting("Яркость", 5.0, 1.0, 10.0, 0.5));
    }

    private void applyGamma(double value) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        if (gammaValueField != null) {
            try {
                gammaValueField.set(client.options.getGamma(), value);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            previousGamma = client.options.getGamma().getValue();
        }
        applyGamma(brightness.getValue());
    }

    @Override
    public void onDisable() {
        applyGamma(previousGamma);
    }

    @Override
    public void onTick() {
        // Reapply every tick — Minecraft may reset it
        applyGamma(brightness.getValue());
    }
}
