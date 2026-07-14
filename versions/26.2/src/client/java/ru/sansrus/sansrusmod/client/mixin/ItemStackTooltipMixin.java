package ru.sansrus.sansrusmod.client.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.PotDecorations;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Unique
    private static boolean showComponents = false;
    @Unique
    private static boolean wasPressed = false;

    @Shadow
    public abstract DataComponentMap getComponents();

    @Shadow
    public abstract Item getItem();

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void onGetTooltip(Item.TooltipContext context, Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir) {
        if (!SansrusModClient.config.itemTooltipComponents) return;
        long window = Minecraft.getInstance().getWindow().handle();
        boolean rightAltPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        if (rightAltPressed && !wasPressed) {
            showComponents = !showComponents;
            wasPressed = true;
        } else if (!rightAltPressed) {
            wasPressed = false;
        }

        if (!showComponents) {
            return;
        }

        List<Component> tooltip = cir.getReturnValue();
        DataComponentMap components = this.getComponents();

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("sansrusmod.tooltip.components").withStyle(ChatFormatting.GRAY));

        components.forEach(entry -> {
            DataComponentType<?> componentType = entry.type();
            Object value = entry.value();

            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType);
            String shortName = id != null ? id.getPath() : componentType.toString();

            List<Component> formattedValues = formatSpecificComponent(componentType, value);

            if (formattedValues == null || formattedValues.isEmpty()) {
                return;
            }

            MutableComponent firstLine = Component.literal("  ");
            firstLine.append(Component.literal(shortName).withStyle(ChatFormatting.BLUE));
            firstLine.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
            firstLine.append(formattedValues.get(0));
            tooltip.add(firstLine);

            for (int i = 1; i < formattedValues.size(); i++) {
                MutableComponent line = Component.literal("    ");
                line.append(formattedValues.get(i));
                tooltip.add(line);
            }
        });
    }

    @Unique
    private List<Component> formatSpecificComponent(DataComponentType<?> type, Object value) {
        if (type == DataComponents.TOOLTIP_STYLE ||
                type == DataComponents.CONTAINER ||
                type == DataComponents.BUNDLE_CONTENTS ||
                type == DataComponents.TOOLTIP_DISPLAY ||
                type == DataComponents.WRITTEN_BOOK_CONTENT ||
                type == DataComponents.WRITABLE_BOOK_CONTENT ||
                type == DataComponents.BLOCKS_ATTACKS) {
            return null;
        }

        Component result = null;

        if (type == DataComponents.ENCHANTMENTS || type == DataComponents.STORED_ENCHANTMENTS) {
            return formatEnchantments((ItemEnchantments) value);
        } else if (type == DataComponents.LORE) {
            result = formatLore((ItemLore) value);
        } else if (type == DataComponents.CUSTOM_NAME || type == DataComponents.ITEM_NAME) {
            result = formatText((Component) value);
        } else if (type == DataComponents.DAMAGE) {
            int intVal = (Integer) value;
            if (intVal == 0) return null;
            result = Component.literal(String.valueOf(intVal)).withStyle(ChatFormatting.GOLD);
        } else if (type == DataComponents.MAX_STACK_SIZE ||
                type == DataComponents.REPAIR_COST || type == DataComponents.MAX_DAMAGE) {
            result = Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GOLD);
        } else if (type == DataComponents.RARITY) {
            result = Component.literal(String.valueOf(value)).withStyle(ChatFormatting.YELLOW);
        } else if (type == DataComponents.ATTRIBUTE_MODIFIERS) {
            return formatAttributeModifiers((ItemAttributeModifiers) value);
        } else if (type == DataComponents.ENCHANTABLE) {
            result = formatEnchantable((Enchantable) value);
        } else if (type == DataComponents.REPAIRABLE) {
            result = formatRepairable((Repairable) value);
        } else if (type == DataComponents.TOOL) {
            return formatTool((Tool) value);
        } else if (type == DataComponents.DAMAGE_RESISTANT) {
            result = formatDamageResistant((DamageResistant) value);
        } else if (type == DataComponents.ENCHANTMENT_GLINT_OVERRIDE) {
            result = Component.literal(String.valueOf(value)).withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (type == DataComponents.INTANGIBLE_PROJECTILE ||
                type == DataComponents.GLIDER) {
            result = value instanceof Unit ? Component.literal("true").withStyle(ChatFormatting.GREEN) :
                    Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GREEN);
        } else if (type == DataComponents.ITEM_MODEL) {
            result = formatIdentifier((Identifier) value);
        } else if (type == DataComponents.WEAPON) {
            return formatWeapon((Weapon) value);
        } else if (type == DataComponents.BREAK_SOUND) {
            result = formatSoundEvent((Holder<?>) value);
        } else if (type == DataComponents.POTION_CONTENTS) {
            result = formatPotionContents((PotionContents) value);
        } else if (type == DataComponents.INSTRUMENT) {
            result = formatInstrument((InstrumentComponent) value);
        } else if (type == DataComponents.JUKEBOX_PLAYABLE) {
            result = formatJukeboxPlayable((JukeboxPlayable) value);
        } else if (type == DataComponents.FIREWORKS) {
            result = formatFireworks((Fireworks) value);
        } else if (type == DataComponents.USE_COOLDOWN) {
            result = formatUseCooldown((UseCooldown) value);
        } else if (type == DataComponents.EQUIPPABLE) {
            result = formatEquippable((Equippable) value);
        } else if (type == DataComponents.PROVIDES_TRIM_MATERIAL) {
            result = formatTrimMaterial((Holder<TrimMaterial>) value);
        } else if (type == DataComponents.FOOD) {
            result = formatFood((FoodProperties) value);
        } else if (type == DataComponents.CONSUMABLE) {
            result = formatConsumable((Consumable) value);
        } else if (type == DataComponents.USE_REMAINDER) {
            result = formatUseRemainder((UseRemainder) value);
        } else if (type == DataComponents.SUSPICIOUS_STEW_EFFECTS) {
            return formatSuspiciousStewEffects((SuspiciousStewEffects) value);
        } else if (type == DataComponents.BANNER_PATTERNS) {
            return formatBannerPatterns((BannerPatternLayers) value);
        } else if (type == DataComponents.DEATH_PROTECTION) {
            return formatDeathProtection((DeathProtection) value);
        } else if (type == DataComponents.CHARGED_PROJECTILES) {
            return formatChargedProjectiles((ChargedProjectiles) value);
        } else if (type == DataComponents.POT_DECORATIONS) {
            return formatPotDecorations((PotDecorations) value);
        } else if (type == DataComponents.BLOCK_STATE) {
            result = formatBlockState((BlockItemStateProperties) value);
        } else if (type == DataComponents.BEES) {
            return formatBees((Bees) value);
        } else if (type == DataComponents.OMINOUS_BOTTLE_AMPLIFIER) {
            result = formatOminousBottleAmplifier((OminousBottleAmplifier) value);
        } else if (type == DataComponents.TRIM) {
            result = formatTrim((ArmorTrim) value);
        } else if (type == DataComponents.SWING_ANIMATION) {
            result = formatSwingAnimation((SwingAnimation) value);
        } else if (type == DataComponents.USE_EFFECTS) {
            result = formatUseEffects((UseEffects) value);
        } else if (type == DataComponents.ENTITY_DATA) {
            return formatEntityData((TypedEntityData<?>) value);
        } else if (type == DataComponents.PAINTING_VARIANT) {
            result = formatHolderId((Holder<?>) value);
        } else if (type == DataComponents.ATTACK_RANGE) {
            result = formatAttackRange((AttackRange) value);
        } else if (type == DataComponents.KINETIC_WEAPON) {
            result = formatKineticWeapon((KineticWeapon) value);
        } else if (type == DataComponents.DAMAGE_TYPE) {
            result = formatHolderId((Holder<?>) value);
        } else if (type == DataComponents.PIERCING_WEAPON) {
            result = formatPiercingWeapon((PiercingWeapon) value);
        } else if (type == DataComponents.CHICKEN_VARIANT) {
            result = formatHolderId((Holder<?>) value);
        } else if (type == DataComponents.DYE) {
            result = formatDye((DyeColor) value);
        } else {
            result = formatComponentValue(value);
        }

        return result != null ? java.util.Collections.singletonList(result) : null;
    }

    @Unique
    private List<Component> formatEnchantments(ItemEnchantments enchantments) {
        if (enchantments.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        for (var entry : enchantments.entrySet()) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;

            Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();

            Identifier enchId = enchantment.unwrapKey().map(key -> key.identifier()).orElse(null);
            String enchName = enchId != null ? enchId.getPath() : "unknown";

            result.append(Component.literal("["));
            result.append(Component.literal(enchName).withStyle(ChatFormatting.AQUA));
            result.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
            result.append(Component.literal(String.valueOf(level)).withStyle(ChatFormatting.GOLD));
            result.append(Component.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Component formatLore(ItemLore lore) {
        List<Component> lines = lore.lines();
        if (lines.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            Component line = lines.get(i);
            result.append(Component.literal(line.getString()).withStyle(ChatFormatting.GREEN));
        }

        return result;
    }

    @Unique
    private List<Component> formatAttributeModifiers(ItemAttributeModifiers modifiers) {
        List<ItemAttributeModifiers.Entry> modifiersList = modifiers.modifiers();
        if (modifiersList.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        for (ItemAttributeModifiers.Entry entry : modifiersList) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;

            Holder<Attribute> attribute = entry.attribute();
            AttributeModifier modifier = entry.modifier();

            Identifier attrId = attribute.unwrapKey().map(key -> key.identifier()).orElse(null);
            String attrName = attrId != null ? attrId.getPath() : "unknown";

            double value = modifier.amount();
            String formattedValue;

            switch (modifier.operation()) {
                case ADD_VALUE:
                    formattedValue = (value >= 0 ? "+" : "") + String.format("%.2f", value);
                    break;
                case ADD_MULTIPLIED_BASE:
                    formattedValue = (value >= 0 ? "+" : "") + String.format("%.0f%%", value * 100);
                    break;
                case ADD_MULTIPLIED_TOTAL:
                    formattedValue = (value >= 0 ? "+" : "") + String.format("%.0f%%", value * 100);
                    break;
                default:
                    formattedValue = String.format("%.2f", value);
            }

            result.append(Component.literal("["));
            result.append(Component.literal(attrName).withStyle(ChatFormatting.AQUA));
            result.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
            result.append(Component.literal(formattedValue).withStyle(ChatFormatting.GOLD));
            result.append(Component.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Component formatEnchantable(Enchantable enchantable) {
        int val = enchantable.value();
        if (val == 0) return null;
        return Component.literal(String.valueOf(val)).withStyle(ChatFormatting.GOLD);
    }

    @Unique
    private Component formatRepairable(Repairable repairable) {
        try {
            var items = repairable.items();
            var tagKey = items.unwrapKey();
            if (tagKey.isPresent()) {
                Identifier id = tagKey.get().location();
                return Component.literal(id.getPath()).withStyle(ChatFormatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Component.literal("unknown").withStyle(ChatFormatting.GRAY);
    }

    @Unique
    private List<Component> formatTool(Tool tool) {
        List<Tool.Rule> rules = tool.rules();
        if (rules.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        for (Tool.Rule rule : rules) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;

            result.append(Component.literal("["));

            String blocks = rule.blocks().toString();
            if (blocks.contains("Tag{key=")) {
                int start = blocks.indexOf("key=") + 4;
                int end = blocks.indexOf("}", start);
                if (end > start) {
                    String tagKey = blocks.substring(start, end);
                    Identifier id = Identifier.tryParse(tagKey);
                    result.append(Component.literal(id != null ? id.getPath() : tagKey).withStyle(ChatFormatting.AQUA));
                }
            } else {
                result.append(Component.literal("blocks").withStyle(ChatFormatting.AQUA));
            }

            result.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));

            if (rule.speed().isPresent()) {
                result.append(Component.literal("speed " + String.format("%.1f", rule.speed().get())).withStyle(ChatFormatting.GOLD));
            }
            if (rule.correctForDrops().isPresent()) {
                if (rule.speed().isPresent()) {
                    result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                }
                result.append(Component.literal("drops " + rule.correctForDrops().get()).withStyle(ChatFormatting.GOLD));
            }

            result.append(Component.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Component formatDamageResistant(DamageResistant resistant) {
        try {
            var types = resistant.types();
            var tagKey = types.unwrapKey();
            if (tagKey.isPresent()) {
                Identifier id = tagKey.get().location();
                return Component.literal(id.getPath()).withStyle(ChatFormatting.RED);
            }
        } catch (Exception ignored) {}
        return Component.literal("unknown").withStyle(ChatFormatting.GRAY);
    }

    @Unique
    private List<Component> formatWeapon(Weapon weapon) {
        try {
            String str = weapon.toString();

            MutableComponent result = Component.literal("");

            if (str.contains("itemDamagePerAttack=")) {
                int start = str.indexOf("itemDamagePerAttack=") + 20;
                int end = str.indexOf(",", start);
                if (end == -1) end = str.indexOf("]", start);
                if (end > start) {
                    String damage = str.substring(start, end).trim();
                    result.append(Component.literal("damage: ").withStyle(ChatFormatting.GRAY));
                    result.append(Component.literal(damage).withStyle(ChatFormatting.GOLD));
                }
            }

            if (str.contains("disableBlockingForSeconds=")) {
                if (!result.getString().isEmpty()) {
                    result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                }
                int start = str.indexOf("disableBlockingForSeconds=") + 26;
                int end = str.indexOf(",", start);
                if (end == -1) end = str.indexOf("]", start);
                if (end > start) {
                    String blocking = str.substring(start, end).trim();
                    result.append(Component.literal("blocking: ").withStyle(ChatFormatting.GRAY));
                    result.append(Component.literal(blocking + "s").withStyle(ChatFormatting.GOLD));
                }
            }

            return result.getString().isEmpty() ? null : java.util.Collections.singletonList(result);
        } catch (Exception ignored) {}
        return java.util.Collections.singletonList(Component.literal("weapon").withStyle(ChatFormatting.GRAY));
    }

    @Unique
    private Component formatSoundEvent(Holder<?> soundEntry) {
        try {
            Identifier soundId = soundEntry.unwrapKey().map(key -> key.identifier()).orElse(null);
            if (soundId != null) {
                return Component.literal(soundId.getPath()).withStyle(ChatFormatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Component.literal("sound").withStyle(ChatFormatting.AQUA);
    }

    @Unique
    private Component formatPotionContents(PotionContents potionContents) {
        MutableComponent result = Component.literal("");

        if (potionContents.potion().isPresent()) {
            Holder<Potion> potion = potionContents.potion().get();
            Identifier potionId = potion.unwrapKey().map(key -> key.identifier()).orElse(null);
            if (potionId != null) {
                result.append(Component.literal(potionId.getPath()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        List<MobEffectInstance> customEffects = potionContents.customEffects();
        if (customEffects != null && !customEffects.isEmpty()) {
            if (!result.getString().isEmpty()) {
                result.append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY));
            }

            boolean first = true;
            for (MobEffectInstance effect : customEffects) {
                if (!first) {
                    result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                }
                first = false;

                MobEffect mobEffect = effect.getEffect().value();
                Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(mobEffect);
                String effectName = effectId != null ? effectId.getPath() : "unknown";

                result.append(Component.literal(effectName).withStyle(ChatFormatting.LIGHT_PURPLE));
                result.append(Component.literal(" ").withStyle(ChatFormatting.WHITE));
                result.append(Component.literal(String.valueOf(effect.getAmplifier() + 1)).withStyle(ChatFormatting.GOLD));
                result.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY));
                result.append(Component.literal(effect.getDuration() / 20 + "s").withStyle(ChatFormatting.GOLD));
                result.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        return result.getString().isEmpty() ? null : result;
    }

    @Unique
    private Component formatInstrument(InstrumentComponent instrument) {
        try {
            var instrumentEntry = instrument.instrument();
            Identifier instrumentId = instrumentEntry.unwrapKey().map(key -> key.identifier()).orElse(null);
            if (instrumentId != null) {
                return Component.literal(instrumentId.getPath()).withStyle(ChatFormatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Component.literal("instrument").withStyle(ChatFormatting.AQUA);
    }

    @Unique
    private Component formatJukeboxPlayable(JukeboxPlayable jukebox) {
        try {
            var song = jukebox.song();
            Identifier songId = song.unwrapKey().map(key -> key.identifier()).orElse(null);
            if (songId != null) {
                return Component.literal(songId.getPath()).withStyle(ChatFormatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Component.literal("playable").withStyle(ChatFormatting.AQUA);
    }

    @Unique
    private Component formatFireworks(Fireworks fireworks) {
        int explosions = fireworks.explosions().size();
        if (explosions == 0) {
            return Component.literal("flight: " + fireworks.flightDuration()).withStyle(ChatFormatting.GOLD);
        }
        return Component.literal(explosions + " explosion" + (explosions > 1 ? "s" : "") +
                ", flight: " + fireworks.flightDuration()).withStyle(ChatFormatting.GOLD);
    }

    @Unique
    private Component formatUseCooldown(UseCooldown cooldown) {
        return Component.literal(String.valueOf(cooldown.seconds()) + "s").withStyle(ChatFormatting.GOLD);
    }

    @Unique
    private Component formatEquippable(Equippable equippable) {
        EquipmentSlot slot = equippable.slot();
        return Component.literal(slot.getName()).withStyle(ChatFormatting.YELLOW);
    }

    @Unique
    private Component formatTrimMaterial(Holder<TrimMaterial> trimMaterial) {
        try {
            Identifier trimId = trimMaterial.unwrapKey().map(key -> key.identifier()).orElse(null);
            if (trimId != null) {
                return Component.literal(trimId.getPath()).withStyle(ChatFormatting.LIGHT_PURPLE);
            }
        } catch (Exception ignored) {}
        return Component.literal("trim").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Unique
    private Component formatFood(FoodProperties food) {
        MutableComponent result = Component.literal("");
        result.append(Component.literal("nutrition: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(food.nutrition())).withStyle(ChatFormatting.GOLD));
        result.append(Component.literal(", saturation: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.format("%.1f", food.saturation())).withStyle(ChatFormatting.GOLD));
        if (food.canAlwaysEat()) {
            result.append(Component.literal(", always_eat").withStyle(ChatFormatting.GREEN));
        }
        return result;
    }

    @Unique
    private Component formatConsumable(Consumable consumable) {
        MutableComponent result = Component.literal("");
        result.append(Component.literal("seconds: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.format("%.1f", consumable.consumeSeconds())).withStyle(ChatFormatting.GOLD));
        return result;
    }

    @Unique
    private Component formatUseRemainder(UseRemainder remainder) {
        try {
            var template = remainder.convertInto();
            Item item = template.item().value();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            return Component.literal(itemId.getPath()).withStyle(ChatFormatting.AQUA);
        } catch (Exception ignored) {}
        return Component.literal("remainder").withStyle(ChatFormatting.GRAY);
    }

    @Unique
    private List<Component> formatSuspiciousStewEffects(SuspiciousStewEffects effects) {
        var effectsList = effects.effects();
        if (effectsList.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        for (var stewEffect : effectsList) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;

            Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(stewEffect.effect().value());
            String effectName = effectId != null ? effectId.getPath() : "unknown";

            result.append(Component.literal("[") );
            result.append(Component.literal(effectName).withStyle(ChatFormatting.LIGHT_PURPLE));
            result.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
            result.append(Component.literal(stewEffect.duration() / 20 + "s").withStyle(ChatFormatting.GOLD));
            result.append(Component.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private List<Component> formatBannerPatterns(BannerPatternLayers patterns) {
        var layers = patterns.layers();
        if (layers.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        for (var layer : layers) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;

            try {
                Identifier patternId = layer.pattern().unwrapKey().map(key -> key.identifier()).orElse(null);
                String patternName = patternId != null ? patternId.getPath() : "unknown";

                result.append(Component.literal("["));
                result.append(Component.literal(patternName).withStyle(ChatFormatting.AQUA));
                result.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
                result.append(Component.literal(layer.color().getSerializedName()).withStyle(ChatFormatting.GOLD));
                result.append(Component.literal("]"));
            } catch (Exception e) {
                result.append(Component.literal("[pattern]").withStyle(ChatFormatting.GRAY));
            }
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private List<Component> formatDeathProtection(DeathProtection deathProtection) {
        try {
            var effects = deathProtection.deathEffects();
            if (effects == null || effects.isEmpty()) {
                return java.util.Collections.singletonList(Component.literal("protection").withStyle(ChatFormatting.GREEN));
            }

            List<Component> resultList = new java.util.ArrayList<>();

            for (var effect : effects) {
                MutableComponent line = Component.literal("");

                try {
                    String className = effect.getClass().getSimpleName();

                    if (className.contains("ApplyStatusEffects")) {
                        line.append(Component.literal("status effects").withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else if (className.contains("Teleport")) {
                        line.append(Component.literal("teleport").withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else if (className.contains("PlaySound")) {
                        line.append(Component.literal("sound").withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else {
                        line.append(Component.literal(className.toLowerCase()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }

                    resultList.add(line);
                } catch (Exception ignored) {
                    resultList.add(Component.literal("effect").withStyle(ChatFormatting.GRAY));
                }
            }

            if (resultList.isEmpty()) {
                return java.util.Collections.singletonList(Component.literal("protection").withStyle(ChatFormatting.GREEN));
            }

            MutableComponent combined = Component.literal("");
            for (int i = 0; i < resultList.size(); i++) {
                if (i > 0) {
                    combined.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                }
                combined.append(resultList.get(i));
            }

            return java.util.Collections.singletonList(combined);
        } catch (Exception e) {
            return java.util.Collections.singletonList(Component.literal("protection").withStyle(ChatFormatting.GREEN));
        }
    }

    @Unique
    private Component formatIdentifier(Identifier id) {
        return Component.literal(id.getPath()).withStyle(ChatFormatting.AQUA);
    }

    @Unique
    private Component formatText(Component text) {
        return Component.literal(text.getString()).withStyle(ChatFormatting.GREEN);
    }

    @Unique
    private Component formatSwingAnimation(SwingAnimation animation) {
        MutableComponent result = Component.literal(animation.type().getSerializedName()).withStyle(ChatFormatting.YELLOW);
        if (animation.duration() > 0) {
            result.append(Component.literal(" (" + animation.duration() + " ticks)").withStyle(ChatFormatting.GOLD));
        }
        return result;
    }

    @Unique
    private Component formatUseEffects(UseEffects effects) {
        MutableComponent result = Component.literal("");
        result.append(Component.literal("sprint: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(effects.canSprint())).withStyle(ChatFormatting.GOLD));
        result.append(Component.literal(", vibrations: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(effects.interactVibrations())).withStyle(ChatFormatting.GOLD));
        result.append(Component.literal(", speed: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.format("%.1f", effects.speedMultiplier())).withStyle(ChatFormatting.GOLD));
        return result;
    }

    @Unique
    private List<Component> formatEntityData(TypedEntityData<?> entityData) {
        EntityType<?> entityType = (EntityType<?>) entityData.type();
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (typeId == null) return null;

        MutableComponent result = Component.literal(typeId.getPath()).withStyle(ChatFormatting.AQUA);
        result.append(Component.literal(" {").withStyle(ChatFormatting.WHITE));

        var tag = entityData.copyTagWithoutId();
        if (tag != null && !tag.isEmpty()) {
            result.append(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
        }

        result.append(Component.literal("}").withStyle(ChatFormatting.WHITE));
        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Component formatHolderId(Holder<?> holder) {
        Identifier id = holder.unwrapKey().map(key -> key.identifier()).orElse(null);
        if (id != null) {
            return Component.literal(id.getPath()).withStyle(ChatFormatting.AQUA);
        }
        return null;
    }

    @Unique
    private Component formatAttackRange(AttackRange range) {
        MutableComponent result = Component.literal("reach: ").withStyle(ChatFormatting.GRAY);
        result.append(Component.literal(String.format("%.1f-%.1f", range.minReach(), range.maxReach())).withStyle(ChatFormatting.GOLD));
        return result;
    }

    @Unique
    private Component formatKineticWeapon(KineticWeapon weapon) {
        MutableComponent result = Component.literal("");
        result.append(Component.literal("cooldown: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(weapon.contactCooldownTicks())).withStyle(ChatFormatting.GOLD));
        result.append(Component.literal(", delay: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(weapon.delayTicks())).withStyle(ChatFormatting.GOLD));
        result.append(Component.literal(", forward: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.format("%.1f", weapon.forwardMovement())).withStyle(ChatFormatting.GOLD));
        result.append(Component.literal(", damage: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.format("%.1fx", weapon.damageMultiplier())).withStyle(ChatFormatting.GOLD));
        return result;
    }

    @Unique
    private Component formatPiercingWeapon(PiercingWeapon weapon) {
        MutableComponent result = Component.literal("");
        result.append(Component.literal("knockback: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(weapon.dealsKnockback())).withStyle(ChatFormatting.GOLD));
        result.append(Component.literal(", dismount: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(weapon.dismounts())).withStyle(ChatFormatting.GOLD));
        return result;
    }

    @Unique
    private Component formatDye(DyeColor dye) {
        return Component.literal(dye.getName()).withStyle(style -> style.withColor(TextColor.fromRgb(dye.getTextColor())));
    }

    @Unique
    private Component formatComponentValue(Object value) {
        if (value == null) {
            return null;
        }

        String str = value.toString();

        if (str.isEmpty()) {
            return null;
        }

        if (str.length() > 50) {
            str = str.substring(0, 47) + "...";
        }

        MutableComponent result = Component.literal("");
        int i = 0;

        while (i < str.length()) {
            char c = str.charAt(i);

            if (c == '.') {
                int dotCount = 0;
                int start = i;
                while (i < str.length() && str.charAt(i) == '.') {
                    dotCount++;
                    i++;
                }
                if (dotCount >= 3) {
                    result.append(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    result.append(Component.literal(str.substring(start, i)).withStyle(ChatFormatting.WHITE));
                }
                continue;
            }

            if (Character.isDigit(c) || (c == '-' && i + 1 < str.length() && Character.isDigit(str.charAt(i + 1)))) {
                int start = i;
                if (c == '-') i++;
                while (i < str.length() && (Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
                    i++;
                }
                String number = str.substring(start, i);
                result.append(Component.literal(number).withStyle(ChatFormatting.GOLD));

                if (i < str.length() && "bdfLsB".indexOf(str.charAt(i)) != -1) {
                    result.append(Component.literal(String.valueOf(str.charAt(i))).withStyle(ChatFormatting.RED));
                    i++;
                }
            } else if (c == '"') {
                int start = i;
                i++;
                while (i < str.length() && str.charAt(i) != '"') {
                    if (str.charAt(i) == '\\' && i + 1 < str.length()) {
                        i += 2;
                    } else {
                        i++;
                    }
                }
                if (i < str.length()) i++;
                result.append(Component.literal(str.substring(start, i)).withStyle(ChatFormatting.GREEN));
            } else if (c == '{' || c == '}' || c == '[' || c == ']') {
                result.append(Component.literal(String.valueOf(c)).withStyle(ChatFormatting.YELLOW));
                i++;
            } else if (c == ':' || c == '=' || c == ',') {
                result.append(Component.literal(String.valueOf(c)).withStyle(ChatFormatting.DARK_GRAY));
                i++;
            } else {
                result.append(Component.literal(String.valueOf(c)).withStyle(ChatFormatting.WHITE));
                i++;
            }
        }

        return result;
    }

    @Unique
    private List<Component> formatChargedProjectiles(ChargedProjectiles projectiles) {
        List<ItemStack> projectilesList = projectiles.itemCopies();
        if (projectilesList.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        for (ItemStack projectile : projectilesList) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;

            Item item = projectile.getItem();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            int count = projectile.getCount();

            result.append(Component.literal("["));
            result.append(Component.literal(itemId.getPath()).withStyle(ChatFormatting.AQUA));
            if (count > 1) {
                result.append(Component.literal(" x").withStyle(ChatFormatting.WHITE));
                result.append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GOLD));
            }
            result.append(Component.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Component formatOminousBottleAmplifier(OminousBottleAmplifier amplifier) {
        int val = amplifier.value();
        if (val == 0) return null;
        return Component.literal(String.valueOf(val)).withStyle(ChatFormatting.GOLD);
    }

    @Unique
    private Component formatTrim(ArmorTrim trim) {
        try {
            MutableComponent result = Component.literal("");

            Identifier materialId = trim.material().unwrapKey().map(ResourceKey::identifier).orElse(null);
            String materialName = materialId != null ? materialId.getPath() : "unknown";

            Identifier patternId = trim.pattern().unwrapKey().map(ResourceKey::identifier).orElse(null);
            String patternName = patternId != null ? patternId.getPath() : "unknown";

            result.append(Component.literal(materialName).withStyle(ChatFormatting.AQUA));
            result.append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY));
            result.append(Component.literal(patternName).withStyle(ChatFormatting.LIGHT_PURPLE));

            return result;
        } catch (Exception ignored) {}
        return Component.literal("trim").withStyle(ChatFormatting.GRAY);
    }

    @Unique
    private List<Component> formatPotDecorations(PotDecorations decorations) {
        if (decorations.equals(PotDecorations.EMPTY)) {
            return null;
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        if (decorations.front().isPresent()) {
            if (!first) result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            first = false;

            Item item = decorations.front().get();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            result.append(Component.literal("front: ").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(itemId.getPath()).withStyle(ChatFormatting.AQUA));
        }

        if (decorations.left().isPresent()) {
            if (!first) result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            first = false;

            Item item = decorations.left().get();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            result.append(Component.literal("left: ").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(itemId.getPath()).withStyle(ChatFormatting.AQUA));
        }

        if (decorations.right().isPresent()) {
            if (!first) result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            first = false;

            Item item = decorations.right().get();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            result.append(Component.literal("right: ").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(itemId.getPath()).withStyle(ChatFormatting.AQUA));
        }

        if (decorations.back().isPresent()) {
            if (!first) result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            first = false;

            Item item = decorations.back().get();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            result.append(Component.literal("back: ").withStyle(ChatFormatting.GRAY));
            result.append(Component.literal(itemId.getPath()).withStyle(ChatFormatting.AQUA));
        }

        return result.getString().isEmpty() ? null : java.util.Collections.singletonList(result);
    }

    @Unique
    private Component formatBlockState(BlockItemStateProperties blockState) {
        var properties = blockState.properties();
        if (properties.isEmpty()) {
            return Component.literal("empty").withStyle(ChatFormatting.GRAY);
        }

        MutableComponent result = Component.literal("");
        boolean first = true;

        for (var entry : properties.entrySet()) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            first = false;

            result.append(Component.literal(entry.getKey()).withStyle(ChatFormatting.AQUA));
            result.append(Component.literal("=").withStyle(ChatFormatting.WHITE));
            result.append(Component.literal(entry.getValue()).withStyle(ChatFormatting.GOLD));
        }

        return result;
    }

    @Unique
    private List<Component> formatBees(Bees bees) {
        var beesList = bees.bees();
        if (beesList.isEmpty()) {
            return null;
        }

        MutableComponent result = Component.literal("");
        result.append(Component.literal("count: ").withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.valueOf(beesList.size())).withStyle(ChatFormatting.GOLD));

        return java.util.Collections.singletonList(result);
    }

}
