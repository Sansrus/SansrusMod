package org.example.sansrus.sansrusmod.client.mixin.armorbar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.tooltip.WidgetTooltipPositioner;
import net.minecraft.client.gui.widget.ContainerWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ArmorBarHudMixin {

    @Shadow @Final
    private static Identifier HOTBAR_OFFHAND_LEFT_TEXTURE;

    @Unique private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS,
            EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    // 8 тиков = 0.4с анимация
    @Unique private static final float ANIM_TICKS = 8f;
    @Unique private static final int SLIDE_OFFSET = 26;
    @Unique private static final int LOW_DUR = 20;

    // Предыдущий стейт брони для детекции изменений
    @Unique private final ItemStack[] sansrus$prev = {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    @Unique private float sansrus$showTimer = 0f;
    @Unique private float sansrus$slideProgress = 0f;

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void sansrus$renderArmorBar(DrawContext context,
                                        RenderTickCounter tickCounter,
                                        CallbackInfo ci) {
        int displayMode = SansrusModClient.config.armorbarDisplay;
        if (displayMode == 0) return; // Выкл

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // ── Режим "Всегда" (статичный) ────────────────────────────
        if (displayMode == 21) {
            sansrus$renderStatic(context, client, player);
            return;
        }

        // ── Анимированный режим (1-20 секунд) ─────────────────────
        float showTicks = displayMode * 20f; // конвертируем секунды в тики
        float dt = tickCounter.getDynamicDeltaTicks();

        // 1. Детектируем изменения брони
        if (sansrus$checkAndSync(player)) {
            sansrus$showTimer = showTicks;
        }

        // 2. Проверяем критическую прочность
        boolean hasCritical = sansrus$hasCritical(player);

        // 3. Таймер не убывает если есть критическая броня
        if (!hasCritical) {
            sansrus$showTimer = Math.max(0f, sansrus$showTimer - dt);
        }

        // 4. Плавно двигаем прогресс к цели
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

        // 5. Рендер со смещением: slideProgress=0 → ниже экрана, 1 → нормальная позиция
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int hotbarLeft = screenW / 2 - 91;
        int baseX = hotbarLeft - 29 - 29;

        // Ножницы: не рисуем ниже хотбара
        context.enableScissor(0, screenH - SLIDE_OFFSET - 22, screenW, screenH);

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getEquippedStack(ARMOR_SLOTS[i]);
            if (stack.isEmpty()) continue;

            // Критический слот — всегда на финальной позиции, не анимируется вниз
            boolean isCritical = stack.isDamageable()
                    && stack.getMaxDamage() - stack.getDamage() <= LOW_DUR;

            float progress = isCritical ? 1f : sansrus$slideProgress;
            int offsetY = (int) ((1f - progress) * SLIDE_OFFSET);
            int slotY   = screenH - 23 + offsetY;
            int slotX = baseX - i * 21;

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                    HOTBAR_OFFHAND_LEFT_TEXTURE, slotX, slotY, 29, 24);
            context.drawItem(stack, slotX + 3, slotY + 4);
            context.drawStackOverlay(client.textRenderer, stack, slotX + 3, slotY + 4);
        }

        context.disableScissor();
    }

    @Unique
    private void sansrus$renderStatic(DrawContext context,
                                      MinecraftClient client,
                                      ClientPlayerEntity player) {
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int hotbarLeft = screenW / 2 - 91;
        int baseX = hotbarLeft - 29 - 29;
        int slotY = screenH - 24;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getEquippedStack(ARMOR_SLOTS[i]);
            if (stack.isEmpty()) continue;

            int slotX = baseX - i * 21;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                    HOTBAR_OFFHAND_LEFT_TEXTURE, slotX, slotY, 29, 24);
            context.drawItem(stack, slotX + 3, slotY + 4);
            context.drawStackOverlay(client.textRenderer, stack, slotX + 3, slotY + 4);
        }
    }

    /** Сравнивает текущую броню с предыдущей, обновляет кэш. Возвращает true при изменении. */
    @Unique
    private boolean sansrus$checkAndSync(ClientPlayerEntity player) {
        boolean changed = false;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack cur = player.getEquippedStack(ARMOR_SLOTS[i]);
            ItemStack prv = sansrus$prev[i];

            boolean diff = cur.isEmpty() != prv.isEmpty()
                    || (!cur.isEmpty() && (cur.getItem() != prv.getItem()
                    || cur.getDamage() != prv.getDamage()));
            if (diff) changed = true;
            sansrus$prev[i] = cur.isEmpty() ? ItemStack.EMPTY : cur.copy();
        }
        return changed;
    }

    /** Возвращает true если хотя бы один надетый предмет брони имеет прочность ≤ LOW_DUR. */
    @Unique
    private boolean sansrus$hasCritical(ClientPlayerEntity player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack s = player.getEquippedStack(slot);
            if (!s.isEmpty() && s.isDamageable()
                    && s.getMaxDamage() - s.getDamage() <= LOW_DUR) {
                return true;
            }
        }
        return false;
    }
}
