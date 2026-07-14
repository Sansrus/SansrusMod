package ru.sansrus.sansrusmod.client.mixin.maptip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.sansrus.sansrusmod.client.SansrusModClient;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    @Shadow
    public void fill(int x1, int y1, int x2, int y2, int color) {}

    @Inject(
            method = "item(Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("RETURN")
    )
    private void onItemRender3(ItemStack stack, int x, int y, CallbackInfo ci) {
        onItemRender(stack, x, y);
    }

    @Inject(
            method = "item(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("RETURN")
    )
    private void onItemRender4(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        onItemRender(stack, x, y);
    }

    @Unique
    private void onItemRender(ItemStack stack, int x, int y) {
        if (!SansrusModClient.config.mapSlotPreview) return;
        if (stack.getItem() != Items.FILLED_MAP) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId == null) return;

        MapItemSavedData mapData = mc.level.getMapData(mapId);
        if (mapData == null) return;

        renderMapOverlay(mapData, x, y);
    }

    @Unique
    private void renderMapOverlay(MapItemSavedData mapData, int slotX, int slotY) {
        byte[] colors = mapData.colors;
        for (int sy = 0; sy < 16; sy++) {
            for (int sx = 0; sx < 16; sx++) {
                int mapX = sx * 8;
                int mapY = sy * 8;
                int rawByte = colors[mapY * 128 + mapX] & 0xFF;
                if (rawByte / 4 == 0) continue;
                int argb = MapColor.getColorFromPackedId(rawByte);
                this.fill(slotX + sx, slotY + sy, slotX + sx + 1, slotY + sy + 1, argb);
            }
        }
    }
}
