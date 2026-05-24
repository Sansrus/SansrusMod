package org.example.sansrus.sansrusmod.client.deathlog;

import com.mojang.serialization.DataResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
                NbtCompound nbt = NbtIo.read(file.toPath());
                if (nbt != null && nbt.contains("Snapshots")) {
                    NbtList list = nbt.getList("Snapshots").get();
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

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        RegistryOps<NbtElement> ops = client.world.getRegistryManager().getOps(NbtOps.INSTANCE);

        NbtCompound snapshotNbt = new NbtCompound();
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        snapshotNbt.putString("Time", timeStr);

        NbtList itemsNbt = new NbtList();

        for (int i = 0; i < inventoryCache.length; i++) {
            ItemStack stack = inventoryCache[i];
            if (stack != null && !stack.isEmpty()) {
                DataResult<NbtElement> result = ItemStack.CODEC.encodeStart(ops, stack);
                int finalI = i;
                result.ifSuccess(nbtEl -> {
                    if (nbtEl instanceof NbtCompound itemCompound) {
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
        NbtCompound root = new NbtCompound();
        NbtList list = new NbtList();
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
        Path configDir = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve("sansrusmod")
                .resolve("deaths");
        return configDir.resolve(currentServerId + ".nbt").toFile();
    }

    private static String getServerId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer() && client.getServer() != null) {
            return "singleplayer_" + client.getServer().getSaveProperties().getLevelName();
        } else if (client.getCurrentServerEntry() != null) {
            return "server_" + client.getCurrentServerEntry().address
                    .replace(":", "_").replace(".", "-");
        }
        return "unknown_world";
    }

    public static class DeathSnapshot {
        public final NbtCompound rawNbt;
        public final String time;
        public final ItemStack[] inventory = new ItemStack[41];

        public DeathSnapshot(NbtCompound nbt) {
            this.rawNbt = nbt;

            this.time = nbt.getString("Time").orElse("неизвестно");

            for (int i = 0; i < inventory.length; i++) {
                inventory[i] = ItemStack.EMPTY;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                RegistryOps<NbtElement> ops = client.world.getRegistryManager().getOps(NbtOps.INSTANCE);
                NbtList itemsList = nbt.getList("Items").get();

                for (int i = 0; i < itemsList.size(); i++) {
                    NbtCompound itemNbt = itemsList.getCompound(i).get();
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
