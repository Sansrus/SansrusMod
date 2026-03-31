package org.example.sansrus.sansrusmod.client.mixin;

import net.minecraft.block.entity.Sherds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
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
    public abstract ComponentMap getComponents();

    @Shadow
    public abstract Item getItem();

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void onGetTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        if (!SansrusModClient.config.itemTooltipComponents) return;
        long window = MinecraftClient.getInstance().getWindow().getHandle();
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

        List<Text> tooltip = cir.getReturnValue();
        ComponentMap components = this.getComponents();

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("sansrusmod.tooltip.components").formatted(Formatting.GRAY));

        components.forEach(entry -> {
            ComponentType<?> componentType = entry.type();
            Object value = entry.value();

            Identifier id = Registries.DATA_COMPONENT_TYPE.getId(componentType);
            String shortName = id != null ? id.getPath() : componentType.toString();

            List<Text> formattedValues = formatSpecificComponent(componentType, value);

            if (formattedValues == null || formattedValues.isEmpty()) {
                return;
            }

            MutableText firstLine = Text.literal("  ");
            firstLine.append(Text.literal(shortName).formatted(Formatting.BLUE));
            firstLine.append(Text.literal(": ").formatted(Formatting.WHITE));
            firstLine.append(formattedValues.get(0));
            tooltip.add(firstLine);

            for (int i = 1; i < formattedValues.size(); i++) {
                MutableText line = Text.literal("    ");
                line.append(formattedValues.get(i));
                tooltip.add(line);
            }
        });
    }

    @Unique
    private List<Text> formatSpecificComponent(ComponentType<?> type, Object value) {
        // Игнорируем эти компоненты
        if (type == DataComponentTypes.TOOLTIP_STYLE ||
                type == DataComponentTypes.CONTAINER ||
                type == DataComponentTypes.BUNDLE_CONTENTS ||
                type == DataComponentTypes.TOOLTIP_DISPLAY ||
                type == DataComponentTypes.WRITTEN_BOOK_CONTENT ||
                type == DataComponentTypes.WRITABLE_BOOK_CONTENT ||
                type == DataComponentTypes.BLOCKS_ATTACKS ||
                type == DataComponentTypes.PROVIDES_BANNER_PATTERNS ||
                type == DataComponentTypes.CHICKEN_VARIANT) {
            return null;
        }

        Text result = null;

        if (type == DataComponentTypes.ENCHANTMENTS || type == DataComponentTypes.STORED_ENCHANTMENTS) {
            return formatEnchantments((ItemEnchantmentsComponent) value);
        } else if (type == DataComponentTypes.LORE) {
            result = formatLore((LoreComponent) value);
        } else if (type == DataComponentTypes.CUSTOM_NAME || type == DataComponentTypes.ITEM_NAME) {
            result = formatText((Text) value);
        } else if (type == DataComponentTypes.DAMAGE || type == DataComponentTypes.MAX_STACK_SIZE ||
                type == DataComponentTypes.REPAIR_COST || type == DataComponentTypes.MAX_DAMAGE) {
            result = Text.literal(String.valueOf(value)).formatted(Formatting.GOLD);
        } else if (type == DataComponentTypes.RARITY) {
            result = Text.literal(String.valueOf(value)).formatted(Formatting.YELLOW);
        } else if (type == DataComponentTypes.ATTRIBUTE_MODIFIERS) {
            return formatAttributeModifiers((AttributeModifiersComponent) value);
        } else if (type == DataComponentTypes.ENCHANTABLE) {
            result = formatEnchantable((EnchantableComponent) value);
        } else if (type == DataComponentTypes.REPAIRABLE) {
            result = formatRepairable((RepairableComponent) value);
        } else if (type == DataComponentTypes.TOOL) {
            return formatTool((ToolComponent) value);
        } else if (type == DataComponentTypes.DAMAGE_RESISTANT) {
            result = formatDamageResistant((DamageResistantComponent) value);
        } else if (type == DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) {
            result = Text.literal(String.valueOf(value)).formatted(Formatting.LIGHT_PURPLE);
        } else if (type == DataComponentTypes.INTANGIBLE_PROJECTILE ||
                type == DataComponentTypes.GLIDER) {
            result = value instanceof Unit ? Text.literal("true").formatted(Formatting.GREEN) :
                    Text.literal(String.valueOf(value)).formatted(Formatting.GREEN);
        } else if (type == DataComponentTypes.ITEM_MODEL) {
            result = formatIdentifier((Identifier) value);
        } else if (type == DataComponentTypes.WEAPON) {
            return formatWeapon((WeaponComponent) value);
        } else if (type == DataComponentTypes.BREAK_SOUND) {
            result = formatSoundEvent((RegistryEntry<?>) value);
        } else if (type == DataComponentTypes.POTION_CONTENTS) {
            result = formatPotionContents((PotionContentsComponent) value);
        } else if (type == DataComponentTypes.INSTRUMENT) {
            result = formatInstrument((InstrumentComponent) value);
        } else if (type == DataComponentTypes.JUKEBOX_PLAYABLE) {
            result = formatJukeboxPlayable((JukeboxPlayableComponent) value);
        } else if (type == DataComponentTypes.FIREWORKS) {
            result = formatFireworks((FireworksComponent) value);
        } else if (type == DataComponentTypes.USE_COOLDOWN) {
            result = formatUseCooldown((UseCooldownComponent) value);
        } else if (type == DataComponentTypes.EQUIPPABLE) {
            result = formatEquippable((EquippableComponent) value);
        } else if (type == DataComponentTypes.PROVIDES_TRIM_MATERIAL) {
            result = formatTrimMaterial((ProvidesTrimMaterialComponent) value);
        } else if (type == DataComponentTypes.FOOD) {
            result = formatFood((FoodComponent) value);
        } else if (type == DataComponentTypes.CONSUMABLE) {
            result = formatConsumable((ConsumableComponent) value);
        } else if (type == DataComponentTypes.USE_REMAINDER) {
            result = formatUseRemainder((UseRemainderComponent) value);
        } else if (type == DataComponentTypes.SUSPICIOUS_STEW_EFFECTS) {
            return formatSuspiciousStewEffects((SuspiciousStewEffectsComponent) value);
        } else if (type == DataComponentTypes.BANNER_PATTERNS) {
            return formatBannerPatterns((BannerPatternsComponent) value);
        } else if (type == DataComponentTypes.DEATH_PROTECTION) {
            return formatDeathProtection((DeathProtectionComponent) value);
        } else if (type == DataComponentTypes.CHARGED_PROJECTILES) {
            return formatChargedProjectiles((ChargedProjectilesComponent) value);
        } else if (type == DataComponentTypes.POT_DECORATIONS) {
            return formatPotDecorations((Sherds) value);
        } else if (type == DataComponentTypes.BLOCK_STATE) {
            result = formatBlockState((BlockStateComponent) value);
        } else if (type == DataComponentTypes.BEES) {
            return formatBees((BeesComponent) value);
        } else if (type == DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER) {
            result = formatOminousBottleAmplifier((OminousBottleAmplifierComponent) value);
        } else if (type == DataComponentTypes.TRIM) {
            result = formatTrim((ArmorTrim) value);
        } else {
            result = formatComponentValue(value);
        }

        return result != null ? java.util.Collections.singletonList(result) : null;
    }

    @Unique
    private List<Text> formatEnchantments(ItemEnchantmentsComponent enchantments) {
        if (enchantments.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");
        boolean first = true;

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (!first) {
                result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            }
            first = false;

            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();

            Identifier enchId = enchantment.getKey().map(key -> key.getValue()).orElse(null);
            String enchName = enchId != null ? enchId.getPath() : "unknown";

            result.append(Text.literal("["));
            result.append(Text.literal(enchName).formatted(Formatting.AQUA));
            result.append(Text.literal(": ").formatted(Formatting.WHITE));
            result.append(Text.literal(String.valueOf(level)).formatted(Formatting.GOLD));
            result.append(Text.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Text formatLore(LoreComponent lore) {
        List<Text> lines = lore.lines();
        if (lines.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
            }
            Text line = lines.get(i);
            result.append(Text.literal(line.getString()).formatted(Formatting.GREEN));
        }

        return result;
    }

    @Unique
    private List<Text> formatAttributeModifiers(AttributeModifiersComponent modifiers) {
        List<AttributeModifiersComponent.Entry> modifiersList = modifiers.modifiers();
        if (modifiersList.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");
        boolean first = true;

        for (AttributeModifiersComponent.Entry entry : modifiersList) {
            if (!first) {
                result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            }
            first = false;

            RegistryEntry<EntityAttribute> attribute = entry.attribute();
            EntityAttributeModifier modifier = entry.modifier();

            Identifier attrId = attribute.getKey().map(key -> key.getValue()).orElse(null);
            String attrName = attrId != null ? attrId.getPath() : "unknown";

            double value = modifier.value();
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

            result.append(Text.literal("["));
            result.append(Text.literal(attrName).formatted(Formatting.AQUA));
            result.append(Text.literal(": ").formatted(Formatting.WHITE));
            result.append(Text.literal(formattedValue).formatted(Formatting.GOLD));
            result.append(Text.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Text formatEnchantable(EnchantableComponent enchantable) {
        return Text.literal(String.valueOf(enchantable.value())).formatted(Formatting.GOLD);
    }

    @Unique
    private Text formatRepairable(RepairableComponent repairable) {
        try {
            var items = repairable.items();
            var tagKey = items.getTagKey();
            if (tagKey.isPresent()) {
                Identifier id = tagKey.get().id();
                return Text.literal(id.getPath()).formatted(Formatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Text.literal("unknown").formatted(Formatting.GRAY);
    }

    @Unique
    private List<Text> formatTool(ToolComponent tool) {
        List<ToolComponent.Rule> rules = tool.rules();
        if (rules.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");
        boolean first = true;

        for (ToolComponent.Rule rule : rules) {
            if (!first) {
                result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            }
            first = false;

            result.append(Text.literal("["));

            String blocks = rule.blocks().toString();
            if (blocks.contains("Tag{key=")) {
                int start = blocks.indexOf("key=") + 4;
                int end = blocks.indexOf("}", start);
                if (end > start) {
                    String tagKey = blocks.substring(start, end);
                    Identifier id = Identifier.tryParse(tagKey);
                    result.append(Text.literal(id != null ? id.getPath() : tagKey).formatted(Formatting.AQUA));
                }
            } else {
                result.append(Text.literal("blocks").formatted(Formatting.AQUA));
            }

            result.append(Text.literal(": ").formatted(Formatting.WHITE));

            if (rule.speed().isPresent()) {
                result.append(Text.literal("speed " + String.format("%.1f", rule.speed().get())).formatted(Formatting.GOLD));
            }
            if (rule.correctForDrops().isPresent()) {
                if (rule.speed().isPresent()) {
                    result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                }
                result.append(Text.literal("drops " + rule.correctForDrops().get()).formatted(Formatting.GOLD));
            }

            result.append(Text.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Text formatDamageResistant(DamageResistantComponent resistant) {
        try {
            var types = resistant.types();
            if (types instanceof TagKey) {
                TagKey<?> tagKey = (TagKey<?>) types;
                Identifier id = tagKey.id();
                return Text.literal(id.getPath()).formatted(Formatting.RED);
            }
        } catch (Exception ignored) {}
        return Text.literal("unknown").formatted(Formatting.GRAY);
    }

    @Unique
    private List<Text> formatWeapon(WeaponComponent weapon) {
        try {
            String str = weapon.toString();

            MutableText result = Text.literal("");

            if (str.contains("itemDamagePerAttack=")) {
                int start = str.indexOf("itemDamagePerAttack=") + 20;
                int end = str.indexOf(",", start);
                if (end == -1) end = str.indexOf("]", start);
                if (end > start) {
                    String damage = str.substring(start, end).trim();
                    result.append(Text.literal("damage: ").formatted(Formatting.GRAY));
                    result.append(Text.literal(damage).formatted(Formatting.GOLD));
                }
            }

            if (str.contains("disableBlockingForSeconds=")) {
                if (!result.getString().isEmpty()) {
                    result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                }
                int start = str.indexOf("disableBlockingForSeconds=") + 26;
                int end = str.indexOf(",", start);
                if (end == -1) end = str.indexOf("]", start);
                if (end > start) {
                    String blocking = str.substring(start, end).trim();
                    result.append(Text.literal("blocking: ").formatted(Formatting.GRAY));
                    result.append(Text.literal(blocking + "s").formatted(Formatting.GOLD));
                }
            }

            return result.getString().isEmpty() ? null : java.util.Collections.singletonList(result);
        } catch (Exception ignored) {}
        return java.util.Collections.singletonList(Text.literal("weapon").formatted(Formatting.GRAY));
    }

    @Unique
    private Text formatSoundEvent(RegistryEntry<?> soundEntry) {
        try {
            Identifier soundId = soundEntry.getKey().map(key -> key.getValue()).orElse(null);
            if (soundId != null) {
                return Text.literal(soundId.getPath()).formatted(Formatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Text.literal("sound").formatted(Formatting.AQUA);
    }

    @Unique
    private Text formatPotionContents(PotionContentsComponent potionContents) {
        MutableText result = Text.literal("");

        if (potionContents.potion().isPresent()) {
            RegistryEntry<Potion> potion = potionContents.potion().get();
            Identifier potionId = potion.getKey().map(key -> key.getValue()).orElse(null);
            if (potionId != null) {
                result.append(Text.literal(potionId.getPath()).formatted(Formatting.LIGHT_PURPLE));
            }
        }

        if (!potionContents.customEffects().isEmpty()) {
            if (!result.getString().isEmpty()) {
                result.append(Text.literal(" + ").formatted(Formatting.DARK_GRAY));
            }

            boolean first = true;
            for (StatusEffectInstance effect : potionContents.customEffects()) {
                if (!first) {
                    result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                }
                first = false;

                Identifier effectId = Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
                String effectName = effectId != null ? effectId.getPath() : "unknown";

                result.append(Text.literal(effectName).formatted(Formatting.LIGHT_PURPLE));
                result.append(Text.literal(" ").formatted(Formatting.WHITE));
                result.append(Text.literal(String.valueOf(effect.getAmplifier() + 1)).formatted(Formatting.GOLD));
                result.append(Text.literal(" (").formatted(Formatting.DARK_GRAY));
                result.append(Text.literal(effect.getDuration() / 20 + "s").formatted(Formatting.GOLD));
                result.append(Text.literal(")").formatted(Formatting.DARK_GRAY));
            }
        }

        return result.getString().isEmpty() ? null : result;
    }

    @Unique
    private Text formatInstrument(InstrumentComponent instrument) {
        try {
            var instrumentEntry = instrument.instrument();
            Identifier instrumentId = instrumentEntry.getKey().map(key -> key.getValue()).orElse(null);
            if (instrumentId != null) {
                return Text.literal(instrumentId.getPath()).formatted(Formatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Text.literal("instrument").formatted(Formatting.AQUA);
    }


    @Unique
    private Text formatJukeboxPlayable(JukeboxPlayableComponent jukebox) {
        try {
            var song = jukebox.song();
            Identifier songId = song.getKey().map(key -> key.getValue()).orElse(null);
            if (songId != null) {
                return Text.literal(songId.getPath()).formatted(Formatting.AQUA);
            }
        } catch (Exception ignored) {}
        return Text.literal("playable").formatted(Formatting.AQUA);
    }

    @Unique
    private Text formatFireworks(FireworksComponent fireworks) {
        int explosions = fireworks.explosions().size();
        if (explosions == 0) {
            return Text.literal("flight: " + fireworks.flightDuration()).formatted(Formatting.GOLD);
        }
        return Text.literal(explosions + " explosion" + (explosions > 1 ? "s" : "") +
                ", flight: " + fireworks.flightDuration()).formatted(Formatting.GOLD);
    }

    @Unique
    private Text formatUseCooldown(UseCooldownComponent cooldown) {
        return Text.literal(String.valueOf(cooldown.seconds()) + "s").formatted(Formatting.GOLD);
    }

    @Unique
    private Text formatEquippable(EquippableComponent equippable) {
        String slot = equippable.slot().getName();
        return Text.literal(slot).formatted(Formatting.YELLOW);
    }

    @Unique
    private Text formatTrimMaterial(ProvidesTrimMaterialComponent trimMaterial) {
        try {
            Identifier trimId = trimMaterial.material().getKey().map(key -> key.getValue()).orElse(null);
            if (trimId != null) {
                return Text.literal(trimId.getPath()).formatted(Formatting.LIGHT_PURPLE);
            }
        } catch (Exception ignored) {}
        return Text.literal("trim").formatted(Formatting.LIGHT_PURPLE);
    }

    @Unique
    private Text formatFood(FoodComponent food) {
        MutableText result = Text.literal("");
        result.append(Text.literal("nutrition: ").formatted(Formatting.GRAY));
        result.append(Text.literal(String.valueOf(food.nutrition())).formatted(Formatting.GOLD));
        result.append(Text.literal(", saturation: ").formatted(Formatting.GRAY));
        result.append(Text.literal(String.format("%.1f", food.saturation())).formatted(Formatting.GOLD));
        if (food.canAlwaysEat()) {
            result.append(Text.literal(", always_eat").formatted(Formatting.GREEN));
        }
        return result;
    }

    @Unique
    private Text formatConsumable(ConsumableComponent consumable) {
        MutableText result = Text.literal("");
        result.append(Text.literal("seconds: ").formatted(Formatting.GRAY));
        result.append(Text.literal(String.format("%.1f", consumable.consumeSeconds())).formatted(Formatting.GOLD));
        return result;
    }

    @Unique
    private Text formatUseRemainder(UseRemainderComponent remainder) {
        try {
            ItemStack stack = remainder.convertInto();
            Item item = stack.getItem();
            Identifier itemId = Registries.ITEM.getId(item);
            return Text.literal(itemId.getPath()).formatted(Formatting.AQUA);
        } catch (Exception ignored) {}
        return Text.literal("remainder").formatted(Formatting.GRAY);
    }

    @Unique
    private List<Text> formatSuspiciousStewEffects(SuspiciousStewEffectsComponent effects) {
        List<SuspiciousStewEffectsComponent.StewEffect> effectsList = effects.effects();
        if (effectsList.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");
        boolean first = true;

        for (SuspiciousStewEffectsComponent.StewEffect stewEffect : effectsList) {
            if (!first) {
                result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            }
            first = false;

            Identifier effectId = Registries.STATUS_EFFECT.getId(stewEffect.effect().value());
            String effectName = effectId != null ? effectId.getPath() : "unknown";

            result.append(Text.literal("["));
            result.append(Text.literal(effectName).formatted(Formatting.LIGHT_PURPLE));
            result.append(Text.literal(": ").formatted(Formatting.WHITE));
            result.append(Text.literal(stewEffect.duration() / 20 + "s").formatted(Formatting.GOLD));
            result.append(Text.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private List<Text> formatBannerPatterns(BannerPatternsComponent patterns) {
        List<BannerPatternsComponent.Layer> layers = patterns.layers();
        if (layers.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");
        boolean first = true;

        for (BannerPatternsComponent.Layer layer : layers) {
            if (!first) {
                result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            }
            first = false;

            try {
                Identifier patternId = layer.pattern().getKey().map(key -> key.getValue()).orElse(null);
                String patternName = patternId != null ? patternId.getPath() : "unknown";

                result.append(Text.literal("["));
                result.append(Text.literal(patternName).formatted(Formatting.AQUA));
                result.append(Text.literal(": ").formatted(Formatting.WHITE));
                result.append(Text.literal(layer.color().asString()).formatted(Formatting.GOLD));
                result.append(Text.literal("]"));
            } catch (Exception e) {
                result.append(Text.literal("[pattern]").formatted(Formatting.GRAY));
            }
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private List<Text> formatDeathProtection(DeathProtectionComponent deathProtection) {
        try {
            var effects = deathProtection.deathEffects();
            if (effects == null || effects.isEmpty()) {
                return java.util.Collections.singletonList(Text.literal("protection").formatted(Formatting.GREEN));
            }

            List<Text> resultList = new java.util.ArrayList<>();

            for (var effect : effects) {
                MutableText line = Text.literal("");

                try {
                    // Проверяем тип эффекта
                    String className = effect.getClass().getSimpleName();

                    if (className.contains("ApplyStatusEffects")) {
                        // Пытаемся получить эффекты через рефлексию или toString
                        String str = effect.toString();

                        if (str.contains("effects=[") || str.contains("effects={")) {
                            line.append(Text.literal("status effects").formatted(Formatting.LIGHT_PURPLE));
                        } else {
                            line.append(Text.literal("apply effects").formatted(Formatting.LIGHT_PURPLE));
                        }
                    } else if (className.contains("Teleport")) {
                        line.append(Text.literal("teleport").formatted(Formatting.LIGHT_PURPLE));
                    } else if (className.contains("PlaySound")) {
                        line.append(Text.literal("sound").formatted(Formatting.LIGHT_PURPLE));
                    } else {
                        line.append(Text.literal(className.toLowerCase()).formatted(Formatting.LIGHT_PURPLE));
                    }

                    resultList.add(line);
                } catch (Exception ignored) {
                    resultList.add(Text.literal("effect").formatted(Formatting.GRAY));
                }
            }

            // Объединяем все в одну строку
            if (resultList.isEmpty()) {
                return java.util.Collections.singletonList(Text.literal("protection").formatted(Formatting.GREEN));
            }

            MutableText combined = Text.literal("");
            for (int i = 0; i < resultList.size(); i++) {
                if (i > 0) {
                    combined.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                }
                combined.append(resultList.get(i));
            }

            return java.util.Collections.singletonList(combined);
        } catch (Exception e) {
            return java.util.Collections.singletonList(Text.literal("protection").formatted(Formatting.GREEN));
        }
    }




    @Unique
    private Text formatIdentifier(Identifier id) {
        return Text.literal(id.getPath()).formatted(Formatting.AQUA);
    }

    @Unique
    private Text formatText(Text text) {
        return Text.literal(text.getString()).formatted(Formatting.GREEN);
    }

    @Unique
    private Text formatComponentValue(Object value) {
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

        MutableText result = Text.literal("");
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
                    result.append(Text.literal("...").formatted(Formatting.DARK_GRAY));
                } else {
                    result.append(Text.literal(str.substring(start, i)).formatted(Formatting.WHITE));
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
                result.append(Text.literal(number).formatted(Formatting.GOLD));

                if (i < str.length() && "bdfLsB".indexOf(str.charAt(i)) != -1) {
                    result.append(Text.literal(String.valueOf(str.charAt(i))).formatted(Formatting.RED));
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
                result.append(Text.literal(str.substring(start, i)).formatted(Formatting.GREEN));
            } else if (c == '{' || c == '}' || c == '[' || c == ']') {
                result.append(Text.literal(String.valueOf(c)).formatted(Formatting.YELLOW));
                i++;
            } else if (c == ':' || c == '=' || c == ',') {
                result.append(Text.literal(String.valueOf(c)).formatted(Formatting.DARK_GRAY));
                i++;
            } else {
                result.append(Text.literal(String.valueOf(c)).formatted(Formatting.WHITE));
                i++;
            }
        }

        return result;
    }

    @Unique
    private List<Text> formatChargedProjectiles(ChargedProjectilesComponent projectiles) {
        List<ItemStack> projectilesList = projectiles.getProjectiles();
        if (projectilesList.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");
        boolean first = true;

        for (ItemStack projectile : projectilesList) {
            if (!first) {
                result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            }
            first = false;

            Item item = projectile.getItem();
            Identifier itemId = Registries.ITEM.getId(item);
            int count = projectile.getCount();

            result.append(Text.literal("["));
            result.append(Text.literal(itemId.getPath()).formatted(Formatting.AQUA));
            if (count > 1) {
                result.append(Text.literal(" x").formatted(Formatting.WHITE));
                result.append(Text.literal(String.valueOf(count)).formatted(Formatting.GOLD));
            }
            result.append(Text.literal("]"));
        }

        return java.util.Collections.singletonList(result);
    }

    @Unique
    private Text formatOminousBottleAmplifier(OminousBottleAmplifierComponent amplifier) {
        return Text.literal(String.valueOf(amplifier.value())).formatted(Formatting.GOLD);
    }


    @Unique
    private Text formatTrim(ArmorTrim trim) {
        try {
            MutableText result = Text.literal("");

            Identifier materialId = trim.material().getKey().map(RegistryKey::getValue).orElse(null);
            String materialName = materialId != null ? materialId.getPath() : "unknown";

            Identifier patternId = trim.pattern().getKey().map(RegistryKey::getValue).orElse(null);
            String patternName = patternId != null ? patternId.getPath() : "unknown";

            result.append(Text.literal(materialName).formatted(Formatting.AQUA));
            result.append(Text.literal(" + ").formatted(Formatting.DARK_GRAY));
            result.append(Text.literal(patternName).formatted(Formatting.LIGHT_PURPLE));

            return result;
        } catch (Exception ignored) {}
        return Text.literal("trim").formatted(Formatting.GRAY);
    }

    @Unique
    private List<Text> formatPotDecorations(Sherds decorations) {
        if (decorations.equals(Sherds.DEFAULT)) {
            return null;
        }

        MutableText result = Text.literal("");
        boolean first = true;

        // Проверяем каждую сторону
        if (decorations.front().isPresent()) {
            if (!first) result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            first = false;

            Item item = decorations.front().get();
            Identifier itemId = Registries.ITEM.getId(item);
            result.append(Text.literal("front: ").formatted(Formatting.GRAY));
            result.append(Text.literal(itemId.getPath()).formatted(Formatting.AQUA));
        }

        if (decorations.left().isPresent()) {
            if (!first) result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            first = false;

            Item item = decorations.left().get();
            Identifier itemId = Registries.ITEM.getId(item);
            result.append(Text.literal("left: ").formatted(Formatting.GRAY));
            result.append(Text.literal(itemId.getPath()).formatted(Formatting.AQUA));
        }

        if (decorations.right().isPresent()) {
            if (!first) result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            first = false;

            Item item = decorations.right().get();
            Identifier itemId = Registries.ITEM.getId(item);
            result.append(Text.literal("right: ").formatted(Formatting.GRAY));
            result.append(Text.literal(itemId.getPath()).formatted(Formatting.AQUA));
        }

        if (decorations.back().isPresent()) {
            if (!first) result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            first = false;

            Item item = decorations.back().get();
            Identifier itemId = Registries.ITEM.getId(item);
            result.append(Text.literal("back: ").formatted(Formatting.GRAY));
            result.append(Text.literal(itemId.getPath()).formatted(Formatting.AQUA));
        }

        return result.getString().isEmpty() ? null : java.util.Collections.singletonList(result);
    }

    @Unique
    private Text formatBlockState(BlockStateComponent blockState) {
        var properties = blockState.properties();
        if (properties.isEmpty()) {
            return Text.literal("empty").formatted(Formatting.GRAY);
        }

        MutableText result = Text.literal("");
        boolean first = true;

        for (var entry : properties.entrySet()) {
            if (!first) {
                result.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
            }
            first = false;

            result.append(Text.literal(entry.getKey()).formatted(Formatting.AQUA));
            result.append(Text.literal("=").formatted(Formatting.WHITE));
            result.append(Text.literal(entry.getValue()).formatted(Formatting.GOLD));
        }

        return result;
    }

    @Unique
    private List<Text> formatBees(BeesComponent bees) {
        var beesList = bees.bees();
        if (beesList.isEmpty()) {
            return null;
        }

        MutableText result = Text.literal("");
        result.append(Text.literal("count: ").formatted(Formatting.GRAY));
        result.append(Text.literal(String.valueOf(beesList.size())).formatted(Formatting.GOLD));

        return java.util.Collections.singletonList(result);
    }

}
