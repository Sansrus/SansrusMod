package ru.sansrus.sansrusmod.client.deathlog;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;

public class DeathHistoryScreen extends Screen {

    private static final Identifier INVENTORY_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");

    private static final Identifier[] ARMOR_SLOT_SPRITES = {
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    };

    private static final Identifier OFFHAND_SLOT_SPRITE =
            InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;

    private final Screen parent;

    private int currentIndex  = 0;
    private int previousIndex = 0;

    private float slideOffset = 0f;
    private int   slideDir    = 0;

    private static final int BG_W = 176;
    private static final int BG_H = 166;
    private int bgX, bgY;

    private boolean pendingClipboardCopy = false;

    public DeathHistoryScreen(Screen parent) {
        super(Component.translatable("sansrusmod.deathHistory.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        bgX = (this.width  - BG_W) / 2;
        bgY = (this.height - BG_H) / 2;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> navigate(-1))
                .bounds(5, this.height / 2 - 10, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> navigate(1))
                .bounds(this.width - 25, this.height / 2 - 10, 20, 20).build());
    }

    private void navigate(int dir) {
        int size = DeathHistoryManager.currentSnapshots.size();
        if (size <= 1) return;

        int next = currentIndex + dir;
        if (next < 0 || next >= size) return;

        previousIndex = currentIndex;
        slideDir      = dir;
        currentIndex  = next;

        slideOffset = dir > 0 ? this.width : -this.width;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_LEFT)  { navigate(-1); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { navigate(1);  return true; }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            int size = DeathHistoryManager.currentSnapshots.size();
            if (size > 0) {
                DeathHistoryManager.deleteSnapshot(currentIndex);
                if (currentIndex >= DeathHistoryManager.currentSnapshots.size()) {
                    currentIndex = Math.max(0, currentIndex - 1);
                }
                previousIndex = currentIndex;
                slideOffset = 0f;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_C && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            pendingClipboardCopy = true;
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0101010);
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);

        if (DeathHistoryManager.currentSnapshots.isEmpty()) {
            guiGraphics.centeredText(
                    this.font, Component.translatable("sansrusmod.deathHistory.noDeaths"),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAAAA);
            return;
        }

        if (Math.abs(slideOffset) > 0.5f) {
            slideOffset += (0f - slideOffset) * 0.3f * delta;
        } else {
            slideOffset = 0f;
            slideDir    = 0;
        }

        boolean animating = Math.abs(slideOffset) >= 0.5f;

        if (animating && slideDir != 0) {
            int prevDrawX = (int) (bgX + slideOffset - slideDir * this.width);
            DeathHistoryManager.DeathSnapshot prevSnap =
                    DeathHistoryManager.currentSnapshots.get(previousIndex);
            drawInventoryPanel(guiGraphics, prevSnap, prevDrawX, -1, -1);
        }

        int drawX = (int) (bgX + slideOffset);
        DeathHistoryManager.DeathSnapshot snap =
                DeathHistoryManager.currentSnapshots.get(currentIndex);
        ItemStack hovered = drawInventoryPanel(guiGraphics, snap, drawX, mouseX, mouseY);

        int SERVER_LABEL_Y = 42;
        String serverLabel;
        if ("singleplayer".equals(snap.serverType)) {
            serverLabel = Component.translatable("sansrusmod.deathHistory.server.singleplayer").getString()
                    + "/" + snap.serverName;
        } else if ("multiplayer".equals(snap.serverType)) {
            serverLabel = snap.serverName;
        } else {
            serverLabel = Component.translatable("sansrusmod.deathHistory.server.unknown").getString();
        }
        guiGraphics.centeredText(
                this.font, Component.literal(serverLabel),
                this.width / 2, bgY - SERVER_LABEL_Y, 0xFF888888);

        String timeLabel = (snap.time == null || snap.time.isBlank() || snap.time.equals("неизвестно"))
                ? Component.translatable("sansrusmod.deathHistory.noDate").getString()
                : snap.time;
        guiGraphics.centeredText(
                this.font, Component.literal(timeLabel),
                this.width / 2, bgY - 22, 0xFFFFD700);

        String counter = (currentIndex + 1) + " / " + DeathHistoryManager.currentSnapshots.size();
        guiGraphics.centeredText(
                this.font, Component.literal(counter),
                this.width / 2, bgY + BG_H + 8, 0xFFAAAAAA);

