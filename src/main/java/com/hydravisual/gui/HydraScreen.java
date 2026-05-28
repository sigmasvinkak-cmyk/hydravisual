package com.hydravisual.gui;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.Module;
import com.hydravisual.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Главное меню — красивый тёмный UI с боковым меню
 * Right Shift — открыть/закрыть
 */
public class HydraScreen extends Screen {

    // ===== ТАБЫ =====
    private enum Tab {
        VISUALS("Visuals", "\uD83D\uDC41", 0xFFa78bfa),
        FRIENDS("Friends", "\uD83D\uDC65", 0xFF60a5fa),
        UTILITIES("Utilities", "\uD83D\uDD27", 0xFF34d399),
        COSMETICS("Cosmetics", "\uD83C\uDFA8", 0xFFf472b6),
        CONFIGS("Configs", "\u2699", 0xFFfbbf24);

        final String label;
        final String icon;
        final int accentColor;
        Tab(String label, String icon, int accent) {
            this.label = label;
            this.icon = icon;
            this.accentColor = accent;
        }
    }

    // ===== ЦВЕТА =====
    private static final int BG_PANEL      = 0xFF0d0d1a;
    private static final int BG_SIDEBAR    = 0xFF0a0a15;
    private static final int BG_CONTENT    = 0xFF111126;
    private static final int BG_CARD       = 0xFF181838;
    private static final int BG_CARD_HOVER = 0xFF1f1f50;
    private static final int BORDER_DIM    = 0xFF1a1a3e;
    private static final int TEXT_WHITE    = 0xFFe8e8f8;
    private static final int TEXT_GRAY     = 0xFF9898b8;
    private static final int TEXT_DIM      = 0xFF505070;
    private static final int TOGGLE_ON     = 0xFF7c3aed;
    private static final int TOGGLE_OFF    = 0xFF252545;

    // ===== СОСТОЯНИЕ =====
    private Tab selectedTab = Tab.VISUALS;
    private final ModuleManager moduleManager;

    // Размеры панели
    private int px, py, pw, ph;
    private static final int SIDEBAR_W = 160;
    private static final int TAB_H = 40;
    private static final int HEADER_H = 60;
    private static final int CARD_H = 48;
    private static final int CARD_GAP = 4;
    private static final int CARD_RADIUS = 4;

    // Анимация
    private float anim = 0f;
    private float tabIndicatorY = -1;
    private float tabIndicatorTargetY = -1;
    private int hoveredCard = -1;

    public HydraScreen() {
        super(Text.literal("Menu"));
        this.moduleManager = HydraVisualClient.INSTANCE.getModuleManager();
    }

    @Override
    protected void init() {
        pw = Math.min(540, width - 40);
        ph = Math.min(380, height - 40);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        anim = 0f;
        tabIndicatorY = -1;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Плавное появление
        anim = Math.min(1f, anim + delta * 0.1f);
        float ease = easeOutCubic(anim);

        if (ease < 0.02f) return;

        int alpha = (int)(ease * 255);

        // Тёмный оверлей на весь экран
        int overlayAlpha = (int)(ease * 120);
        ctx.fill(0, 0, width, height, (overlayAlpha << 24));

        // ===== ТЕНЬ ПАНЕЛИ =====
        for (int i = 3; i >= 1; i--) {
            int shadowAlpha = (int)(ease * 15);
            ctx.fill(px - i, py - i, px + pw + i, py + ph + i, (shadowAlpha << 24) | 0x000008);
        }

        // ===== ПАНЕЛЬ =====
        ctx.fill(px, py, px + pw, py + ph, withAlpha(BG_PANEL, alpha));

        // ===== ВЕРХНЯЯ АКЦЕНТНАЯ ЛИНИЯ (градиент) =====
        drawGradientLine(ctx, px, py, px + pw, py + 2, selectedTab.accentColor, alpha);

        // ===== САЙДБАР =====
        ctx.fill(px, py, px + SIDEBAR_W, py + ph, withAlpha(BG_SIDEBAR, alpha));
        // Разделитель сайдбар/контент
        ctx.fill(px + SIDEBAR_W, py + 2, px + SIDEBAR_W + 1, py + ph, withAlpha(BORDER_DIM, alpha));

        drawSidebar(ctx, mouseX, mouseY, alpha, delta);

        // ===== КОНТЕНТ =====
        drawContent(ctx, mouseX, mouseY, alpha);

        // ===== НИЖНЯЯ ТОНКАЯ ЛИНИЯ =====
        drawGradientLine(ctx, px, py + ph - 1, px + pw, py + ph, selectedTab.accentColor, alpha / 4);
    }

