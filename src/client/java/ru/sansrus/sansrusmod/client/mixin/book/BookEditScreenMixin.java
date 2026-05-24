package ru.sansrus.sansrusmod.client.mixin.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin {

    @Shadow private PageButton forwardButton;
    @Shadow private PageButton backButton;

    @Shadow public abstract void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks);

    @Unique private int sansrus$holdTicks = 0;
    @Unique private boolean sansrus$wasHeld = false;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!SansrusModClient.config.bookPageHold) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        long handle = client.getWindow().handle();
        boolean isHeld = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

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
