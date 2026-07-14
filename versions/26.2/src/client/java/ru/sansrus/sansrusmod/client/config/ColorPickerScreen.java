package ru.sansrus.sansrusmod.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final ColorCallback callback;

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 210;
    private static final int SLIDER_WIDTH = 180;
    private static final int SLIDER_HEIGHT = 20;

    private int alpha, red, green, blue;

    private int panelX, panelY;

    private String notificationText = null;
    private long notificationStartTime = 0;
    private static final long NOTIFICATION_DURATION = 3000;
    private static final long NOTIFICATION_FADE_TIME = 400;

    public ColorPickerScreen(Screen parent, int initialColor, ColorCallback callback) {
        super(Component.translatable("sansrusmod.config.colorPicker.title"));
        this.parent = parent;
        this.callback = callback;
        updateFromColor(initialColor);
    }

    @Override
    protected void init() {
        super.init();

        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        int sliderX = panelX + (PANEL_WIDTH - SLIDER_WIDTH) / 2;
        int startY = panelY + 40;
        int spacing = SLIDER_HEIGHT + 6;

        addRenderableWidget(new ChannelSlider(sliderX, startY,               0, "A", alpha));
        addRenderableWidget(new ChannelSlider(sliderX, startY + spacing,    1, "R", red));
        addRenderableWidget(new ChannelSlider(sliderX, startY + spacing * 2,2, "G", green));
        addRenderableWidget(new ChannelSlider(sliderX, startY + spacing * 3,3, "B", blue));

        int btnY = panelY + PANEL_HEIGHT - 30;
        int btnW = 60;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> {
            callback.onColorSelected(argbToInt(alpha, red, green, blue));
            onClose();
        }).bounds(panelX + PANEL_WIDTH - btnW - 10, btnY, btnW, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> {
            onClose();
        }).bounds(panelX + PANEL_WIDTH - btnW * 2 - 15, btnY, btnW, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xC0000000);
        g.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF444444);

        g.text(this.font, this.title, panelX + (PANEL_WIDTH - this.font.width(this.title)) / 2, panelY + 8, 0xFFFFFFFF, false);

        int previewSize = 18;
        int previewX = panelX + 10;
        int startY = panelY + 40;
        int spacing = SLIDER_HEIGHT + 6;
        int previewY = startY + (spacing * 4 - previewSize) / 2;

        drawCheckerboard(g, previewX, previewY, previewSize, previewSize);
        g.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, argbToInt(alpha, red, green, blue));

        String hexText = String.format("#%08X", argbToInt(alpha, red, green, blue));
        int hexX = panelX + 10;
        int hexY = panelY + PANEL_HEIGHT - 46;
        boolean hoverHex = mouseX >= hexX && mouseX < hexX + this.font.width(hexText) && mouseY >= hexY && mouseY < hexY + this.font.lineHeight;
        g.text(this.font, hexText, hexX, hexY, hoverHex ? 0xFFFFFF00 : 0xFFAAAAAA, false);

        renderNotification(g);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawCheckerboard(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int sq = 4;
        for (int i = 0; i < w; i += sq) {
            for (int j = 0; j < h; j += sq) {
                boolean even = ((i / sq) + (j / sq)) % 2 == 0;
                int x2 = Math.min(x + i + sq, x + w);
                int y2 = Math.min(y + j + sq, y + h);
                g.fill(x + i, y + j, x2, y2, even ? 0xFFCCCCCC : 0xFF999999);
            }
        }
    }

    private void renderNotification(GuiGraphicsExtractor g) {
        if (notificationText == null) return;
        long elapsed = System.currentTimeMillis() - notificationStartTime;
        if (elapsed > NOTIFICATION_DURATION) { notificationText = null; return; }
        float a = elapsed > NOTIFICATION_DURATION - NOTIFICATION_FADE_TIME
                ? 1f - (float)(elapsed - (NOTIFICATION_DURATION - NOTIFICATION_FADE_TIME)) / NOTIFICATION_FADE_TIME
                : 1f;
        float slide = Math.min(1f, elapsed / 200f);
        int nw = this.font.width(notificationText) + 16;
        int nx = this.width - (int)(nw * slide);
        int ny = 8;
        g.fill(nx, ny, nx + nw, ny + 24, ((int)(a * 180)) << 24 | 0x2B2B2B);
        g.text(this.font, notificationText, nx + 8, ny + 8, ((int)(a * 255)) << 24 | 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean skipNarration) {
        String hexText = String.format("#%08X", argbToInt(alpha, red, green, blue));
        int hexX = panelX + 10;
        int hexY = panelY + PANEL_HEIGHT - 46;
        if (event.button() == 0 && event.x() >= hexX && event.x() < hexX + this.font.width(hexText)
                && event.y() >= hexY && event.y() < hexY + this.font.lineHeight) {
            Minecraft.getInstance().keyboardHandler.setClipboard(hexText);
            notificationText = "Copied: " + hexText;
            notificationStartTime = System.currentTimeMillis();
            return true;
        }
        return super.mouseClicked(event, skipNarration);
    }

    private void updateFromColor(int color) {
        alpha = (color >> 24) & 0xFF;
        red   = (color >> 16) & 0xFF;
        green = (color >> 8)  & 0xFF;
        blue  =  color        & 0xFF;
    }

    private int argbToInt(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public interface ColorCallback {
        void onColorSelected(int color);
    }

    private class ChannelSlider extends AbstractSliderButton {
        private final int channel;
        private final String label;

        ChannelSlider(int x, int y, int channel, String label, int initialValue) {
            super(x, y, SLIDER_WIDTH, SLIDER_HEIGHT, Component.empty(), initialValue / 255.0);
            this.channel = channel;
            this.label = label;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int v = (int) Mth.clamp(this.value * 255, 0, 255);
            setMessage(Component.literal(label + ": " + v));
        }

        @Override
        protected void applyValue() {
            int v = (int) Mth.clamp(this.value * 255, 0, 255);
            switch (channel) {
                case 0 -> ColorPickerScreen.this.alpha = v;
                case 1 -> red   = v;
                case 2 -> green = v;
                case 3 -> blue  = v;
            }
        }
    }
}
