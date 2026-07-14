package ru.sansrus.sansrusmod.client.mixin.highlight;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class MatchingSlotHighlightMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected Slot hoveredSlot;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void sansrus$highlightMatchingSlots(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!SansrusModClient.config.matchingSlotHighlight) return;
        if (hoveredSlot == null || hoveredSlot.getItem().isEmpty()) return;

        ItemStack hovered = hoveredSlot.getItem();
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;

        boolean isEnchantedBook = hovered.getItem() == Items.ENCHANTED_BOOK;

        for (Slot slot : self.getMenu().slots) {
            if (slot == hoveredSlot) continue;

            ItemStack candidate = slot.getItem();
            if (candidate.isEmpty()) continue;

            boolean shouldHighlight = false;

            if (isEnchantedBook) {
                if (candidate.getItem() == Items.ENCHANTED_BOOK) {
                    shouldHighlight = hasMatchingEnchantments(hovered, candidate);
                }
            } else {
                shouldHighlight = candidate.getItem() == hovered.getItem();
            }

            if (shouldHighlight) {
                int slotX = this.leftPos + slot.x;
                int slotY = this.topPos + slot.y;
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, SansrusModClient.config.highlightColor);
            }
        }
    }

    @Unique
    private Set<Holder<Enchantment>> getEnchantments(ItemStack stack) {
        Set<Holder<Enchantment>> enchantments = new HashSet<>();
        
        ItemEnchantments storedEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchantments != null && !storedEnchantments.isEmpty()) {
            for (var entry : storedEnchantments.entrySet()) {
                enchantments.add(entry.getKey());
            }
        }
        
        ItemEnchantments regularEnchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (regularEnchantments != null && !regularEnchantments.isEmpty()) {
            for (var entry : regularEnchantments.entrySet()) {
                enchantments.add(entry.getKey());
            }
        }
        
        return enchantments;
    }

    @Unique
    private boolean hasMatchingEnchantments(ItemStack stack1, ItemStack stack2) {
        Set<Holder<Enchantment>> enchantments1 = getEnchantments(stack1);
        Set<Holder<Enchantment>> enchantments2 = getEnchantments(stack2);
        
        for (Holder<Enchantment> enchantment : enchantments1) {
            if (enchantments2.contains(enchantment)) {
                return true;
            }
        }
        
        return false;
    }
}
