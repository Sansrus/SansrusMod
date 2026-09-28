package ru.sansrus.sansrusmod.client.mixin.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonInfo;
import ru.sansrus.sansrusmod.client.mixin.MouseHandlerAccessor;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookViewScreen.class)
public abstract class BookScreenMixin {

    @Shadow private PageButton forwardButton;
    @Shadow private PageButton backButton;

    @Unique private int sansrus$holdTicks = 0;
    @Unique private boolean sansrus$wasHeld = false;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void sansrus$onRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!SansrusModClient.config.bookPageHold) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        MouseButtonInfo activeButton = ((MouseHandlerAccessor) client.mouseHandler).getActiveButton();
        boolean isHeld = activeButton != null && activeButton.button() == InputConstants.MOUSE_BUTTON_LEFT;

        boolean nextHovered = forwardButton != null && forwardButton.isHovered();
        boolean prevHovered = backButton != null && backButton.isHovered();

        if (isHeld && (nextHovered || prevHovered)) {
            sansrus$holdTicks++;
            if (!sansrus$wasHeld) {
                sansrus$holdTicks = 0;
            }
            int interval = Math.max(1, 41 - SansrusModClient.config.bookScrollSpeed);
            if (sansrus$holdTicks >= 20 && (sansrus$holdTicks - 20) % interval == 0) {
                if (nextHovered) {
                    forwardButton.onPress(null);
                } else {
                    backButton.onPress(null);
                }
            }
        } else {
            sansrus$holdTicks = 0;
        }

        sansrus$wasHeld = isHeld && (nextHovered || prevHovered);
    }
}