        if (!hovered.isEmpty() && !animating) {
            guiGraphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
        }

    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        boolean animating = Math.abs(slideOffset) >= 0.5f;
        if (pendingClipboardCopy && !animating) {
            pendingClipboardCopy = false;
            int drawX = (int) (bgX + slideOffset);
            copyInventoryToClipboardSync(drawX);
        }
        super.extractBackground(guiGraphics, mouseX, mouseY, delta);
    }

    private ItemStack drawInventoryPanel(GuiGraphicsExtractor guiGraphics,
                                         DeathHistoryManager.DeathSnapshot snap,
                                         int drawX, int mouseX, int mouseY) {
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                INVENTORY_TEXTURE,
                drawX, bgY, 0, 0, BG_W, BG_H, 256, 256);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mouseX >= 0) {
            int dollX1 = drawX + 25;
            int dollY1 = bgY + 15;
            int dollX2 = drawX + 75;
            int dollY2 = bgY + 80;
            float cx = (dollX1 + dollX2) / 2f;
            float cy = (dollY1 + dollY2) / 2f;
            InventoryScreen.extractEntityInInventoryFollowsMouse(guiGraphics, dollX1, dollY1, dollX2, dollY2,
                    30, 0f, cx, cy, mc.player);
        }

        ItemStack hovered = ItemStack.EMPTY;

        for (int i = 0; i < 9; i++)
            hovered = drawSlot(guiGraphics, snap.inventory[i],
                    drawX + 8 + i * 18, bgY + 142, mouseX, mouseY, hovered);

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                hovered = drawSlot(guiGraphics, snap.inventory[9 + row * 9 + col],
                        drawX + 8 + col * 18, bgY + 84 + row * 18, mouseX, mouseY, hovered);

        for (int i = 0; i < 4; i++) {
            int sx = drawX + 8;
            int sy = bgY + 62 - i * 18;
            ItemStack armorStack = snap.inventory[36 + i];

            if (armorStack == null || armorStack.isEmpty()) {
                guiGraphics.blitSprite(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        ARMOR_SLOT_SPRITES[i], sx, sy, 16, 16);
            }

            hovered = drawSlot(guiGraphics, armorStack, sx, sy, mouseX, mouseY, hovered);
        }

        int ohX = drawX + 77;
        int ohY = bgY + 62;
        ItemStack offhandStack = snap.inventory[40];

        if (offhandStack == null || offhandStack.isEmpty()) {
            guiGraphics.blitSprite(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    OFFHAND_SLOT_SPRITE, ohX, ohY, 16, 16);
        }

        hovered = drawSlot(guiGraphics, offhandStack, ohX, ohY, mouseX, mouseY, hovered);

        return hovered;
    }

    private ItemStack drawSlot(GuiGraphicsExtractor guiGraphics, ItemStack stack,
                               int sx, int sy, int mx, int my, ItemStack prev) {
        if (stack == null || stack.isEmpty()) return prev;

        guiGraphics.item(stack, sx, sy);
        guiGraphics.itemDecorations(this.font, stack, sx, sy);

        if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
            guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
            return stack;
        }
        return prev;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void copyInventoryToClipboardSync(int drawX) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();

        int sx = (int) (drawX * scale);
        int sy = (int) (bgY * scale);
        int sw = (int) (BG_W * scale);
        int sh = (int) (BG_H * scale);

        int windowHeight = mc.getWindow().getHeight();
        int glY = windowHeight - sy - sh;

        ByteBuffer buf = ByteBuffer.allocateDirect(sw * sh * 4);
        GL11.glReadPixels(sx, glY, sw, sh,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buf);

        BufferedImage image = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);

        int nonZeroPixels = 0;
        int blackPixels = 0;
        int transparentPixels = 0;
        for (int py = 0; py < sh; py++) {
            for (int px = 0; px < sw; px++) {
                int i = (py * sw + px) * 4;
                int r = buf.get(i) & 0xFF;
                int g = buf.get(i + 1) & 0xFF;
                int b = buf.get(i + 2) & 0xFF;
                int a = buf.get(i + 3) & 0xFF;

                int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(px, sh - 1 - py, color);

                if (color != 0) nonZeroPixels++;
                if (color == 0xFF000000) blackPixels++;
                if (a == 0) transparentPixels++;
            }
        }

        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new ImageSelection(image), null);

            if (mc.player != null) {
                mc.player.sendOverlayMessage(
                        Component.translatable("sansrusmod.deathHistory.inventoryCopied").withStyle(ChatFormatting.GREEN));
            }
        } catch (Exception e) {
            DeathHistoryManager.LOGGER.error("Ошибка копирования в буфер обмена: {}", e.getMessage(), e);
        }
    }

    private static class ImageSelection implements Transferable {
        private final Image image;

        ImageSelection(Image image) { this.image = image; }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{
                    DataFlavor.imageFlavor
            };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor))
                throw new UnsupportedFlavorException(flavor);
            return image;
        }
    }

}
