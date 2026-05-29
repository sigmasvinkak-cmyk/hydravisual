package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import com.hydravisual.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.BlockPos;

/**
 * HUD — единый модуль для всех HUD элементов.
 * FPS, Coords и т.д. — всё внутри как настройки.
 * Элементы можно перетаскивать когда открыт чат (T).
 */
public class HudOverlayModule extends Module {
    private final Setting showFps;
    private final Setting showCoords;

    // Draggable positions (screen-relative)
    public int fpsX = 4, fpsY = 4;
    public int coordsX = 4, coordsY = 18;

    // Drag state
    private int dragElement = -1; // -1=none, 0=fps, 1=coords
    private int dragOffX, dragOffY;

    public HudOverlayModule() {
        super("HUD", "FPS, координаты", Category.HUD);
        showFps = addSetting(new Setting("FPS", true));
        showCoords = addSetting(new Setting("Координаты", true));
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    public boolean isShowFps() { return showFps.isEnabled(); }
    public boolean isShowCoords() { return showCoords.isEnabled(); }

    /**
     * Render HUD elements (called from HudRenderCallback)
     */
    public void renderHud(DrawContext ctx) {
        if (!isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        boolean inChat = client.currentScreen instanceof ChatScreen;

        if (showFps.isEnabled()) {
            String fps = "FPS: " + client.getCurrentFps();
            int w = client.textRenderer.getWidth(fps) + 6;
            int h = 12;
            if (inChat) {
                // Draw background for drag hint
                ctx.fill(fpsX - 2, fpsY - 2, fpsX + w, fpsY + h, 0x40FFFFFF);
            }
            ctx.drawText(client.textRenderer, fps, fpsX, fpsY, 0xFF00FF88, true);
        }

        if (showCoords.isEnabled()) {
            BlockPos pos = client.player.getBlockPos();
            String coords = String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ());
            int w = client.textRenderer.getWidth(coords) + 6;
            int h = 12;
            if (inChat) {
                ctx.fill(coordsX - 2, coordsY - 2, coordsX + w, coordsY + h, 0x40FFFFFF);
            }
            ctx.drawText(client.textRenderer, coords, coordsX, coordsY, 0xFFAABBFF, true);
        }
    }

    /**
     * Handle mouse press in chat screen for dragging
     */
    public boolean onMouseDown(double mx, double my) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !isEnabled()) return false;

        if (showFps.isEnabled()) {
            String fps = "FPS: " + client.getCurrentFps();
            int w = client.textRenderer.getWidth(fps) + 6;
            if (mx >= fpsX - 2 && mx <= fpsX + w && my >= fpsY - 2 && my <= fpsY + 12) {
                dragElement = 0;
                dragOffX = (int) mx - fpsX;
                dragOffY = (int) my - fpsY;
                return true;
            }
        }

        if (showCoords.isEnabled()) {
            BlockPos pos = client.player != null ? client.player.getBlockPos() : BlockPos.ORIGIN;
            String coords = String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ());
            int w = client.textRenderer.getWidth(coords) + 6;
            if (mx >= coordsX - 2 && mx <= coordsX + w && my >= coordsY - 2 && my <= coordsY + 12) {
                dragElement = 1;
                dragOffX = (int) mx - coordsX;
                dragOffY = (int) my - coordsY;
                return true;
            }
        }

        return false;
    }

    public boolean onMouseDrag(double mx, double my) {
        if (dragElement == 0) {
            fpsX = (int) mx - dragOffX;
            fpsY = (int) my - dragOffY;
            return true;
        } else if (dragElement == 1) {
            coordsX = (int) mx - dragOffX;
            coordsY = (int) my - dragOffY;
            return true;
        }
        return false;
    }

    public void onMouseUp() {
        dragElement = -1;
    }

    /**
     * Called each tick — polls mouse for drag when in chat
     */
    public void tickDrag() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        if (!(client.currentScreen instanceof ChatScreen)) { dragElement = -1; return; }

        long handle = client.getWindow().getHandle();
        boolean pressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (pressed && dragElement >= 0) {
            double[] xBuf = new double[1], yBuf = new double[1];
            org.lwjgl.glfw.GLFW.glfwGetCursorPos(handle, xBuf, yBuf);
            double scale = client.getWindow().getScaleFactor();
            onMouseDrag(xBuf[0] / scale, yBuf[0] / scale);
        } else if (!pressed) {
            dragElement = -1;
        }
    }
}
