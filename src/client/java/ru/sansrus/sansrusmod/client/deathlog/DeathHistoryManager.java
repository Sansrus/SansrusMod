package ru.sansrus.sansrusmod.client.deathlog;

import com.mojang.serialization.DataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public class DeathHistoryManager {

    public static final Logger LOGGER = LoggerFactory.getLogger("SansrusMod/DeathHistory");

    public static final List<DeathSnapshot> currentSnapshots = new ArrayList<>();
    private static String currentServerId = "unknown";

    public static void loadForServer() {
        currentServerId = getServerId();
        currentSnapshots.clear();
        File file = getSaveFile();

        if (file.exists()) {
            try {
                CompoundTag nbt = NbtIo.read(file.toPath());
                if (nbt != null && nbt.contains("Snapshots")) {
                    ListTag list = nbt.getList("Snapshots").get();
                    for (int i = 0; i < list.size(); i++) {
                        currentSnapshots.add(new DeathSnapshot(list.getCompound(i).get()));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void saveSnapshot(ItemStack[] inventoryCache) {
        boolean hasItems = false;
        for (ItemStack stack : inventoryCache) {
            if (stack != null && !stack.isEmpty()) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

                RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, client.level.registryAccess());

        CompoundTag snapshotNbt = new CompoundTag();
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        snapshotNbt.putString("Time", timeStr);

        String serverType;
        String serverName;
        if (client.isSingleplayer() && client.getSingleplayerServer() != null) {
            serverType = "singleplayer";
            serverName = client.getSingleplayerServer().getWorldData().getLevelName();
        } else if (client.getCurrentServer() != null) {
            serverType = "multiplayer";
            serverName = client.getCurrentServer().ip;
        } else {
            serverType = "unknown";
            serverName = "";
        }
        snapshotNbt.putString("ServerType", serverType);
        snapshotNbt.putString("ServerName", serverName);

        ListTag itemsNbt = new ListTag();

        for (int i = 0; i < inventoryCache.length; i++) {
            ItemStack stack = inventoryCache[i];
            if (stack != null && !stack.isEmpty()) {
                DataResult<Tag> result = ItemStack.CODEC.encodeStart(ops, stack);
                int finalI = i;
                result.ifSuccess(nbtEl -> {
                    if (nbtEl instanceof CompoundTag itemCompound) {
                        itemCompound.putInt("Slot", finalI);
                        itemsNbt.add(itemCompound);
                    }
                });
            }
        }

        if (itemsNbt.isEmpty()) {
            return;
        }

        snapshotNbt.put("Items", itemsNbt);
        currentSnapshots.add(0, new DeathSnapshot(snapshotNbt));

        saveToDisk();
    }


    private static void saveToDisk() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (DeathSnapshot snap : currentSnapshots) {
            list.add(snap.rawNbt);
        }
        root.put("Snapshots", list);

        try {
            File file = getSaveFile();
            file.getParentFile().mkdirs();
            NbtIo.write(root, file.toPath());
        } catch (Exception ignored) {
        }
    }

    private static File getSaveFile() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("sansrusmod")
                .resolve("deaths");
        return configDir.resolve(currentServerId + ".nbt").toFile();
    }

    private static String getServerId() {
        Minecraft client = Minecraft.getInstance();
        if (client.isSingleplayer() && client.getSingleplayerServer() != null) {
            return "singleplayer_" + client.getSingleplayerServer().getWorldData().getLevelName();
        } else if (client.getCurrentServer() != null) {
            return "server_" + client.getCurrentServer().ip
                    .replace(":", "_").replace(".", "-");
        }
        return "unknown_world";
    }

    public static class DeathSnapshot {
        public final CompoundTag rawNbt;
        public final String time;
        public final String serverType;
        public final String serverName;
        public final ItemStack[] inventory = new ItemStack[41];

        public DeathSnapshot(CompoundTag nbt) {
            this.rawNbt = nbt;

            this.time = nbt.getString("Time").orElse("неизвестно");
            this.serverType = nbt.getString("ServerType").orElse("unknown");
            this.serverName = nbt.getString("ServerName").orElse("");

            for (int i = 0; i < inventory.length; i++) {
                inventory[i] = ItemStack.EMPTY;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, client.level.registryAccess());
                ListTag itemsList = nbt.getList("Items").get();

                for (int i = 0; i < itemsList.size(); i++) {
                    CompoundTag itemNbt = itemsList.getCompound(i).get();
                    int slot = itemNbt.getInt("Slot").orElse(-1);
                    if (slot >= 0 && slot < inventory.length) {
                        DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, itemNbt);
                        result.ifSuccess(stack -> inventory[slot] = stack);
                    }
                }
            }
        }
    }

    public static void deleteSnapshot(int index) {
        if (index < 0 || index >= currentSnapshots.size()) return;
        String time = currentSnapshots.get(index).time;
        currentSnapshots.remove(index);
        saveToDisk();
    }
}
