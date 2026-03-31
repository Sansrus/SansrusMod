package org.example.sansrus.sansrusmod.client.maptip;

import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class MapPreviewTooltipComponent implements TooltipComponent {

    private static final int SIZE = 128;

    // NativeImageBackedTexture — конкретная реализация интерфейса DynamicTexture
    private static final Map<Integer, NativeImageBackedTexture> textureCache = new HashMap<>();
    private static final Map<Integer, Identifier> idCache = new HashMap<>();

    private final MapIdComponent mapId;
    private final MapState state;

    public MapPreviewTooltipComponent(MapTooltipData data) {
        this.mapId = data.mapId();
        this.state = data.state();
    }

    @Override
    public int getHeight(TextRenderer textRenderer) { return SIZE + 6; }

    @Override
    public int getWidth(TextRenderer textRenderer) { return SIZE + 6; }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        Identifier texId = getOrUpdateTexture();
        if (texId == null) return;

        // Рамка в стиле ванильной карты
        context.fill(x,     y,     x + SIZE + 6, y + SIZE + 6, 0xFF594020);
        context.fill(x + 1, y + 1, x + SIZE + 5, y + SIZE + 5, 0xFF7A5C30);
        context.fill(x + 2, y + 2, x + SIZE + 4, y + SIZE + 4, 0xFF594020);

        // Содержимое карты
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texId,
                x + 3, y + 3, 0, 0, SIZE, SIZE, SIZE, SIZE);
    }

    private Identifier getOrUpdateTexture() {
        int id = mapId.id();
        MinecraftClient mc = MinecraftClient.getInstance();

        NativeImageBackedTexture texture = textureCache.get(id);

        if (texture == null) {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
            fillImage(image);
            // Конструктор: Supplier<String> nameSupplier, NativeImage image
            texture = new NativeImageBackedTexture(() -> "sansrusmod:map_preview/" + id, image);
            Identifier texId = Identifier.of("sansrusmod", "map_preview/" + id);
            mc.getTextureManager().registerTexture(texId, texture); // AbstractTexture — принимается ✓
            textureCache.put(id, texture);
            idCache.put(id, texId);
        } else {
            // Обновляем если карта изменилась
            NativeImage image = texture.getImage();
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
                argb = MapColor.getRenderColor(rawByte);
            }

            image.setColorArgb(i % SIZE, i / SIZE, argb);
        }
    }
}
