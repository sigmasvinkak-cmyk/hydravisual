package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Coords Display — показывает координаты на экране
 */
public class CoordsDisplay extends Module {
    public CoordsDisplay() {
        super("Coords", "Показывает координаты XYZ", Category.HUD);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    public String getCoordsText() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "";

        BlockPos pos = client.player.getBlockPos();
        return String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ());
    }
}
