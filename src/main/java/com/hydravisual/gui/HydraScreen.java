package com.hydravisual.gui;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.Module;
import com.hydravisual.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HydraScreen extends Screen {

    private enum Tab {
        VISUALS("Visuals"),
        FRIENDS("Friends"),
        UTILITIES("Utilities"),
        COSMETICS("Cosmetics"),
        CONFIGS("Configs");
        final String label;
        Tab(String l) { label = l; }
    }

    private Tab selectedTab = Tab.VISUALS;
    private final ModuleManager moduleManager;

    private int px, py, pw, ph;
    private static final int SIDEBAR_W = 110;
    private static final int TAB_H = 24;
    private static final int CORNER_R = 8;

    private float openAnim = 0f;
    private float scrollOffset = 0f;
    private float scrollTarget = 0f;
    private float tabIndicatorY = -1f;
    private float[] tabHoverAnim = new float[Tab.values().length];

    public HydraScreen() {
        super(Text.literal("Menu"));
        this.moduleManager = HydraVisualClient.INSTANCE.getModuleManager();
    }

    @Override
    protected void init() {
        pw = Math.min(420, width - 60);
        ph = Math.min(300, height - 60);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        openAnim = 0f;
        tabIndicatorY = -1f;
        scrollOffset = 0f;
        scrollTarget = 0f;
    }

    // ========== COLOR HELPERS ==========

    private static int hsbToRgb(float hue, float sat, float bri) {
        int r, g, b;
        if (sat == 0) {
            r = g = b = (int)(bri * 255f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6f;
            float f = h - (float)Math.floor(h);
            float p = bri * (1f - sat), q = bri * (1f - sat * f), t = bri * (1f - sat * (1f - f));
            switch ((int) h) {
                case 0 -> { r=(int)(bri*255+.5f); g=(int)(t*255+.5f); b=(int)(p*255+.5f); }
                case 1 -> { r=(int)(q*255+.5f); g=(int)(bri*255+.5f); b=(int)(p*255+.5f); }
                case 2 -> { r=(int)(p*255+.5f); g=(int)(bri*255+.5f); b=(int)(t*255+.5f); }
                case 3 -> { r=(int)(p*255+.5f); g=(int)(q*255+.5f); b=(int)(bri*255+.5f); }
                case 4 -> { r=(int)(t*255+.5f); g=(int)(p*255+.5f); b=(int)(bri*255+.5f); }
                default -> { r=(int)(bri*255+.5f); g=(int)(p*255+.5f); b=(int)(q*255+.5f); }
            }
        }
        return (r << 16) | (g << 8) | b;
    }

    private int accent(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 5000) / 5000f;
        return 0xFF000000 | hsbToRgb(hue, 0.5f, 0.9f);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int a1=(a>>24)&0xFF, r1=(a>>16)&0xFF, g1=(a>>8)&0xFF, b1=a&0xFF;
        int a2=(b>>24)&0xFF, r2=(b>>16)&0xFF, g2=(b>>8)&0xFF, b2=b&0xFF;
        return ((int)(a1+(a2-a1)*t)<<24)|((int)(r1+(r2-r1)*t)<<16)|((int)(g1+(g2-g1)*t)<<8)|(int)(b1+(b2-b1)*t);
    }

    // ========== ROUNDED RECT ==========
    // Draws a filled rectangle with rounded corners by masking corner pixels
    private void fillRounded(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (((color >> 24) & 0xFF) == 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        // Center body
        ctx.fill(x + r, y, x + w - r, y + h, color);
        // Left strip
        ctx.fill(x, y + r, x + r, y + h - r, color);
        // Right strip
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);
        // Corner pixels (quarter-circle approximation)
        for (int cy2 = 0; cy2 < r; cy2++) {
            for (int cx2 = 0; cx2 < r; cx2++) {
                float dist = (float)Math.sqrt((r - cx2 - 0.5f) * (r - cx2 - 0.5f) + (r - cy2 - 0.5f) * (r - cy2 - 0.5f));
                if (dist <= r) {
                    // Top-left
                    ctx.fill(x + cx2, y + cy2, x + cx2 + 1, y + cy2 + 1, color);
                    // Top-right
                    ctx.fill(x + w - cx2 - 1, y + cy2, x + w - cx2, y + cy2 + 1, color);
                    // Bottom-left
                    ctx.fill(x + cx2, y + h - cy2 - 1, x + cx2 + 1, y + h - cy2, color);
                    // Bottom-right
                    ctx.fill(x + w - cx2 - 1, y + h - cy2 - 1, x + w - cx2, y + h - cy2, color);
                }
            }
        }
    }

    // Rounded rect with only specific corners rounded
    private void fillRoundedTop(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (((color >> 24) & 0xFF) == 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h, color);
        ctx.fill(x + w - r, y + r, x + w, y + h, color);
        for (int cy2 = 0; cy2 < r; cy2++) {
            for (int cx2 = 0; cx2 < r; cx2++) {
                float dist = (float)Math.sqrt((r - cx2 - 0.5f) * (r - cx2 - 0.5f) + (r - cy2 - 0.5f) * (r - cy2 - 0.5f));
                if (dist <= r) {
                    ctx.fill(x + cx2, y + cy2, x + cx2 + 1, y + cy2 + 1, color);
                    ctx.fill(x + w - cx2 - 1, y + cy2, x + w - cx2, y + cy2 + 1, color);
                }
            }
        }
    }

    // ========== MAIN RENDER ==========

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        openAnim = Math.min(1f, openAnim + delta * 0.07f);
        float ease = 1f - (1f - openAnim) * (1f - openAnim) * (1f - openAnim);
        if (ease < 0.01f) return;
        int alpha = (int)(ease * 255);

        scrollOffset += (scrollTarget - scrollOffset) * Math.min(1f, delta * 8f);

        // Dim overlay
        ctx.fill(0, 0, width, height, withAlpha(0xFF000000, (int)(ease * 100)));

        // Soft shadow behind panel
        for (int i = 5; i >= 1; i--) {
            fillRounded(ctx, px - i, py - i, pw + i * 2, ph + i * 2, CORNER_R + i,
                    withAlpha(0xFF000000, (int)(ease * (8 - i))));
        }

        // Main panel — dark, rounded
        fillRounded(ctx, px, py, pw, ph, CORNER_R, withAlpha(0xFF141418, alpha));

        // Sidebar background — slightly different shade, rounded left side
        drawSidebarBg(ctx, px, py, SIDEBAR_W, ph, CORNER_R, withAlpha(0xFF1a1a1f, alpha));

        // Thin separator line between sidebar and content
        ctx.fill(px + SIDEBAR_W, py + 10, px + SIDEBAR_W + 1, py + ph - 10,
                withAlpha(0xFF2a2a32, alpha));

        drawSidebar(ctx, mouseX, mouseY, alpha, delta);
        drawContent(ctx, mouseX, mouseY, alpha, delta);
    }

    // Sidebar bg with only left corners rounded
    private void drawSidebarBg(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (((color >> 24) & 0xFF) == 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        // Main body (no right rounding)
        ctx.fill(x + r, y, x + w, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        // Only left corners
        for (int cy2 = 0; cy2 < r; cy2++) {
            for (int cx2 = 0; cx2 < r; cx2++) {
                float dist = (float)Math.sqrt((r - cx2 - 0.5f) * (r - cx2 - 0.5f) + (r - cy2 - 0.5f) * (r - cy2 - 0.5f));
                if (dist <= r) {
                    ctx.fill(x + cx2, y + cy2, x + cx2 + 1, y + cy2 + 1, color);
                    ctx.fill(x + cx2, y + h - cy2 - 1, x + cx2 + 1, y + h - cy2, color);
                }
            }
        }
    }

    // ========== SIDEBAR ==========

    private void drawSidebar(DrawContext ctx, int mx, int my, int alpha, float delta) {
        // Logo / brand area
        int logoY = py + 14;
        ctx.drawText(textRenderer, "SeladalaVisual", px + 14, logoY, withAlpha(0xFFe0e0e8, alpha), false);
        // Subtle subtitle
        ctx.drawText(textRenderer, "v1.2.0", px + 14, logoY + 12, withAlpha(0xFF505058, alpha), false);

        // Separator
        int sepY = logoY + 28;
        ctx.fill(px + 12, sepY, px + SIDEBAR_W - 12, sepY + 1, withAlpha(0xFF2a2a32, alpha));

        // Category label
        ctx.drawText(textRenderer, "Modules", px + 14, sepY + 8, withAlpha(0xFF505058, alpha), false);

        // Tabs
        int tabStartY = sepY + 22;
        float targetIndY = tabStartY;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int tabY = tabStartY + i * TAB_H;
            boolean sel = tab == selectedTab;
            boolean hov = mx >= px + 4 && mx < px + SIDEBAR_W - 4 && my >= tabY && my < tabY + TAB_H;

            if (sel) targetIndY = tabY;

            // Hover animation
            float ht = (sel || hov) ? 1f : 0f;
            tabHoverAnim[i] += (ht - tabHoverAnim[i]) * Math.min(1f, delta * 10f);

            // Background on hover/select
            if (tabHoverAnim[i] > 0.01f) {
                int bgA = (int)(alpha * 0.06f * tabHoverAnim[i]);
                fillRounded(ctx, px + 6, tabY + 1, SIDEBAR_W - 12, TAB_H - 2, 4,
                        withAlpha(0xFFFFFFFF, bgA));
            }

            // Label color
            int labelC;
            if (sel) {
                labelC = withAlpha(0xFFf0f0f4, alpha);
            } else {
                labelC = lerp(withAlpha(0xFF707078, alpha), withAlpha(0xFFb0b0b8, alpha), tabHoverAnim[i]);
            }

            ctx.drawText(textRenderer, tab.label, px + 18, tabY + (TAB_H - 8) / 2, labelC, false);
        }

        // Animated indicator dot/bar — small circle on the left
        if (tabIndicatorY < 0) tabIndicatorY = targetIndY;
        else tabIndicatorY += (targetIndY - tabIndicatorY) * Math.min(1f, delta * 7f);

        int iy = (int) tabIndicatorY + TAB_H / 2 - 2;
        int ic = accent(0);
        // Small dot indicator
        ctx.fill(px + 8, iy, px + 11, iy + 5, withAlpha(ic, alpha));
        // Subtle glow
        ctx.fill(px + 7, iy - 1, px + 12, iy + 6, withAlpha(ic, alpha / 10));

        // Bottom hint
        ctx.drawText(textRenderer, "RShift", px + 14, py + ph - 18, withAlpha(0xFF383840, alpha), false);
    }

    // ========== CONTENT ==========

    private void drawContent(DrawContext ctx, int mx, int my, int alpha, float delta) {
        int cx = px + SIDEBAR_W + 12;
        int cy = py + 14;
        int cw = pw - SIDEBAR_W - 24;
        int contentH = ph - 28;

        // Tab title — clean, white
        ctx.drawText(textRenderer, selectedTab.label, cx, cy, withAlpha(0xFFe8e8f0, alpha), false);

        int contentTop = cy + 18;
        int contentArea = contentH - 18;

        switch (selectedTab) {
            case VISUALS -> drawModuleGrid(ctx, cx, contentTop, cw, contentArea, mx, my, alpha, delta);
            case FRIENDS -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Friends", "Coming soon");
            case UTILITIES -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Utilities", "Coming soon");
            case COSMETICS -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Cosmetics", "Coming soon");
            case CONFIGS -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Configs", "Coming soon");
        }
    }

    // ========== MODULE GRID (2 columns like Celestial) ==========

    private void drawModuleGrid(DrawContext ctx, int x, int y, int w, int maxH, int mx, int my, int alpha, float delta) {
        List<Module> modules = moduleManager.getModules();
        int gap = 4;
        int colW = (w - gap) / 2;
        int cardH = 40;

        int rows = (modules.size() + 1) / 2;
        int totalH = rows * (cardH + gap) - gap;
        int maxScroll = Math.max(0, totalH - maxH);
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));

        int clipTop = y;
        int clipBottom = y + maxH;

        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            int col = i % 2;
            int row = i / 2;

            int cardX = x + col * (colW + gap);
            int cardY = y + row * (cardH + gap) - (int) scrollOffset;

            if (cardY + cardH < clipTop || cardY > clipBottom) continue;

            boolean hov = mx >= cardX && mx <= cardX + colW && my >= Math.max(clipTop, cardY) && my < Math.min(clipBottom, cardY + cardH);
            boolean on = mod.isEnabled();

            // Card background — subtle rounded
            int bg;
            if (on) {
                bg = hov ? withAlpha(0xFF242430, alpha) : withAlpha(0xFF1e1e28, alpha);
            } else {
                bg = hov ? withAlpha(0xFF1e1e24, alpha) : withAlpha(0xFF18181e, alpha);
            }
            fillRounded(ctx, cardX, cardY, colW, cardH, 5, bg);

            // Module name — bold if on
            int nameC = on ? withAlpha(0xFFe8e8f0, alpha) : withAlpha(0xFF808088, alpha);
            ctx.drawText(textRenderer, mod.getName(), cardX + 8, cardY + 8, nameC, on);

            // Description — smaller, grey
            String desc = mod.getDescription();
            int maxDescW = colW - 16;
            if (textRenderer.getWidth(desc) > maxDescW) {
                while (textRenderer.getWidth(desc + "..") > maxDescW && desc.length() > 3) {
                    desc = desc.substring(0, desc.length() - 1);
                }
                desc += "..";
            }
            ctx.drawText(textRenderer, desc, cardX + 8, cardY + 22, withAlpha(0xFF505058, alpha), false);

            // Enabled indicator — small colored dot top-right
            if (on) {
                int dotC = accent(i * 200);
                int dotX = cardX + colW - 10;
                int dotY2 = cardY + 8;
                // 4px dot
                ctx.fill(dotX, dotY2, dotX + 4, dotY2 + 4, withAlpha(dotC, alpha));
                // Glow
                ctx.fill(dotX - 1, dotY2 - 1, dotX + 5, dotY2 + 5, withAlpha(dotC, alpha / 6));
            }
        }

        // Scrollbar (minimal)
        if (totalH > maxH && maxScroll > 0) {
            int barX = x + w - 2;
            float ratio = (float) maxH / totalH;
            int thumbH = Math.max(12, (int)(maxH * ratio));
            int thumbY = y + (int)((scrollOffset / maxScroll) * (maxH - thumbH));

            ctx.fill(barX, y, barX + 2, y + maxH, withAlpha(0xFF1a1a20, alpha / 3));
            fillRounded(ctx, barX, thumbY, 2, thumbH, 1, withAlpha(0xFF404048, alpha));
        }
    }

    // ========== PLACEHOLDER ==========

    private void drawPlaceholder(DrawContext ctx, int x, int y, int w, int h, int alpha, String title, String sub) {
        int cx = x + w / 2;
        int cy = y + h / 2 - 10;
        int tw = textRenderer.getWidth(title);
        ctx.drawText(textRenderer, title, cx - tw / 2, cy, withAlpha(0xFF606068, alpha), false);
        int sw = textRenderer.getWidth(sub);
        ctx.drawText(textRenderer, sub, cx - sw / 2, cy + 14, withAlpha(0xFF383840, alpha), false);
    }

    // ========== INPUT ==========

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Tab clicks
        int sepY = py + 14 + 28;
        int tabStartY = sepY + 22;
        for (int i = 0; i < Tab.values().length; i++) {
            int tabY = tabStartY + i * TAB_H;
            if (mouseX >= px + 4 && mouseX < px + SIDEBAR_W - 4 && mouseY >= tabY && mouseY < tabY + TAB_H) {
                selectedTab = Tab.values()[i];
                scrollTarget = 0;
                scrollOffset = 0;
                return true;
            }
        }

        // Module clicks (grid)
        if (selectedTab == Tab.VISUALS) {
            int cx = px + SIDEBAR_W + 12;
            int cy = py + 14 + 18;
            int cw = pw - SIDEBAR_W - 24;
            int contentArea = ph - 28 - 18;
            int clipBottom = cy + contentArea;
            int gap = 4;
            int colW = (cw - gap) / 2;
            int cardH = 40;

            List<Module> modules = moduleManager.getModules();
            for (int i = 0; i < modules.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                int cardX = cx + col * (colW + gap);
                int cardY = cy + row * (cardH + gap) - (int) scrollOffset;
                if (cardY + cardH < cy || cardY > clipBottom) continue;
                if (mouseX >= cardX && mouseX <= cardX + colW && mouseY >= Math.max(cy, cardY) && mouseY < Math.min(clipBottom, cardY + cardH)) {
                    modules.get(i).toggle();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= (float)(verticalAmount * 30);
        scrollTarget = Math.max(0, scrollTarget);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
