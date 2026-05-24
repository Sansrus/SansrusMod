package ru.sansrus.sansrusmod.client.mixin.armorbar;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class ArmorBarHudMixin {

    @Shadow @Final
    private static Identifier HOTBAR_OFFHAND_LEFT_SPRITE;

    @Unique private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS,
            EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    @Unique private static final float ANIM_TICKS = 8f;
    @Unique private static final int SLIDE_OFFSET = 26;
    @Unique private static final int LOW_DUR = 20;

    @Unique private final ItemStack[] sansrus$prev = {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    @Unique private float sansrus$showTimer = 0f;
    @Unique private float sansrus$slideProgress = 0f;

    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    private void sansrus$renderArmorBar(GuiGraphicsExtractor guiGraphics,
                                        DeltaTracker tickCounter,
                                        CallbackInfo ci) {
        int displayMode = SansrusModClient.config.armorbarDisplay;
        if (displayMode == 0) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        if (displayMode == 21) {
            sansrus$renderStatic(guiGraphics, client, player);
            return;
        }

        float showTicks = displayMode * 20f;
        float dt = tickCounter.getGameTimeDeltaTicks();

        if (sansrus$checkAndSync(player)) {
            sansrus$showTimer = showTicks;
        }

        boolean hasCritical = sansrus$hasCritical(player);

        if (!hasCritical) {
            sansrus$showTimer = Math.max(0f, sansrus$showTimer - dt);
        }

        float target = (hasCritical || sansrus$showTimer > 0f) ? 1f : 0f;
        float clampedDt = Math.min(dt, 0.5f);
        float step = clampedDt / ANIM_TICKS;

        if (Math.abs(target - sansrus$slideProgress) <= step) {
            sansrus$slideProgress = target;
        } else if (target > sansrus$slideProgress) {
            sansrus$slideProgress += step;
        } else {
            sansrus$slideProgress -= step;
        }


        if (sansrus$slideProgress <= 0f) return;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int hotbarLeft = screenW / 2 - 91;
        int baseX = hotbarLeft - 29 - 29;

        guiGraphics.enableScissor(0, screenH - SLIDE_OFFSET - 22, screenW, screenH);

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            if (stack.isEmpty()) continue;

            boolean isCritical = stack.isDamageableItem()
                    && stack.getMaxDamage() - stack.getDamageValue() <= LOW_DUR;

            float progress = isCritical ? 1f : sansrus$slideProgress;
            int offsetY = (int) ((1f - progress) * SLIDE_OFFSET);
            int slotY   = screenH - 23 + offsetY;
            int slotX = baseX - i * 21;

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, slotX, slotY, 29, 24);
            guiGraphics.item(stack, slotX + 3, slotY + 4);
            guiGraphics.itemDecorations(client.font, stack, slotX + 3, slotY + 4);
        }

        guiGraphics.disableScissor();
    }

    @Unique
    private void sansrus$renderStatic(GuiGraphicsExtractor guiGraphics,
                                      Minecraft client,
                                      LocalPlayer player) {
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int hotbarLeft = screenW / 2 - 91;
        int baseX = hotbarLeft - 29 - 29;
        int slotY = screenH - 24;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            if (stack.isEmpty()) continue;

            int slotX = baseX - i * 21;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, slotX, slotY, 29, 24);
            guiGraphics.item(stack, slotX + 3, slotY + 4);
            guiGraphics.itemDecorations(client.font, stack, slotX + 3, slotY + 4);
        }
    }

    @Unique
    private boolean sansrus$checkAndSync(LocalPlayer player) {
        boolean changed = false;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack cur = player.getItemBySlot(ARMOR_SLOTS[i]);
            ItemStack prv = sansrus$prev[i];

            boolean diff = cur.isEmpty() != prv.isEmpty()
                    || (!cur.isEmpty() && (cur.getItem() != prv.getItem()
                    || cur.getDamageValue() != prv.getDamageValue()));
            if (diff) changed = true;
            sansrus$prev[i] = cur.isEmpty() ? ItemStack.EMPTY : cur.copy();
        }
        return changed;
    }

    @Unique
    private boolean sansrus$hasCritical(LocalPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack s = player.getItemBySlot(slot);
            if (!s.isEmpty() && s.isDamageableItem()
                    && s.getMaxDamage() - s.getDamageValue() <= LOW_DUR) {
                return true;
            }
        }
        return false;
    }
}
