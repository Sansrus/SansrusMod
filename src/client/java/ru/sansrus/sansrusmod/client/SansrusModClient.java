package ru.sansrus.sansrusmod.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.Level;
import ru.sansrus.sansrusmod.client.chatcoord.ChatPipelineFlags;
import ru.sansrus.sansrusmod.client.deathlog.DeathHistoryManager;
import ru.sansrus.sansrusmod.client.maptip.MapPreviewTooltipComponent;
import ru.sansrus.sansrusmod.client.maptip.MapTooltipData;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.path.XaeroPath;

public class SansrusModClient implements ClientModInitializer {
    public static SansrusConfig config;
    public static boolean isXaeroMinimapLoaded = false;

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "false");
        config = SansrusConfig.load();

        isXaeroMinimapLoaded = FabricLoader.getInstance().isModLoaded("xaerominimap");


        ClientTooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof MapTooltipData mapData) {
                return new MapPreviewTooltipComponent(mapData);
            }
            return null;
        });


        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            DeathHistoryManager.loadForServer();
        });


        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("create_death_waypoint")
                    .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                            .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                                    .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                            .then(ClientCommands.argument("dim", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        int x = IntegerArgumentType.getInteger(context, "x");
                                                        int y = IntegerArgumentType.getInteger(context, "y");
                                                        int z = IntegerArgumentType.getInteger(context, "z");
                                                        String dim = StringArgumentType.getString(context, "dim");
                                                        createDeathWaypoint(x, y, z, dim);
                                                        return 1;
                                                    }))))));
            dispatcher.register(ClientCommands.literal("create_coord_waypoint")
                    .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                            .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                                    .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                            .executes(context -> {
                                                int x = IntegerArgumentType.getInteger(context, "x");
                                                int y = IntegerArgumentType.getInteger(context, "y");
                                                int z = IntegerArgumentType.getInteger(context, "z");
                                                createCoordWaypoint(x, y, z);
                                                return 1;
                                            })))));

            dispatcher.register(ClientCommands.literal("sansrusmodlookat")
                    .then(ClientCommands.argument("pitch", FloatArgumentType.floatArg(-90f, 90f))
                            .then(ClientCommands.argument("yaw", FloatArgumentType.floatArg(-180f, 180f))
                                    .executes(context -> {
                                        float pitch = FloatArgumentType.getFloat(context, "pitch");
                                        float yaw = FloatArgumentType.getFloat(context, "yaw");
                                        LocalPlayer player = Minecraft.getInstance().player;
                                        if (player == null) return 0;
                                        player.setXRot(pitch);
                                        player.setYRot(yaw);
                                        return 1;
                                    }))));

            dispatcher.register(ClientCommands.literal("sansrusmodposblock")
                    .then(ClientCommands.argument("x", DoubleArgumentType.doubleArg(0.0, 1.0))
                            .then(ClientCommands.argument("z", DoubleArgumentType.doubleArg(0.0, 1.0))
                                    .executes(context -> {
                                        double x = DoubleArgumentType.getDouble(context, "x");
                                        double z = DoubleArgumentType.getDouble(context, "z");
                                        LocalPlayer player = Minecraft.getInstance().player;
                                        if (player == null) return 0;
                                        BlockPos blockPos = player.blockPosition();
                                        player.setPos(blockPos.getX() + x, player.getY(), blockPos.getZ() + z);
                                        return 1;
                                    }))));

        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!config.autoRespawn) return;
            if (client.player == null) return;

            String playerName = client.player.getName().getString();
            if (!playerName.equals("Sansrus") && !playerName.equals("EN403")) return;

            if (client.screen instanceof DeathScreen) {
                if (client.getConnection() != null) {
                    client.getConnection().send(
                            new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)
                    );
                    client.setScreen(null);
                }
            }
        });
    }

    private static void sendDeathMessage(Minecraft client, BlockPos pos, String dim) {
        String coordinates = String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ());
        String command = String.format("/create_death_waypoint %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), dim);

        Component hoverText = Component.translatable("sansrusmod.message.deathCoords.hover");

        Component message = Component.translatable("sansrusmod.message.deathCoords")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(coordinates)
                        .withStyle(ChatFormatting.GOLD)
                        .withStyle(ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent.RunCommand(command))
                                .withHoverEvent(new HoverEvent.ShowText(hoverText))
                        ));

        ChatPipelineFlags.deathMessageActive = true;
        client.player.sendSystemMessage(message);
        ChatPipelineFlags.deathMessageActive = false;
    }

    private void createDeathWaypoint(int x, int y, int z, String targetDim) {
        if (!isXaeroMinimapLoaded) return;

        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

//            client.player.sendMessage(Component.literal("=== НАЧАЛО СОЗДАНИЯ МЕТКИ ===").withStyle(ChatFormatting.AQUA), false);
//            client.player.sendMessage(Component.literal("Координаты: " + x + " " + y + " " + z).withStyle(ChatFormatting.YELLOW), false);
//            client.player.sendMessage(Component.literal("Целевое измерение: " + targetDim).withStyle(ChatFormatting.YELLOW), false);

            MinimapSession minimapSession = (MinimapSession) BuiltInHudModules.MINIMAP.getCurrentSession();

            if (minimapSession == null) {
                client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorSession").withStyle(ChatFormatting.RED));
                return;
            }

            minimapSession.getWorldStateUpdater().update();

            MinimapWorld currentWorld = minimapSession.getWorldManager().getAutoWorld();

            if (currentWorld == null) {
                client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorWorld").withStyle(ChatFormatting.RED));
                return;
            }

//            client.player.sendMessage(Component.literal("Текущий мир: " + currentWorld.getFullPath()).withStyle(ChatFormatting.YELLOW), false);

            //? if >=1.21.11 {
            /*String currentDim = client.player.level().dimension().location().getPath();
            *///?} else {
            String currentDim = client.player.level().dimension().identifier().getPath();
            //?}
//            client.player.sendMessage(Component.literal("Текущее измерение игрока: " + currentDim).withStyle(ChatFormatting.YELLOW), false);

            MinimapWorld world;

            if (currentDim.equals(targetDim)) {
                world = currentWorld;
//                client.player.sendMessage(Component.literal("Измерения совпадают, используем текущий мир").withStyle(ChatFormatting.GREEN), false);
            } else {
//                client.player.sendMessage(Component.literal("Измерения различаются, ищем целевой мир").withStyle(ChatFormatting.YELLOW), false);

                ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", targetDim));
//                client.player.sendMessage(Component.literal("RegistryKey создан: " + targetDimKey.location()).withStyle(ChatFormatting.YELLOW), false);

                String targetDimDir = minimapSession.getDimensionHelper().getDimensionDirectoryName(targetDimKey);
//                client.player.sendMessage(Component.literal("Директория измерения: " + targetDimDir).withStyle(ChatFormatting.YELLOW), false);

                XaeroPath rootPath = currentWorld.getFullPath().getRoot();
//                client.player.sendMessage(Component.literal("Корневой путь: " + rootPath).withStyle(ChatFormatting.YELLOW), false);

                XaeroPath targetContainerPath = rootPath.resolve(targetDimDir);
//                client.player.sendMessage(Component.literal("Путь контейнера: " + targetContainerPath).withStyle(ChatFormatting.YELLOW), false);

                MinimapWorldContainer targetContainer = minimapSession.getWorldManager().getWorldContainerNullable(targetContainerPath);

                if (targetContainer == null) {
                    client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorContainer").append(Component.literal(targetContainerPath.toString())).withStyle(ChatFormatting.RED));
                    client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorVisit", targetDim).withStyle(ChatFormatting.RED));
                    return;
                }

//                client.player.sendMessage(Component.literal("Контейнер найден: " + targetContainer).withStyle(ChatFormatting.YELLOW), false);

                world = targetContainer.getFirstWorld();

                if (world == null) {
                    client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorNoWorlds").withStyle(ChatFormatting.RED));
                    client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorVisit", targetDim).withStyle(ChatFormatting.RED));
                    return;
                }

//                client.player.sendMessage(Component.literal("Целевой мир найден: " + world.getFullPath()).withStyle(ChatFormatting.GREEN), false);
            }

//            client.player.sendMessage(Component.literal("Локальный ключ мира: " + world.getLocalWorldKey()).withStyle(ChatFormatting.YELLOW), false);

            WaypointSet currentSet = world.getCurrentWaypointSet();

            if (currentSet == null) {
                client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorSet").withStyle(ChatFormatting.RED));
                return;
            }

//            client.player.sendMessage(Component.literal("Набор вейпоинтов: " + currentSet.getName()).withStyle(ChatFormatting.YELLOW), false);

            Waypoint waypoint = new Waypoint(
                    x,
                    y,
                    z,
                    Component.translatable("sansrusmod.waypoint.death.name").getString(),
                    Component.translatable("sansrusmod.waypoint.death.symbol").getString(),
                    WaypointColor.RED,
                    WaypointPurpose.NORMAL,
                    false,
                    true
            );

            currentSet.add(waypoint, true);
//            client.player.sendMessage(Component.literal("Вейпоинт добавлен в набор").withStyle(ChatFormatting.YELLOW), false);

            try {
                minimapSession.getWorldManagerIO().saveWorld(world);
                client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointCreated").withStyle(ChatFormatting.GREEN));
            } catch (Exception e) {
                e.printStackTrace();
                client.player.sendSystemMessage(Component.translatable("sansrusmod.message.waypointErrorSave").append(Component.literal(e.getMessage())).withStyle(ChatFormatting.RED));
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.translatable("sansrusmod.message.waypointError").append(Component.literal(e.getMessage())).withStyle(ChatFormatting.RED));
            }
        }
    }

    private void createCoordWaypoint(int x, int y, int z) {
        if (!isXaeroMinimapLoaded) return;

        try {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            MinimapSession minimapSession = (MinimapSession) BuiltInHudModules.MINIMAP.getCurrentSession();
            if (minimapSession == null) return;

            minimapSession.getWorldStateUpdater().update();

            MinimapWorld world = minimapSession.getWorldManager().getAutoWorld();
            if (world == null) return;

            WaypointSet currentSet = world.getCurrentWaypointSet();
            if (currentSet == null) return;

            String label = String.format("%d %d %d", x, y, z);
            Waypoint waypoint = new Waypoint(
                    x, y, z,
                    label,
                    Component.translatable("sansrusmod.waypoint.coord.symbol").getString(),
                    WaypointColor.YELLOW,
                    WaypointPurpose.NORMAL,
                    false,
                    true
            );

            currentSet.add(waypoint, true);
            minimapSession.getWorldManagerIO().saveWorld(world);
            client.player.sendOverlayMessage(
                    Component.translatable("sansrusmod.message.coordWaypointCreated").append(Component.literal(label)).withStyle(ChatFormatting.GREEN));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void captureDeathSnapshotNow(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) return;
        ItemStack[] snapshot = new ItemStack[41];
        Inventory inv = player.getInventory();

        for (int i = 0; i < snapshot.length; i++) {
            ItemStack stack = inv.getItem(i);
            snapshot[i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }

        DeathHistoryManager.saveSnapshot(snapshot);

        if (isXaeroMinimapLoaded) {
            //? if >=1.21.11 {
            /*sendDeathMessage(client, player.blockPosition(), player.level().dimension().location().getPath());
            *///?} else {
            sendDeathMessage(client, player.blockPosition(), player.level().dimension().identifier().getPath());
            //?}
        }
    }
}
