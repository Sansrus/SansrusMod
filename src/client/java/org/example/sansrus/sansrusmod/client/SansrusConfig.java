package org.example.sansrus.sansrusmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SansrusConfig {

    public boolean disableInvisibility  = true;
    public boolean deathWaypoint        = true;
    public boolean disableRmbCooldown   = true;
    public boolean itemTooltipComponents= true;
    public boolean bookPageHold         = true;
    public boolean shiftDragItems       = true;
    public boolean copyChatMessage      = true;
    public boolean autoRespawn          = true;
    public boolean cyrillicCommands     = true;
    public boolean deathLogbool         = true;
    public boolean tooltipmap           = true;
    public boolean matchingSlotHighlight= true;
    public boolean chatMessageCounter   = true;
    public int armorbarDisplay          = 5;
    public boolean coordparser          = true;
    public boolean protectVillage       = true;
    public boolean elytrafeaturerender  = true;



    public int bookScrollSpeed = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("sansrusmod.json");

    public static SansrusConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                SansrusConfig loaded = GSON.fromJson(reader, SansrusConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SansrusConfig defaults = new SansrusConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Screen createScreen(Screen parent) {

        // Проверка ника игрока
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isAllowedPlayer = false;
        if (client.player != null) {
            String playerName = client.player.getName().getString();
            isAllowedPlayer = playerName.equals("Sansrus") || playerName.equals("EN403");
        }
        
        // Проверка наличия Xaero's Minimap
        boolean hasXaero = SansrusModClient.isXaeroMinimapLoaded;

        // ═══════════════════════════════════════════════
        //  Создаём опции ОДИН РАЗ
        // ═══════════════════════════════════════════════

        Option<Boolean> optDisableInvisibility = bool(
                "sansrusmod.config.disableInvisibility",
                "sansrusmod.config.disableInvisibility.tooltip",
                true, () -> disableInvisibility, v -> disableInvisibility = v);

        Option<Boolean> optDeathWaypoint = bool(
                "sansrusmod.config.deathWaypoint",
                "sansrusmod.config.deathWaypoint.tooltip",
                true, () -> deathWaypoint, v -> deathWaypoint = v);

        Option<Boolean> optAutoRespawn = bool(
                "sansrusmod.config.autoRespawn",
                "sansrusmod.config.autoRespawn.tooltip",
                true, () -> autoRespawn, v -> autoRespawn = v);

        Option<Boolean> optDisableRmbCooldown = bool(
                "sansrusmod.config.disableRmbCooldown",
                "sansrusmod.config.disableRmbCooldown.tooltip",
                true, () -> disableRmbCooldown, v -> disableRmbCooldown = v);

        Option<Boolean> optItemTooltipComponents = bool(
                "sansrusmod.config.itemTooltipComponents",
                "sansrusmod.config.itemTooltipComponents.tooltip",
                true, () -> itemTooltipComponents, v -> itemTooltipComponents = v);

        Option<Boolean> optBookPageHold = bool(
                "sansrusmod.config.bookPageHold",
                "sansrusmod.config.bookPageHold.tooltip",
                true, () -> bookPageHold, v -> bookPageHold = v);

        Option<Integer> optBookScrollSpeed = Option.<Integer>createBuilder()
                .name(Text.translatable("sansrusmod.config.bookScrollSpeed"))
                .description(OptionDescription.of(Text.translatable("sansrusmod.config.bookScrollSpeed.tooltip")))
                .binding(10, () -> bookScrollSpeed, v -> bookScrollSpeed = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 40).step(1))
                .build();


        Option<Boolean> optShiftDragItems = bool(
                "sansrusmod.config.shiftDragItems",
                "sansrusmod.config.shiftDragItems.tooltip",
                true, () -> shiftDragItems, v -> shiftDragItems = v);

        Option<Boolean> optCopyChatMessage = bool(
                "sansrusmod.config.copyChatMessage",
                "sansrusmod.config.copyChatMessage.tooltip",
                true, () -> copyChatMessage, v -> copyChatMessage = v);

        Option<Boolean> optCyrillicCommands = bool(
                "sansrusmod.config.cyrillicCommands",
                "sansrusmod.config.cyrillicCommands.tooltip",
                true, () -> cyrillicCommands, v -> cyrillicCommands = v);
        Option<Boolean> optDeathLog = bool(
                "sansrusmod.config.deathLog",
                "sansrusmod.config.deathLog.tooltip",
                true, () -> deathLogbool, v -> deathLogbool = v);
        Option<Boolean> optToolTipMap = bool(
                "sansrusmod.config.tooltipMap",
                "sansrusmod.config.tooltipMap.tooltip",
                true, () -> tooltipmap, v -> tooltipmap = v);
        Option<Boolean> optHightLight = bool(
                "sansrusmod.config.matchingSlotHighlight",
                "sansrusmod.config.matchingSlotHighlight.tooltip",
                true, () -> matchingSlotHighlight, v -> matchingSlotHighlight = v);
        Option<Boolean> optChatMessage = bool(
                "sansrusmod.config.chatMessageCounter",
                "sansrusmod.config.chatMessageCounter.tooltip",
                true, () -> chatMessageCounter, v -> chatMessageCounter = v);
        
        Option<Integer> optArmorBarDisplay = Option.<Integer>createBuilder()
                .name(Text.translatable("sansrusmod.config.armorbarDisplay"))
                .description(OptionDescription.of(Text.translatable("sansrusmod.config.armorbarDisplay.tooltip")))
                .binding(21, () -> armorbarDisplay, v -> armorbarDisplay = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(0, 21)
                        .step(1)
                        .formatValue(val -> {
                            if (val == 0) return Text.translatable("sansrusmod.config.armorbarDisplay.off");
                            if (val == 21) return Text.translatable("sansrusmod.config.armorbarDisplay.always");
                            return Text.translatable("sansrusmod.config.armorbarDisplay.seconds", val);
                        }))
                .build();
        
        Option<Boolean> optCoordParsesr = bool(
                "sansrusmod.config.coordParser",
                "sansrusmod.config.coordParser.tooltip",
                true, () -> coordparser, v -> coordparser = v);
        Option<Boolean> optProtectVillage = bool(
                "sansrusmod.config.protectVillage",
                "sansrusmod.config.protectVillage.tooltip",
                true, () -> protectVillage, v -> protectVillage = v);
        Option<Boolean> optElytraFeatures = bool(
                "sansrusmod.config.elytraFeature",
                "sansrusmod.config.elytraFeature.tooltip",
                true, () -> elytrafeaturerender, v -> elytrafeaturerender = v);

        // ═══════════════════════════════════════════════
        //  Строим экран, переиспользуя те же объекты
        // ═══════════════════════════════════════════════

        ConfigCategory.Builder mainCategoryBuilder = ConfigCategory.createBuilder()
                .name(Text.translatable("sansrusmod.config.category.main"))
                .tooltip(Text.translatable("sansrusmod.config.category.main.tooltip"));

        // Добавляем опции только для разрешенных игроков
        if (isAllowedPlayer) {
            mainCategoryBuilder.option(optDisableInvisibility);
        }
        if (hasXaero) {
            mainCategoryBuilder.option(optDeathWaypoint);
        }
        if (isAllowedPlayer) {
            mainCategoryBuilder.option(optAutoRespawn);
        }
        mainCategoryBuilder
                .option(optDisableRmbCooldown)
                .option(optItemTooltipComponents)
                .option(optBookPageHold)
                .option(optBookScrollSpeed)
                .option(optShiftDragItems)
                .option(optCopyChatMessage)
                .option(optCyrillicCommands)
                .option(optDeathLog)
                .option(optToolTipMap)
                .option(optHightLight)
                .option(optChatMessage)
                .option(optArmorBarDisplay);
        if (hasXaero) {
            mainCategoryBuilder.option(optCoordParsesr);
        }
        mainCategoryBuilder
                .option(optProtectVillage)
                .option(optElytraFeatures);

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("sansrusmod.config.title"))

                // ─── Основное ────────────────────────────────────────────────
                .category(mainCategoryBuilder.build())

                .save(this::save)
                .build()
                .generateScreen(parent);
    }


    // ═══════════════════════════════════════════════
    //  УТИЛИТА
    // ═══════════════════════════════════════════════

    private static Option<Boolean> bool(
            String name, String desc, boolean def,
            Supplier<Boolean> getter, Consumer<Boolean> setter
    ) {
        return Option.<Boolean>createBuilder()
                .name(Text.translatable(name))
                .description(OptionDescription.of(Text.translatable(desc)))
                .binding(def, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }
}
