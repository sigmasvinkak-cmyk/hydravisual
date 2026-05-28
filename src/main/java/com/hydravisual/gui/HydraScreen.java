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
        VISUALS("Visuals", "\u2726"),
        FRIENDS("Friends", "\u2661"),
        UTILITIES("Utilities", "\u2692"),
        COSMETICS("Cosmetics", "\u2605"),
        CONFIGS("Configs", "\u2699");
        final String label, icon;
        Tab(String l, String i) { label = l; icon = i; }
    }

    private Tab selectedTab = Tab.VISUALS;
    private final ModuleManager moduleManager;

    // Panel geometry
    private int px, py, pw, ph;
    private static final int SIDEBAR_W = 130;
    private static final int TAB_H = 32;
    private static final int CARD_H = 44;
    private static final int CARD_GAP = 4;
    private static final int RAINBOW_BAR_H = 2;

    // Animation state
    private float openAnim = 0f;
    private float scrollOffset = 0f;
    private float scrollTarget = 0f;
    private float tabIndicatorY = -1f;
    private float[] tabHoverAnim = new float[Tab.values().length];
    private float[] cardHoverAnim;
    private float headerPulse = 0f;

    public HydraScreen() {
        super(Text.literal("Menu"));
        this.moduleManager = HydraVisualClient.INSTANCE.getModuleManager();
    }

    @Override
    protected void init() {
        pw = Math.min(480, width - 40);
        ph = Math.min(340, height - 40);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        openAnim = 0f;
        tabIndicatorY = -1f;
        scrollOffset = 0f;
        scrollTarget = 0f;
        int modCount = moduleManager.getModules().size();
        cardHoverAnim = new float[Math.max(modCount, 1)];
    }

    // ==================== COLOR UTILS ====================

    private int rainbow(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 4000) / 4000f;
        return 0xFF000000 | hsbToRgb(hue, 0.6f, 1.0f);
    }

    private int rainbowSoft(int offset) {
        float hue = ((System.currentTimeMillis() + offset) % 4000) / 4000f;
        return 0xFF000000 | hsbToRgb(hue, 0.35f, 0.85f);
    }

    private static int hsbToRgb(float hue, float sat, float bri) {
        int r = 0, g = 0, b = 0;
        if (sat == 0) {
            r = g = b = (int)(bri * 255f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6f;
            float f = h - (float)Math.floor(h);
            float p = bri * (1f - sat);
            float q = bri * (1f - sat * f);
            float t = bri * (1f - (sat * (1f - f)));
            switch ((int) h) {
                case 0 -> { r = (int)(bri*255+.5f); g = (int)(t*255+.5f); b = (int)(p*255+.5f); }
                case 1 -> { r = (int)(q*255+.5f); g = (int)(bri*255+.5f); b = (int)(p*255+.5f); }
                case 2 -> { r = (int)(p*255+.5f); g = (int)(bri*255+.5f); b = (int)(t*255+.5f); }
                case 3 -> { r = (int)(p*255+.5f); g = (int)(q*255+.5f); b = (int)(bri*255+.5f); }
                case 4 -> { r = (int)(t*255+.5f); g = (int)(p*255+.5f); b = (int)(bri*255+.5f); }
                case 5 -> { r = (int)(bri*255+.5f); g = (int)(p*255+.5f); b = (int)(q*255+.5f); }
            }
        }
        return (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((int)(a1 + (a2 - a1) * t) << 24) |
               ((int)(r1 + (r2 - r1) * t) << 16) |
               ((int)(g1 + (g2 - g1) * t) << 8) |
               (int)(b1 + (b2 - b1) * t);
    }

    // ==================== DRAWING HELPERS ====================

    private void drawRainbowBar(DrawContext ctx, int x, int y, int w, int h, int alpha, int speed) {
        for (int i = 0; i < w; i++) {
            int c = rainbow(i * speed);
            ctx.fill(x + i, y, x + i + 1, y + h, withAlpha(c, alpha));
        }
    }

    private void drawGlowRect(DrawContext ctx, int x, int y, int w, int h, int color, int layers) {
        int a = (color >> 24) & 0xFF;
        for (int i = layers; i >= 1; i--) {
            int ga = a / (i * 2 + 1);
            ctx.fill(x - i, y - i, x + w + i, y + h + i, withAlpha(color, ga));
        }
    }

    // ==================== MAIN RENDER ====================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Opening animation (ease out cubic)
        openAnim = Math.min(1f, openAnim + delta * 0.06f);
        float ease = 1f - (1f - openAnim) * (1f - openAnim) * (1f - openAnim);
        if (ease < 0.01f) return;
        int alpha = (int)(ease * 255);

        headerPulse += delta * 0.02f;

        // Smooth scroll interpolation
        scrollOffset += (scrollTarget - scrollOffset) * Math.min(1f, delta * 8f);

        // Dark overlay behind panel
        ctx.fill(0, 0, width, height, withAlpha(0xFF000000, (int)(ease * 120)));

        // Panel outer glow (rainbow tinted)
        int glowC = rainbow(0);
        drawGlowRect(ctx, px, py, pw, ph, withAlpha(glowC, 15), 6);

        // Main panel background
        ctx.fill(px, py, px + pw, py + ph, withAlpha(0xFF0c0c1a, alpha));

        // Top rainbow gradient bar
        drawRainbowBar(ctx, px, py, pw, RAINBOW_BAR_H, alpha, 30);

        // Sidebar
        ctx.fill(px, py + RAINBOW_BAR_H, px + SIDEBAR_W, py + ph, withAlpha(0xFF090918, alpha));

        // Sidebar right border (subtle rainbow)
        for (int i = 0; i < ph - RAINBOW_BAR_H; i++) {
            int c = rainbow(i * 15);
            ctx.fill(px + SIDEBAR_W, py + RAINBOW_BAR_H + i, px + SIDEBAR_W + 1, py + RAINBOW_BAR_H + i + 1, withAlpha(c, alpha / 20));
        }

        drawSidebar(ctx, mouseX, mouseY, alpha, delta);
        drawContent(ctx, mouseX, mouseY, alpha, delta);

        // Bottom rainbow bar (thinner, dimmer)
        drawRainbowBar(ctx, px, py + ph - 1, pw, 1, alpha / 3, 30);
    }

    // ==================== SIDEBAR ====================

    private void drawSidebar(DrawContext ctx, int mx, int my, int alpha, float delta) {
        // Logo area
        int logoX = px + 12;
        int logoY = py + RAINBOW_BAR_H + 10;

        // "CP" badge with rainbow border
        int badgeSize = 24;
        int rc = rainbow(0);
        ctx.fill(logoX - 1, logoY - 1, logoX + badgeSize + 1, logoY + badgeSize + 1, withAlpha(rc, (int)(alpha * 0.5f)));
        ctx.fill(logoX, logoY, logoX + badgeSize, logoY + badgeSize, withAlpha(0xFF111130, alpha));
        ctx.drawText(textRenderer, "CP", logoX + 5, logoY + 8, withAlpha(rc, alpha), true);

        // Client name
        ctx.drawText(textRenderer, "Client", logoX + badgeSize + 6, logoY + 3, withAlpha(0xFFe0e0ff, alpha), false);
        ctx.drawText(textRenderer, "Pasta", logoX + badgeSize + 6, logoY + 14, withAlpha(0xFF505078, alpha), false);

        // Rainbow separator
        int sepY = logoY + badgeSize + 10;
        drawRainbowBar(ctx, px + 10, sepY, SIDEBAR_W - 20, 1, alpha / 6, 25);

        // Tabs
        int tabStartY = sepY + 8;
        float targetIndY = tabStartY;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int tabY = tabStartY + i * TAB_H;
            boolean sel = tab == selectedTab;
            boolean hov = mx >= px + 4 && mx < px + SIDEBAR_W - 4 && my >= tabY && my < tabY + TAB_H;

            if (sel) targetIndY = tabY;

            // Hover animation
            float hTarget = (sel || hov) ? 1f : 0f;
            tabHoverAnim[i] += (hTarget - tabHoverAnim[i]) * Math.min(1f, delta * 8f);

            // Tab background
            if (tabHoverAnim[i] > 0.01f) {
                int bgAlpha;
                if (sel) {
                    int rc2 = rainbow(i * 180);
                    bgAlpha = (int)(alpha * 0.08f * tabHoverAnim[i]);
                    ctx.fill(px + 6, tabY + 2, px + SIDEBAR_W - 6, tabY + TAB_H - 2, withAlpha(rc2, bgAlpha));
                } else {
                    bgAlpha = (int)(alpha * 0.04f * tabHoverAnim[i]);
                    ctx.fill(px + 6, tabY + 2, px + SIDEBAR_W - 6, tabY + TAB_H - 2, withAlpha(0xFFFFFFFF, bgAlpha));
                }
            }

            // Icon
            int iconX = px + 16;
            int iconCenterY = tabY + TAB_H / 2 - 4;
            int iconC = sel ? rainbow(i * 180) : withAlpha(0xFF606088, alpha);
            ctx.drawText(textRenderer, tab.icon, iconX, iconCenterY, iconC, sel);

            // Label
            int labelC = sel ? withAlpha(0xFFf0f0ff, alpha) : lerpColor(
                withAlpha(0xFF707090, alpha),
                withAlpha(0xFFc0c0e0, alpha),
                tabHoverAnim[i]
            );
            ctx.drawText(textRenderer, tab.label, iconX + 14, iconCenterY, labelC, false);
        }

        // Animated rainbow side indicator
        if (tabIndicatorY < 0) tabIndicatorY = targetIndY;
        else tabIndicatorY += (targetIndY - tabIndicatorY) * Math.min(1f, delta * 6f);

        int iy = (int) tabIndicatorY;
        int ic = rainbow(0);
        // Main indicator line
        ctx.fill(px, iy + 6, px + 2, iy + TAB_H - 6, withAlpha(ic, alpha));
        // Glow layers
        ctx.fill(px + 2, iy + 8, px + 4, iy + TAB_H - 8, withAlpha(ic, alpha / 6));
        ctx.fill(px + 4, iy + 10, px + 6, iy + TAB_H - 10, withAlpha(ic, alpha / 12));

        // Bottom keybind hint
        String hint = "[RShift] toggle";
        int hw = textRenderer.getWidth(hint);
        ctx.drawText(textRenderer, hint, px + (SIDEBAR_W - hw) / 2, py + ph - 16, withAlpha(0xFF303050, alpha / 2), false);
    }

    // ==================== CONTENT AREA ====================

    private void drawContent(DrawContext ctx, int mx, int my, int alpha, float delta) {
        int cx = px + SIDEBAR_W + 12;
        int cy = py + RAINBOW_BAR_H + 10;
        int cw = pw - SIDEBAR_W - 24;
        int contentH = ph - RAINBOW_BAR_H - 20;

        // Tab title
        int titleC = rainbow(0);
        ctx.drawText(textRenderer, selectedTab.label, cx, cy, withAlpha(titleC, alpha), true);

        // Rainbow underline
        int titleW = textRenderer.getWidth(selectedTab.label);
        drawRainbowBar(ctx, cx, cy + 11, titleW + 10, 1, alpha / 3, 20);

        int contentTop = cy + 18;
        int contentArea = contentH - 28;

        switch (selectedTab) {
            case VISUALS -> drawModuleCards(ctx, cx, contentTop, cw, contentArea, mx, my, alpha, delta);
            case FRIENDS -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Friends", "Coming soon...");
            case UTILITIES -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Utilities", "Coming soon...");
            case COSMETICS -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Cosmetics", "Coming soon...");
            case CONFIGS -> drawPlaceholder(ctx, cx, contentTop, cw, contentArea, alpha, "Configs", "Coming soon...");
        }
    }

    // ==================== MODULE CARDS ====================

    private void drawModuleCards(DrawContext ctx, int x, int y, int w, int maxH, int mx, int my, int alpha, float delta) {
        List<Module> modules = moduleManager.getModules();
        int totalH = modules.size() * (CARD_H + CARD_GAP) - CARD_GAP;
        int maxScroll = Math.max(0, totalH - maxH);
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));

        int clipTop = y;
        int clipBottom = y + maxH;

        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            int cardY = y + i * (CARD_H + CARD_GAP) - (int) scrollOffset;

            if (cardY + CARD_H < clipTop || cardY > clipBottom) continue;

            boolean hov = mx >= x && mx <= x + w && my >= Math.max(clipTop, cardY) && my < Math.min(clipBottom, cardY + CARD_H);
            boolean on = mod.isEnabled();

            // Card hover animation
            if (i < cardHoverAnim.length) {
                float ht = hov ? 1f : 0f;
                cardHoverAnim[i] += (ht - cardHoverAnim[i]) * Math.min(1f, delta * 10f);
            }
            float hoverF = (i < cardHoverAnim.length) ? cardHoverAnim[i] : 0f;

            // Card background with hover lift effect
            int bgBase = on ? 0xFF141438 : 0xFF101028;
            int bgHover = on ? 0xFF1a1a48 : 0xFF161638;
            int bg = lerpColor(withAlpha(bgBase, alpha), withAlpha(bgHover, alpha), hoverF);
            ctx.fill(x, cardY, x + w, cardY + CARD_H, bg);

            // Left rainbow accent (enabled modules)
            if (on) {
                int rc = rainbow(i * 120);
                ctx.fill(x, cardY + 3, x + 2, cardY + CARD_H - 3, withAlpha(rc, alpha));
                // Soft glow
                ctx.fill(x + 2, cardY + 5, x + 4, cardY + CARD_H - 5, withAlpha(rc, alpha / 8));
            }

            // Module name
            int nameC = on ? withAlpha(0xFFf0f0ff, alpha) : withAlpha(0xFF9090b0, alpha);
            ctx.drawText(textRenderer, mod.getName(), x + 10, cardY + 8, nameC, on);

            // Description
            String desc = mod.getDescription();
            if (desc.length() > 30) desc = desc.substring(0, 28) + "..";
            ctx.drawText(textRenderer, desc, x + 10, cardY + 22, withAlpha(0xFF505068, alpha), false);

            // Category badge
            String cat = mod.getCategory().getDisplayName();
            int catW = textRenderer.getWidth(cat) + 8;
            int catX = x + w - catW - 40;
            ctx.fill(catX, cardY + 8, catX + catW, catY(cardY, 8, 13), withAlpha(0xFF1a1a40, alpha));
            ctx.drawText(textRenderer, cat, catX + 4, cardY + 10, withAlpha(0xFF606080, alpha), false);

            // Toggle switch
            drawToggle(ctx, x + w - 34, cardY + (CARD_H - 14) / 2, on, alpha, i);

            // Bottom separator line
            if (i < modules.size() - 1) {
                ctx.fill(x + 8, cardY + CARD_H + 1, x + w - 8, cardY + CARD_H + 2, withAlpha(0xFF1a1a30, alpha / 3));
            }
        }

        // Scrollbar
        if (totalH > maxH && maxScroll > 0) {
            int barX = x + w - 2;
            float ratio = (float) maxH / totalH;
            int thumbH = Math.max(16, (int)(maxH * ratio));
            int thumbY = y + (int)((scrollOffset / maxScroll) * (maxH - thumbH));

            // Track
            ctx.fill(barX, y, barX + 2, y + maxH, withAlpha(0xFF151530, alpha / 2));

            // Thumb (rainbow)
            for (int i = 0; i < thumbH; i++) {
                int c = rainbow(i * 12);
                ctx.fill(barX, thumbY + i, barX + 2, thumbY + i + 1, withAlpha(c, (int)(alpha * 0.6f)));
            }
        }
    }

    private int catY(int cardY, int offsetTop, int height) {
        return cardY + offsetTop + height;
    }

    // ==================== TOGGLE SWITCH ====================

    private void drawToggle(DrawContext ctx, int x, int y, boolean on, int alpha, int index) {
        int w = 28, h = 14;

        if (on) {
            // Rainbow-filled track
            for (int i = 0; i < w; i++) {
                int c = rainbow(index * 120 + i * 15);
                ctx.fill(x + i, y, x + i + 1, y + h, withAlpha(c, alpha));
            }
        } else {
            // Dark off-state track
            ctx.fill(x, y, x + w, y + h, withAlpha(0xFF1a1a35, alpha));
            ctx.fill(x, y, x + w, y + 1, withAlpha(0xFF222248, alpha));
        }

        // Knob
        int knobS = h - 4;
        int knobX = on ? x + w - knobS - 2 : x + 2;
        int knobC = on ? withAlpha(0xFFFFFFFF, alpha) : withAlpha(0xFF404060, alpha);
        ctx.fill(knobX, y + 2, knobX + knobS, y + 2 + knobS, knobC);
    }

    // ==================== PLACEHOLDER TABS ====================

    private void drawPlaceholder(DrawContext ctx, int x, int y, int w, int h, int alpha, String title, String sub) {
        int cx = x + w / 2;
        int cy = y + h / 2 - 16;

        // Animated rainbow dots
        for (int i = 0; i < 5; i++) {
            float bounce = (float) Math.sin((System.currentTimeMillis() + i * 200) / 500.0) * 4;
            int dotX = cx - 24 + i * 12;
            int dotY = cy - 12 + (int) bounce;
            int dc = rainbow(i * 250);
            ctx.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, withAlpha(dc, alpha / 2));
        }

        int tw = textRenderer.getWidth(title);
        ctx.drawText(textRenderer, title, cx - tw / 2, cy + 8, withAlpha(0xFF707088, alpha), false);

        int sw = textRenderer.getWidth(sub);
        ctx.drawText(textRenderer, sub, cx - sw / 2, cy + 22, withAlpha(0xFF404058, alpha / 2), false);

        // Rainbow line
        drawRainbowBar(ctx, cx - 30, cy + 36, 60, 1, alpha / 5, 30);
    }

    // ==================== INPUT ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Tab clicks
        int logoY = py + RAINBOW_BAR_H + 10;
        int sepY = logoY + 24 + 10;
        int tabStartY = sepY + 8;

        for (int i = 0; i < Tab.values().length; i++) {
            int tabY = tabStartY + i * TAB_H;
            if (mouseX >= px + 4 && mouseX < px + SIDEBAR_W - 4 && mouseY >= tabY && mouseY < tabY + TAB_H) {
                selectedTab = Tab.values()[i];
                scrollTarget = 0;
                scrollOffset = 0;
                return true;
            }
        }

        // Module card clicks
        if (selectedTab == Tab.VISUALS) {
            int cx = px + SIDEBAR_W + 12;
            int cy = py + RAINBOW_BAR_H + 10 + 18;
            int cw = pw - SIDEBAR_W - 24;
            int contentArea = ph - RAINBOW_BAR_H - 20 - 28;
            int clipBottom = cy + contentArea;

            List<Module> modules = moduleManager.getModules();
            for (int i = 0; i < modules.size(); i++) {
                int cardY = cy + i * (CARD_H + CARD_GAP) - (int) scrollOffset;
                if (cardY + CARD_H < cy || cardY > clipBottom) continue;
                if (mouseX >= cx && mouseX <= cx + cw && mouseY >= Math.max(cy, cardY) && mouseY < Math.min(clipBottom, cardY + CARD_H)) {
                    modules.get(i).toggle();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= (float)(verticalAmount * 28);
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
