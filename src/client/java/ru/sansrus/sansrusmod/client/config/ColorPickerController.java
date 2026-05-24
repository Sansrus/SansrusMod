package ru.sansrus.sansrusmod.client.config;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ColorPickerController implements Controller<Integer> {
    private final Option<Integer> option;

    public ColorPickerController(Option<Integer> option) {
        this.option = option;
    }

    @Override
    public Option<Integer> option() {
        return option;
    }

    @Override
    public Component formatValue() {
        int color = option.pendingValue();
        return Component.literal(String.format("#%08X", color));
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ColorPickerControllerElement(this, screen, widgetDimension);
    }

    public static class ColorPickerControllerElement extends AbstractWidget {
        private final ColorPickerController control;
        private final YACLScreen yaclScreen;

        public ColorPickerControllerElement(ColorPickerController control, YACLScreen screen, Dimension<Integer> dim) {
            super(dim);
            this.control = control;
            this.yaclScreen = screen;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
            // Render button with current color
            int x = getDimension().x();
            int y = getDimension().y();
            int width = getDimension().width();
            int height = getDimension().height();
            
            // Draw button background
            guiGraphics.fill(x, y, x + width, y + height, 0xFF000000);
            guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF404040);
            
            // Draw color preview square (16x16)
            int colorSquareSize = 16;
            int colorX = x + 5;
            int colorY = y + (height - colorSquareSize) / 2;
            
            // Draw checkerboard pattern for transparency
            drawCheckerboard(guiGraphics, colorX, colorY, colorSquareSize, colorSquareSize);
            
            // Draw actual color
            int currentColor = control.option().pendingValue();
            guiGraphics.fill(colorX, colorY, colorX + colorSquareSize, colorY + colorSquareSize, currentColor);
            
            // Draw color text
            String colorText = String.format("#%08X", currentColor);
            guiGraphics.text(Minecraft.getInstance().font, colorText, colorX + colorSquareSize + 5, 
                           y + (height - Minecraft.getInstance().font.lineHeight) / 2, 0xFFFFFFFF, false);
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
            if (event.button() != 0) return false;
            
            double mouseX = event.x();
            double mouseY = event.y();
            
            int x = getDimension().x();
            int y = getDimension().y();
            int width = getDimension().width();
            int height = getDimension().height();
            
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                // Open color picker screen
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(new ColorPickerScreen(yaclScreen, control.option().pendingValue(), 
                        selectedColor -> control.option().requestSet(selectedColor)));
                }
                return true;
            }
            
            return false;
        }

        @Override
        public void unfocus() {
            // Nothing to do
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
            // Nothing to do
        }
    }
}