    // ===== САЙДБАР =====
    private void drawSidebar(DrawContext ctx, int mx, int my, int alpha, float delta) {
        // Лого — просто "CP" бейдж
        int logoX = px + 16;
        int logoY = py + 16;

        // Квадратный бейдж с градиентом
        int badgeSize = 32;
        ctx.fill(logoX, logoY, logoX + badgeSize, logoY + badgeSize, withAlpha(0xFF7c3aed, alpha));
        ctx.fill(logoX + 1, logoY + 1, logoX + badgeSize - 1, logoY + badgeSize - 1, withAlpha(0xFF1a1040, alpha));
        ctx.drawText(textRenderer, "CP", logoX + 8, logoY + 12, withAlpha(0xFFa78bfa, alpha), false);

        // Название рядом
        ctx.drawText(textRenderer, "Client", logoX + badgeSize + 8, logoY + 6, withAlpha(TEXT_WHITE, alpha), false);
        ctx.drawText(textRenderer, "Pasta", logoX + badgeSize + 8, logoY + 18, withAlpha(TEXT_DIM, alpha), false);

        // Разделитель
        int sepY = logoY + badgeSize + 12;
        drawFadeLine(ctx, px + 14, sepY, px + SIDEBAR_W - 14, sepY + 1, alpha / 4);

        // ===== ТАБЫ =====
        int tabStartY = sepY + 10;
        float targetY = tabStartY;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int tabY = tabStartY + i * TAB_H;
            boolean selected = tab == selectedTab;
            boolean hovered = mx >= px + 6 && mx <= px + SIDEBAR_W - 6
                           && my >= tabY && my < tabY + TAB_H;

            if (selected) {
                targetY = tabY;
            }

            // Фон таба
            if (selected) {
                // Подсвеченный фон
                ctx.fill(px + 6, tabY + 2, px + SIDEBAR_W - 6, tabY + TAB_H - 2,
                        withAlpha(tab.accentColor, alpha / 8));
                // Боковая метка
                ctx.fill(px + 6, tabY + 4, px + 6, tabY + TAB_H - 4,
                        withAlpha(tab.accentColor, alpha));
            } else if (hovered) {
                ctx.fill(px + 6, tabY + 2, px + SIDEBAR_W - 6, tabY + TAB_H - 2,
                        withAlpha(0xFFFFFFFF, alpha / 15));
            }

            // Иконка
            int iconX = px + 20;
            int textY = tabY + (TAB_H - 9) / 2;
            ctx.drawText(textRenderer, tab.icon, iconX, textY,
                    withAlpha(selected ? tab.accentColor : TEXT_DIM, alpha), false);

            // Текст
            int labelX = iconX + 16;
            int textColor = selected ? withAlpha(TEXT_WHITE, alpha) : withAlpha(TEXT_GRAY, alpha);
            ctx.drawText(textRenderer, tab.label, labelX, textY, textColor, false);
        }

        // Анимированный боковой индикатор
        if (tabIndicatorY < 0) {
            tabIndicatorY = targetY;
        } else {
            tabIndicatorY += (targetY - tabIndicatorY) * Math.min(1f, delta * 8f);
        }
        int indY = (int) tabIndicatorY;
        ctx.fill(px, indY + 6, px + 3, indY + TAB_H - 6,
                withAlpha(selectedTab.accentColor, alpha));

