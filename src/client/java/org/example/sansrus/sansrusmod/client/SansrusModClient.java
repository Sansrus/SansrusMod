package org.example.sansrus.sansrusmod.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.example.sansrus.sansrusmod.client.chatcoord.ChatPipelineFlags;
import org.example.sansrus.sansrusmod.client.deathlog.DeathHistoryManager;
import org.example.sansrus.sansrusmod.client.maptip.MapPreviewTooltipComponent;
import org.example.sansrus.sansrusmod.client.maptip.MapTooltipData;
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

    private static BlockPos pendingDeathPos = null;
    private static String pendingDeathDim = null;
    private static boolean hasPendingDeath = false;

    private static ClientPlayerEntity lastPlayer = null;
    private static float lastHealth = 20.0f;

    private ItemStack[] cachedInventory = new ItemStack[41];
    public static SansrusConfig config;
    public static boolean isXaeroMinimapLoaded = false;

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "false");
        config = SansrusConfig.load();

        isXaeroMinimapLoaded = FabricLoader.getInstance().isModLoaded("xaerominimap");

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof MapTooltipData mapData) {
                return new MapPreviewTooltipComponent(mapData);
            }
            return null;
        });


        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            DeathHistoryManager.loadForServer();
        });


        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("create_death_waypoint")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                            .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                    .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                            .then(ClientCommandManager.argument("dim", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        int x = IntegerArgumentType.getInteger(context, "x");
                                                        int y = IntegerArgumentType.getInteger(context, "y");
                                                        int z = IntegerArgumentType.getInteger(context, "z");
                                                        String dim = StringArgumentType.getString(context, "dim");
                                                        createDeathWaypoint(x, y, z, dim);
                                                        return 1;
                                                    }))))));
            dispatcher.register(ClientCommandManager.literal("create_coord_waypoint")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                            .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                    .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                            .executes(context -> {
                                                int x = IntegerArgumentType.getInteger(context, "x");
                                                int y = IntegerArgumentType.getInteger(context, "y");
                                                int z = IntegerArgumentType.getInteger(context, "z");
                                                createCoordWaypoint(x, y, z);
                                                return 1;
                                            })))));

        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!config.autoRespawn) return;
            if (client.player == null) return;
            
            String playerName = client.player.getName().getString();
            if (!playerName.equals("Sansrus") && !playerName.equals("EN403")) return;
            
            if (client.currentScreen instanceof DeathScreen) {
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendPacket(
                            new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN)
                    );
                    client.setScreen(null);
                }
            }
        });
    }

    private static void sendDeathMessage(MinecraftClient client, BlockPos pos, String dim) {
        String coordinates = String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ());
        String command = String.format("/create_death_waypoint %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), dim);

        Text hoverText = Text.translatable("sansrusmod.message.deathCoords.hover");

        Text message = Text.translatable("sansrusmod.message.deathCoords")
                .formatted(Formatting.RED)
                .append(Text.literal(coordinates)
                        .formatted(Formatting.GOLD)
                        .formatted(Formatting.UNDERLINE)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.RunCommand(command))
                                .withHoverEvent(new HoverEvent.ShowText(hoverText))
                        ));

        ChatPipelineFlags.deathMessageActive = true;
        client.player.sendMessage(message, false);
        ChatPipelineFlags.deathMessageActive = false;
    }

    private void createDeathWaypoint(int x, int y, int z, String targetDim) {
        if (!isXaeroMinimapLoaded) return;
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

//            client.player.sendMessage(Text.literal("=== НАЧАЛО СОЗДАНИЯ МЕТКИ ===").formatted(Formatting.AQUA), false);
//            client.player.sendMessage(Text.literal("Координаты: " + x + " " + y + " " + z).formatted(Formatting.YELLOW), false);
//            client.player.sendMessage(Text.literal("Целевое измерение: " + targetDim).formatted(Formatting.YELLOW), false);

            MinimapSession minimapSession = (MinimapSession) BuiltInHudModules.MINIMAP.getCurrentSession();

            if (minimapSession == null) {
                client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorSession").formatted(Formatting.RED), false);
                return;
            }

            minimapSession.getWorldStateUpdater().update();

            MinimapWorld currentWorld = minimapSession.getWorldManager().getAutoWorld();

            if (currentWorld == null) {
                client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorWorld").formatted(Formatting.RED), false);
                return;
            }

//            client.player.sendMessage(Text.literal("Текущий мир: " + currentWorld.getFullPath()).formatted(Formatting.YELLOW), false);

            //? if >=1.21.11 {
            /*String currentDim = client.player.getEntityWorld().getRegistryKey().getValue().getPath();
            *///?} else {
            String currentDim = client.player.getWorld().getRegistryKey().getValue().getPath();
            //?}
//            client.player.sendMessage(Text.literal("Текущее измерение игрока: " + currentDim).formatted(Formatting.YELLOW), false);

            MinimapWorld world;

            if (currentDim.equals(targetDim)) {
                world = currentWorld;
//                client.player.sendMessage(Text.literal("Измерения совпадают, используем текущий мир").formatted(Formatting.GREEN), false);
            } else {
//                client.player.sendMessage(Text.literal("Измерения различаются, ищем целевой мир").formatted(Formatting.YELLOW), false);

                RegistryKey<World> targetDimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", targetDim));
//                client.player.sendMessage(Text.literal("RegistryKey создан: " + targetDimKey.getValue()).formatted(Formatting.YELLOW), false);

                String targetDimDir = minimapSession.getDimensionHelper().getDimensionDirectoryName(targetDimKey);
//                client.player.sendMessage(Text.literal("Директория измерения: " + targetDimDir).formatted(Formatting.YELLOW), false);

                XaeroPath rootPath = currentWorld.getFullPath().getRoot();
//                client.player.sendMessage(Text.literal("Корневой путь: " + rootPath).formatted(Formatting.YELLOW), false);

                XaeroPath targetContainerPath = rootPath.resolve(targetDimDir);
//                client.player.sendMessage(Text.literal("Путь контейнера: " + targetContainerPath).formatted(Formatting.YELLOW), false);

                MinimapWorldContainer targetContainer = minimapSession.getWorldManager().getWorldContainerNullable(targetContainerPath);

                if (targetContainer == null) {
                    client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorContainer").append(Text.literal(targetContainerPath.toString())).formatted(Formatting.RED), false);
                    client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorVisit", targetDim).formatted(Formatting.RED), false);
                    return;
                }

//                client.player.sendMessage(Text.literal("Контейнер найден: " + targetContainer).formatted(Formatting.YELLOW), false);

                world = targetContainer.getFirstWorld();

                if (world == null) {
                    client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorNoWorlds").formatted(Formatting.RED), false);
                    client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorVisit", targetDim).formatted(Formatting.RED), false);
                    return;
                }

//                client.player.sendMessage(Text.literal("Целевой мир найден: " + world.getFullPath()).formatted(Formatting.GREEN), false);
            }

//            client.player.sendMessage(Text.literal("Локальный ключ мира: " + world.getLocalWorldKey()).formatted(Formatting.YELLOW), false);

            WaypointSet currentSet = world.getCurrentWaypointSet();

            if (currentSet == null) {
                client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorSet").formatted(Formatting.RED), false);
                return;
            }

//            client.player.sendMessage(Text.literal("Набор вейпоинтов: " + currentSet.getName()).formatted(Formatting.YELLOW), false);

            Waypoint waypoint = new Waypoint(
                    x,
                    y,
                    z,
                    Text.translatable("sansrusmod.waypoint.death.name").getString(),
                    Text.translatable("sansrusmod.waypoint.death.symbol").getString(),
                    WaypointColor.RED,
                    WaypointPurpose.NORMAL,
                    false,
                    true
            );

            currentSet.add(waypoint, true);
//            client.player.sendMessage(Text.literal("Вейпоинт добавлен в набор").formatted(Formatting.YELLOW), false);

            try {
                minimapSession.getWorldManagerIO().saveWorld(world);
                client.player.sendMessage(Text.translatable("sansrusmod.message.waypointCreated").formatted(Formatting.GREEN), false);
            } catch (Exception e) {
                e.printStackTrace();
                client.player.sendMessage(Text.translatable("sansrusmod.message.waypointErrorSave").append(Text.literal(e.getMessage())).formatted(Formatting.RED), false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.translatable("sansrusmod.message.waypointError").append(Text.literal(e.getMessage())).formatted(Formatting.RED), false);
            }
        }
    }

    private void createCoordWaypoint(int x, int y, int z) {
        if (!isXaeroMinimapLoaded) return;
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
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
                    Text.translatable("sansrusmod.waypoint.coord.symbol").getString(),
                    WaypointColor.YELLOW,
                    WaypointPurpose.NORMAL,
                    false,
                    true
            );

            currentSet.add(waypoint, true);
            minimapSession.getWorldManagerIO().saveWorld(world);
            client.player.sendMessage(
                    Text.translatable("sansrusmod.message.coordWaypointCreated").append(Text.literal(label)).formatted(Formatting.GREEN), true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void captureDeathSnapshotNow(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack[] snapshot = new ItemStack[41];
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < snapshot.length; i++) {
            ItemStack stack = inv.getStack(i);
            snapshot[i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }

        DeathHistoryManager.saveSnapshot(snapshot);

        if (isXaeroMinimapLoaded) {
            //? if >=1.21.11 {
            /*sendDeathMessage(client, player.getBlockPos(), player.getEntityWorld().getRegistryKey().getValue().getPath());
            *///?} else {
            sendDeathMessage(client, player.getBlockPos(), player.getWorld().getRegistryKey().getValue().getPath());
            //?}
        }
    }
}
