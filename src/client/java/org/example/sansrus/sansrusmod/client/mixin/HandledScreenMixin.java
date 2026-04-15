package org.example.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Unique
    private boolean isShiftDragging = false;
    @Unique
    private Slot lastDraggedSlot = null;

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.shiftDragItems) return;
        if (button == 0 && hasShiftDown()) {
            isShiftDragging = true;
            lastDraggedSlot = null;
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.shiftDragItems) return;
        if (button == 0) {
            isShiftDragging = false;
            lastDraggedSlot = null;
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.shiftDragItems) return;
        if (isShiftDragging && focusedSlot != null && focusedSlot != lastDraggedSlot) {
            if (focusedSlot.hasStack()) {
                onMouseClick(focusedSlot, focusedSlot.id, 0, SlotActionType.QUICK_MOVE);
                lastDraggedSlot = focusedSlot;
            }
        }
    }

    @Unique
    private boolean hasShiftDown() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS 
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
