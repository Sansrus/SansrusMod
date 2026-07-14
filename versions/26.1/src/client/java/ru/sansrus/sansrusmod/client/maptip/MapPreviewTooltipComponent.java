package ru.sansrus.sansrusmod.client.maptip;

import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.HashMap;
import java.util.Map;

public class MapPreviewTooltipComponent implements ClientTooltipComponent {

    private static final int SIZE = 128;

    private static final Map<Integer, DynamicTexture> textureCache = new HashMap<>();
    private static final Map<Integer, Identifier> idCache = new HashMap<>();

    private final MapItemSavedData state;
    private final int mapId;

    public MapPreviewTooltipComponent(MapTooltipData data) {
        this.state = data.state();
        this.mapId = data.mapId().id();
    }

    @Override
    public int getHeight(Font font) { return SIZE + 6; }

    @Override
    public int getWidth(Font font) { return SIZE + 6; }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor guiGraphics) {
        Identifier texId = getOrUpdateTexture();
        if (texId == null) return;

        guiGraphics.fill(x, y, x + SIZE + 6, y + SIZE + 6, 0xFF594020);
        guiGraphics.fill(x + 1, y + 1, x + SIZE + 5, y + SIZE + 5, 0xFF7A5C30);
        guiGraphics.fill(x + 2, y + 2, x + SIZE + 4, y + SIZE + 4, 0xFF594020);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texId,
                x + 3, y + 3, 0f, 0f, SIZE, SIZE, SIZE, SIZE);
    }

    private Identifier getOrUpdateTexture() {
        int id = mapId;
        Minecraft mc = Minecraft.getInstance();

        DynamicTexture texture = textureCache.get(id);

        if (texture == null) {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
            fillImage(image);
            texture = new DynamicTexture(() -> "map_preview", image);
            Identifier texId = Identifier.fromNamespaceAndPath("sansrusmod", "map_preview/" + id);
            mc.getTextureManager().register(texId, texture);
            textureCache.put(id, texture);
            idCache.put(id, texId);
        } else {
            NativeImage image = texture.getPixels();
            if (image != null) {
                fillImage(image);
                texture.upload();
            }
        }

        return idCache.get(id);
    }

    private void fillImage(NativeImage image) {
        byte[] colors = state.colors;
        for (int i = 0; i < SIZE * SIZE; i++) {
            int rawByte = colors[i] & 0xFF;

            int argb;
            if (rawByte / 4 == 0) {
                argb = 0xFF707070;
            } else {
                argb = MapColor.getColorFromPackedId(rawByte);
            }

            int abgr = (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16);
            image.setPixelABGR(i % SIZE, i / SIZE, abgr);
        }
    }
}
