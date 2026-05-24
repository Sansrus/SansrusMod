package ru.sansrus.sansrusmod.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final int initialColor;
    private final ColorCallback callback;
    
    private static final int POPUP_WIDTH = 280;
    private static final int POPUP_HEIGHT = 240;
    
    private static final int SLIDER_WIDTH = 200;
    private static final int SLIDER_HEIGHT = 10;
    
    private int alpha, red, green, blue;
    
    private int draggingSlider = -1;
    
    private int popupX, popupY;
    
    private String notificationText = null;
    private long notificationStartTime = 0;
    private static final long NOTIFICATION_DURATION = 5000;
    private static final int NOTIFICATION_FADE_TIME = 500;

    public ColorPickerScreen(Screen parent, int initialColor, ColorCallback callback) {
        super(Component.translatable("sansrusmod.config.colorPicker.title"));
        this.parent = parent;
        this.initialColor = initialColor;
        this.callback = callback;
        updateFromColor(initialColor);
    }

    @Override
    protected void init() {
        super.init();
        
        popupX = (this.width - POPUP_WIDTH) / 2;
        popupY = (this.height - POPUP_HEIGHT) / 2;
        
        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonX = popupX + POPUP_WIDTH - buttonWidth - 10;
        int buttonY = popupY + POPUP_HEIGHT - buttonHeight - 10;
        
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            callback.onColorSelected(argbToInt(alpha, red, green, blue));
            this.onClose();
        }).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());
        
        int cancelButtonX = buttonX - buttonWidth - 5;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
            this.onClose();
        }).bounds(cancelButtonX, buttonY, buttonWidth, buttonHeight).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        guiGraphics.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFF2B2B2B);
        guiGraphics.fill(popupX + 1, popupY + 1, popupX + POPUP_WIDTH - 1, popupY + POPUP_HEIGHT - 1, 0xFF1E1E1E);
        
        int contentX = popupX + 10;
        int contentY = popupY + 10;
        
        guiGraphics.text(this.font, this.title, contentX, contentY, 0xFFFFFFFF, false);
        contentY += 20;
        
        int previewSize = 40;
        drawCheckerboard(guiGraphics, contentX, contentY, previewSize, previewSize);
        int previewColor = argbToInt(alpha, red, green, blue);
        guiGraphics.fill(contentX, contentY, contentX + previewSize, contentY + previewSize, previewColor);
        contentY += previewSize + 10;
        
        contentY = renderSlider(guiGraphics, contentX, contentY, "A", alpha, 0xFF000000 | (alpha << 24), mouseX, mouseY, 0);
        contentY = renderSlider(guiGraphics, contentX, contentY, "R", red, 0xFF000000 | (red << 16), mouseX, mouseY, 1);
        contentY = renderSlider(guiGraphics, contentX, contentY, "G", green, 0xFF000000 | (green << 8), mouseX, mouseY, 2);
        contentY = renderSlider(guiGraphics, contentX, contentY, "B", blue, 0xFF000000 | blue, mouseX, mouseY, 3);
        
        contentY += 5;
        
        guiGraphics.text(this.font, "HEX:", contentX, contentY, 0xFFAAAAAA, false);
        String hexText = String.format("#%08X", previewColor);
        int hexTextX = contentX + 40;
        boolean hoveringHex = mouseX >= hexTextX && mouseX <= hexTextX + this.font.width(hexText) && 
                              mouseY >= contentY && mouseY <= contentY + this.font.lineHeight;
        guiGraphics.text(this.font, hexText, hexTextX, contentY, hoveringHex ? 0xFFFFFF00 : 0xFFFFFFFF, false);
        contentY += 15;
        
        guiGraphics.text(this.font, "INT:", contentX, contentY, 0xFFAAAAAA, false);
        String intText = String.valueOf(previewColor);
        int intTextX = contentX + 40;
        boolean hoveringInt = mouseX >= intTextX && mouseX <= intTextX + this.font.width(intText) && 
                              mouseY >= contentY && mouseY <= contentY + this.font.lineHeight;
        guiGraphics.text(this.font, intText, intTextX, contentY, hoveringInt ? 0xFFFFFF00 : 0xFFFFFFFF, false);
        
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
        
        renderNotification(guiGraphics);
    }
    
    private void renderNotification(GuiGraphicsExtractor guiGraphics) {
        if (notificationText == null) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - notificationStartTime;
        
        if (elapsed > NOTIFICATION_DURATION) {
            notificationText = null;
            return;
        }
        
        float alpha = 1.0f;
        if (elapsed > NOTIFICATION_DURATION - NOTIFICATION_FADE_TIME) {
            long fadeElapsed = elapsed - (NOTIFICATION_DURATION - NOTIFICATION_FADE_TIME);
            alpha = 1.0f - (fadeElapsed / (float) NOTIFICATION_FADE_TIME);
        }
        
        float slideProgress = Math.min(1.0f, elapsed / 300.0f);
        int notificationWidth = this.font.width(notificationText) + 20;
        int notificationHeight = 30;
        int notificationX = this.width - (int)(notificationWidth * slideProgress);
        int notificationY = 10;
        
        int bgAlpha = (int)(alpha * 200) << 24;
        guiGraphics.fill(notificationX, notificationY, notificationX + notificationWidth, notificationY + notificationHeight, bgAlpha | 0x2B2B2B);
        
        int textAlpha = (int)(alpha * 255) << 24;
        guiGraphics.text(this.font, notificationText, notificationX + 10, notificationY + 10, textAlpha | 0xFFFFFF, false);
    }

    private int renderSlider(GuiGraphicsExtractor guiGraphics, int x, int y, String label, int value, int color, int mouseX, int mouseY, int sliderIndex) {
        guiGraphics.text(this.font, label + ":", x, y, 0xFFAAAAAA, false);
        guiGraphics.text(this.font, String.valueOf(value), x + 20, y, 0xFFFFFFFF, false);
        
        int sliderX = x + 60;
        int sliderY = y;
        guiGraphics.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + SLIDER_HEIGHT, 0xFF000000);
        
        int fillWidth = (int) ((value / 255.0) * SLIDER_WIDTH);
        guiGraphics.fill(sliderX, sliderY, sliderX + fillWidth, sliderY + SLIDER_HEIGHT, color);
        
        int handleX = sliderX + fillWidth - 2;
        guiGraphics.fill(handleX, sliderY - 2, handleX + 4, sliderY + SLIDER_HEIGHT + 2, 0xFFFFFFFF);
        
        return y + SLIDER_HEIGHT + 8;
    }

    private void drawCheckerboard(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height) {
        int squareSize = 4;
        for (int i = 0; i < width; i += squareSize) {
            for (int j = 0; j < height; j += squareSize) {
                boolean isEven = ((i / squareSize) + (j / squareSize)) % 2 == 0;
                int color = isEven ? 0xFFCCCCCC : 0xFF999999;
                    guiGraphics.fill(x + i, y + j, x + i + squareSize, y + j + squareSize, color);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean skipNarration) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        
        if (button == 0) {
            int contentX = popupX + 10;
            int contentY = popupY + 70;
            int sliderX = contentX + 60;
            
            for (int i = 0; i < 4; i++) {
                int sliderY = contentY + i * (SLIDER_HEIGHT + 8);
                int hitboxTop = sliderY - 5;
                int hitboxBottom = sliderY + SLIDER_HEIGHT + 5;
                
                if (mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH && 
                    mouseY >= hitboxTop && mouseY <= hitboxBottom) {
                    draggingSlider = i;
                    updateSliderValue(mouseX, sliderX);
                    return true;
                }
            }
            
            int hexY = contentY + 4 * (SLIDER_HEIGHT + 8) + 5;
            int hexTextX = contentX + 40;
            int previewColor = argbToInt(alpha, red, green, blue);
            String hexText = String.format("#%08X", previewColor);
            
            if (mouseX >= hexTextX && mouseX <= hexTextX + this.font.width(hexText) && 
                mouseY >= hexY && mouseY <= hexY + this.font.lineHeight) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.keyboardHandler.setClipboard(hexText);
                    showNotification("Copied: " + hexText);
                }
                return true;
            }
            
            int intY = hexY + 15;
            int intTextX = contentX + 40;
            String intText = String.valueOf(previewColor);
            
            if (mouseX >= intTextX && mouseX <= intTextX + this.font.width(intText) && 
                mouseY >= intY && mouseY <= intY + this.font.lineHeight) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.keyboardHandler.setClipboard(intText);
                    showNotification("Copied: " + intText);
                }
                return true;
            }
        }
        
        return super.mouseClicked(event, skipNarration);
    }
    
    private void showNotification(String text) {
        this.notificationText = text;
        this.notificationStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x();
        if (draggingSlider >= 0) {
            int contentX = popupX + 10;
            int sliderX = contentX + 60;
            updateSliderValue(mouseX, sliderX);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSlider >= 0) {
            draggingSlider = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void updateSliderValue(double mouseX, int sliderX) {
        double relativeX = mouseX - sliderX;
        relativeX = Math.max(0, Math.min(SLIDER_WIDTH, relativeX));
        int value = (int) ((relativeX / SLIDER_WIDTH) * 255);
        
        switch (draggingSlider) {
            case 0: alpha = value; break;
            case 1: red = value; break;
            case 2: green = value; break;
            case 3: blue = value; break;
        }
    }

    private void updateFromColor(int color) {
        alpha = (color >> 24) & 0xFF;
        red = (color >> 16) & 0xFF;
        green = (color >> 8) & 0xFF;
        blue = color & 0xFF;
    }

    private int argbToInt(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public interface ColorCallback {
        void onColorSelected(int color);
    }
}
