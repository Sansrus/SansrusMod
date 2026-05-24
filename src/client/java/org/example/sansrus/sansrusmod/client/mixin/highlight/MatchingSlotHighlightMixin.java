package org.example.sansrus.sansrusmod.client.mixin.highlight;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class MatchingSlotHighlightMixin {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void sansrus$highlightMatchingSlots(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!SansrusModClient.config.matchingSlotHighlight) return;
        if (focusedSlot == null || focusedSlot.getStack().isEmpty()) return;

        ItemStack hovered = focusedSlot.getStack();
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;

        for (Slot slot : self.getScreenHandler().slots) {
            if (slot == focusedSlot) continue;

            ItemStack candidate = slot.getStack();
            if (candidate.isEmpty()) continue;
            if (candidate.getItem() != hovered.getItem()) continue;

            int slotX = this.x + slot.x;
            int slotY = this.y + slot.y;

            context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x6000FF00);
        }
    }
}
