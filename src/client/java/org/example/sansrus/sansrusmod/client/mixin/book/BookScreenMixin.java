package org.example.sansrus.sansrusmod.client.mixin.book;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.PageTurnWidget;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookScreen.class)
public abstract class BookScreenMixin {

    @Shadow private PageTurnWidget nextPageButton;
    @Shadow private PageTurnWidget previousPageButton;

    @Unique private int sansrus$holdTicks = 0;
    @Unique private boolean sansrus$wasHeld = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void sansrus$onRender(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!SansrusModClient.config.bookPageHold) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        long handle = client.getWindow().getHandle();
        boolean isHeld = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        boolean nextHovered = nextPageButton != null && nextPageButton.isHovered();
        boolean prevHovered = previousPageButton != null && previousPageButton.isHovered();

        if (isHeld && (nextHovered || prevHovered)) {
            sansrus$holdTicks++;
            if (!sansrus$wasHeld) {
                sansrus$holdTicks = 0;
            }
            int interval = Math.max(1, 41 - SansrusModClient.config.bookScrollSpeed);
            if (sansrus$holdTicks >= 20 && (sansrus$holdTicks - 20) % interval == 0) {
                if (nextHovered) {
                    nextPageButton.onPress();
                } else {
                    previousPageButton.onPress();
                }
            }
        } else {
            sansrus$holdTicks = 0;
        }

        sansrus$wasHeld = isHeld && (nextHovered || prevHovered);
    }
}
