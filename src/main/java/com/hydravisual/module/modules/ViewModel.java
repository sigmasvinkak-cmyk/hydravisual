package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import com.hydravisual.module.Setting;

/**
 * ViewModel — настройка позиции и размера рук (left/right hand).
 * X, Y, Z + Scale для каждой руки.
 */
public class ViewModel extends Module {

    private final Setting rightX, rightY, rightZ, rightScale;
    private final Setting leftX, leftY, leftZ, leftScale;

    public ViewModel() {
        super("ViewModel", "Позиция рук", Category.VISUAL);

        rightX     = addSetting(new Setting("Right X", 0.0, -2.0, 2.0, 0.05));
        rightY     = addSetting(new Setting("Right Y", 0.0, -2.0, 2.0, 0.05));
        rightZ     = addSetting(new Setting("Right Z", 0.0, -2.0, 2.0, 0.05));
        rightScale = addSetting(new Setting("Right Size", 1.0, 0.1, 3.0, 0.1));

        leftX      = addSetting(new Setting("Left X", 0.0, -2.0, 2.0, 0.05));
        leftY      = addSetting(new Setting("Left Y", 0.0, -2.0, 2.0, 0.05));
        leftZ      = addSetting(new Setting("Left Z", 0.0, -2.0, 2.0, 0.05));
        leftScale  = addSetting(new Setting("Left Size", 1.0, 0.1, 3.0, 0.1));
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}

    // Right hand getters
    public float getRX() { return (float) rightX.getValue(); }
    public float getRY() { return (float) rightY.getValue(); }
    public float getRZ() { return (float) rightZ.getValue(); }
    public float getRS() { return (float) rightScale.getValue(); }

    // Left hand getters
    public float getLX() { return (float) leftX.getValue(); }
    public float getLY() { return (float) leftY.getValue(); }
    public float getLZ() { return (float) leftZ.getValue(); }
    public float getLS() { return (float) leftScale.getValue(); }
}