        // Бинд в нижней части сайдбара
        int bottomY = py + ph - 22;
        String bindText = "[RShift] toggle";
        int bindW = textRenderer.getWidth(bindText);
        int bindX = px + (SIDEBAR_W - bindW) / 2;
        ctx.drawText(textRenderer, bindText, bindX, bottomY,
                withAlpha(TEXT_DIM, alpha / 2), false);
    }

    // ===== КОНТЕНТ =====
    private void drawContent(DrawContext ctx, int mx, int my, int alpha) {
        int cx = px + SIDEBAR_W + 16;
        int cy = py + 18;
        int cw = pw - SIDEBAR_W - 32;

        // Заголовок таба
        ctx.drawText(textRenderer, selectedTab.label, cx, cy,
                withAlpha(selectedTab.accentColor, alpha), false);

        // Подчёркивание
        int titleW = textRenderer.getWidth(selectedTab.label);
        ctx.fill(cx, cy + 12, cx + titleW, cy + 13,
                withAlpha(selectedTab.accentColor, alpha / 4));

        cy += 24;

        switch (selectedTab) {
            case VISUALS -> drawModuleCards(ctx, cx, cy, cw, mx, my, alpha);
            case FRIENDS -> drawPlaceholder(ctx, cx, cy, cw, alpha, "Друзья", "Список друзей пока пуст");
            case UTILITIES -> drawPlaceholder(ctx, cx, cy, cw, alpha, "Утилиты", "Будет добавлено позже");
            case COSMETICS -> drawPlaceholder(ctx, cx, cy, cw, alpha, "Косметика", "Будет добавлено позже");
            case CONFIGS -> drawPlaceholder(ctx, cx, cy, cw, alpha, "Конфиги", "Управление конфигами скоро");
        }
    }

    // ===== КАРТОЧКИ МОДУЛЕЙ =====
    private void drawModuleCards(DrawContext ctx, int x, int y, int w, int mx, int my, int alpha) {
        List<Module> modules = moduleManager.getModules();
        hoveredCard = -1;

        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            int cardY = y + i * (CARD_H + CARD_GAP);
            boolean hovered = mx >= x && mx <= x + w && my >= cardY && my < cardY + CARD_H;

            if (hovered) hoveredCard = i;

            // Фон карточки
            int cardBg = hovered ? withAlpha(BG_CARD_HOVER, alpha) : withAlpha(BG_CARD, alpha);
            ctx.fill(x, cardY, x + w, cardY + CARD_H, cardBg);

            // Рамка сверху и снизу
            ctx.fill(x, cardY, x + w, cardY + 1, withAlpha(BORDER_DIM, alpha / 2));
            ctx.fill(x, cardY + CARD_H - 1, x + w, cardY + CARD_H, withAlpha(BORDER_DIM, alpha / 3));

            // Левая акцентная полоска если включён
            if (mod.isEnabled()) {
                ctx.fill(x, cardY + 1, x + 3, cardY + CARD_H - 1, withAlpha(TOGGLE_ON, alpha));
            }

            // Название модуля
            int nameColor = mod.isEnabled() ? withAlpha(TEXT_WHITE, alpha) : withAlpha(TEXT_GRAY, alpha);
            ctx.drawText(textRenderer, mod.getName(), x + 12, cardY + 10, nameColor, false);

            // Описание
            ctx.drawText(textRenderer, mod.getDescription(), x + 12, cardY + 24,
                    withAlpha(TEXT_DIM, alpha), false);

            // Переключатель
            drawToggle(ctx, x + w - 40, cardY + (CARD_H - 14) / 2, mod.isEnabled(), alpha);
        }
    }

    // ===== ПЕРЕКЛЮЧАТЕЛЬ ON/OFF =====
    private void drawToggle(DrawContext ctx, int x, int y, boolean on, int alpha) {
        int w = 30, h = 14;

        // Трек
        int trackColor = on ? withAlpha(TOGGLE_ON, alpha) : withAlpha(TOGGLE_OFF, alpha);
        ctx.fill(x, y, x + w, y + h, trackColor);

        // Свечение
        if (on) {
            ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, withAlpha(TOGGLE_ON, alpha / 8));
        }

        // Ползунок
        int knobSize = h - 4;
        int knobX = on ? x + w - knobSize - 2 : x + 2;
        int knobColor = on ? withAlpha(0xFFFFFFFF, alpha) : withAlpha(0xFF606080, alpha);
        ctx.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, knobColor);
    }

    // ===== ЗАГЛУШКА ДЛЯ ПУСТЫХ ТАБОВ =====
    private void drawPlaceholder(DrawContext ctx, int x, int y, int w, int alpha,
                                  String title, String subtitle) {
        int centerX = x + w / 2;
        int centerY = y + (ph - HEADER_H) / 2 - 30;

        // Иконка — точки
        String dots = "• • •";
        int dotsW = textRenderer.getWidth(dots);
        ctx.drawText(textRenderer, dots, centerX - dotsW / 2, centerY - 14,
                withAlpha(TEXT_DIM, alpha / 3), false);

        // Заголовок
        int titleW = textRenderer.getWidth(title);
        ctx.drawText(textRenderer, title, centerX - titleW / 2, centerY + 4,
                withAlpha(TEXT_GRAY, alpha), false);

        // Подзаголовок
        int subW = textRenderer.getWidth(subtitle);
        ctx.drawText(textRenderer, subtitle, centerX - subW / 2, centerY + 18,
                withAlpha(TEXT_DIM, alpha / 2), false);
    }

    // ===== ГРАДИЕНТНАЯ ЛИНИЯ =====
    private void drawGradientLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int alpha) {
        ctx.fillGradient(x1, y1, x2, y2, withAlpha(color, alpha), withAlpha(color, alpha / 2));
    }

    // ===== ПЛАВНАЯ ЛИНИЯ =====
    private void drawFadeLine(DrawContext ctx, int x1, int y1, int x2, int y2, int alpha) {
        ctx.fillGradient(x1, y1, x2, y2, withAlpha(0xFFFFFFFF, alpha / 2), withAlpha(0xFF000000, 0));
    }

    // ===== ВВОД =====

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Клик по табам
        int sepY = py + 16 + 32 + 12;
        int tabStartY = sepY + 10;
        for (int i = 0; i < Tab.values().length; i++) {
            int tabY = tabStartY + i * TAB_H;
            if (mouseX >= px + 6 && mouseX <= px + SIDEBAR_W - 6
                    && mouseY >= tabY && mouseY < tabY + TAB_H) {
                selectedTab = Tab.values()[i];
                return true;
            }
        }

        // Клик по модулям (toggle)
        if (selectedTab == Tab.VISUALS) {
            int cx = px + SIDEBAR_W + 16;
            int cy = py + 18 + 24;
            int cw = pw - SIDEBAR_W - 32;
            List<Module> modules = moduleManager.getModules();
            for (int i = 0; i < modules.size(); i++) {
                int cardY = cy + i * (CARD_H + CARD_GAP);
                if (mouseX >= cx && mouseX <= cx + cw
                        && mouseY >= cardY && mouseY < cardY + CARD_H) {
                    modules.get(i).toggle();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ===== УТИЛИТЫ =====

    private static float easeOutCubic(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
