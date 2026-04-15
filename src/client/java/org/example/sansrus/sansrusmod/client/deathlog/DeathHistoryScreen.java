package org.example.sansrus.sansrusmod.client.deathlog;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class DeathHistoryScreen extends Screen {

    private static final Identifier INVENTORY_TEXTURE =
            InventoryScreen.BACKGROUND_TEXTURE;
//            Identifier.of("minecraft", "textures/gui/container/inventory.png");


    private static final Identifier[] ARMOR_SLOT_SPRITES = {
            PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE
    };

    private static final Identifier OFFHAND_SLOT_SPRITE =
            PlayerScreenHandler.EMPTY_OFF_HAND_SLOT_TEXTURE;


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
        super(Text.translatable("sansrusmod.deathHistory.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        bgX = (this.width  - BG_W) / 2;
        bgY = (this.height - BG_H) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> navigate(-1))
                .dimensions(5, this.height / 2 - 10, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> navigate(1))
                .dimensions(this.width - 25, this.height / 2 - 10, 20, 20).build());
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

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        int scanCode = keyInput.scancode();
        int modifiers = keyInput.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
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

        if (keyCode == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            pendingClipboardCopy = true;
            return true;
        }

        //? if >=1.21.11 {
        /*return super.keyPressed(keyInput);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(context, mouseX, mouseY, delta);

        if (DeathHistoryManager.currentSnapshots.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer, Text.translatable("sansrusmod.deathHistory.noDeaths").getString(),
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
            drawInventoryPanel(context, prevSnap, prevDrawX, -1, -1);
        }

        int drawX = (int) (bgX + slideOffset);
        DeathHistoryManager.DeathSnapshot snap =
                DeathHistoryManager.currentSnapshots.get(currentIndex);
        ItemStack hovered = drawInventoryPanel(context, snap, drawX, mouseX, mouseY);

        String timeLabel = (snap.time == null || snap.time.isBlank() || snap.time.equals("неизвестно"))
                ? Text.translatable("sansrusmod.deathHistory.noDate").getString()
                : snap.time;
        context.drawCenteredTextWithShadow(
                this.textRenderer, timeLabel,
                this.width / 2, bgY - 22, 0xFFFFD700);

        String counter = (currentIndex + 1) + " / " + DeathHistoryManager.currentSnapshots.size();
        context.drawCenteredTextWithShadow(
                this.textRenderer, counter,
                this.width / 2, bgY + BG_H + 8, 0xFFAAAAAA);

        if (!hovered.isEmpty() && !animating) {
            context.drawItemTooltip(this.textRenderer, hovered, mouseX, mouseY);
        }

        //? if >=1.21.11 {
        /*if (pendingClipboardCopy && !animating) {
            pendingClipboardCopy = false;
            copyInventoryToClipboardSync(drawX);
        }
        *///?}

    }
    
    //? if <1.21.11 {
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean animating = Math.abs(slideOffset) >= 0.5f;
        if (pendingClipboardCopy && !animating) {
            pendingClipboardCopy = false;
            int drawX = (int) (bgX + slideOffset);
            copyInventoryToClipboardSync(drawX);
        }
        super.renderBackground(context, mouseX, mouseY, delta);
    }
    //?}

    private ItemStack drawInventoryPanel(DrawContext context,
                                         DeathHistoryManager.DeathSnapshot snap,
                                         int drawX, int mouseX, int mouseY) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE,
                drawX, bgY, 0, 0, BG_W, BG_H, 256, 256);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mouseX >= 0) {
            int dollX1 = drawX + 25;
            int dollY1 = bgY + 15;
            int dollX2 = drawX + 75;
            int dollY2 = bgY + 80;
            float cx = (dollX1 + dollX2) / 2f;
            float cy = (dollY1 + dollY2) / 2f;
            InventoryScreen.drawEntity(context, dollX1, dollY1, dollX2, dollY2,
                    30, 0f, cx, cy, mc.player);
        }

        ItemStack hovered = ItemStack.EMPTY;

        for (int i = 0; i < 9; i++)
            hovered = drawSlot(context, snap.inventory[i],
                    drawX + 8 + i * 18, bgY + 142, mouseX, mouseY, hovered);

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                hovered = drawSlot(context, snap.inventory[9 + row * 9 + col],
                        drawX + 8 + col * 18, bgY + 84 + row * 18, mouseX, mouseY, hovered);

        for (int i = 0; i < 4; i++) {
            int sx = drawX + 8;
            int sy = bgY + 62 - i * 18;
            ItemStack armorStack = snap.inventory[36 + i];

            if (armorStack == null || armorStack.isEmpty()) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                        ARMOR_SLOT_SPRITES[i], sx, sy, 16, 16);
            }

            hovered = drawSlot(context, armorStack, sx, sy, mouseX, mouseY, hovered);
        }

        int ohX = drawX + 77;
        int ohY = bgY + 62;
        ItemStack offhandStack = snap.inventory[40];

        if (offhandStack == null || offhandStack.isEmpty()) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                    OFFHAND_SLOT_SPRITE, ohX, ohY, 16, 16);
        }

        hovered = drawSlot(context, offhandStack, ohX, ohY, mouseX, mouseY, hovered);

        return hovered;

    }

    private ItemStack drawSlot(DrawContext ctx, ItemStack stack,
                               int sx, int sy, int mx, int my, ItemStack prev) {
        if (stack == null || stack.isEmpty()) return prev;

        ctx.drawItem(stack, sx, sy);
        ctx.drawStackOverlay(this.textRenderer, stack, sx, sy);

        if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
            ctx.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
            return stack;
        }
        return prev;
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    private void copyInventoryToClipboardSync(int drawX) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();

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
                mc.player.sendMessage(
                        Text.translatable("sansrusmod.deathHistory.inventoryCopied").formatted(Formatting.GREEN), true);
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
