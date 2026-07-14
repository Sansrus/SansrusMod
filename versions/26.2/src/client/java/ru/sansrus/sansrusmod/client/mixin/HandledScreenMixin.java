package ru.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected Slot hoveredSlot;

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput action);

    @Unique
    private boolean isShiftDragging = false;
    @Unique
    private Slot lastDraggedSlot = null;

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(MouseButtonEvent event, boolean bool, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.shiftDragItems) return;
        if (event.button() == 0 && event.hasShiftDown()) {
            isShiftDragging = true;
            lastDraggedSlot = null;
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.shiftDragItems) return;
        if (event.button() == 0) {
            isShiftDragging = false;
            lastDraggedSlot = null;
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.shiftDragItems) return;
        if (isShiftDragging && hoveredSlot != null && hoveredSlot != lastDraggedSlot) {
            if (hoveredSlot.hasItem()) {
                slotClicked(hoveredSlot, hoveredSlot.index, 0, ContainerInput.QUICK_MOVE);
                lastDraggedSlot = hoveredSlot;
            }
        }
    }
}
