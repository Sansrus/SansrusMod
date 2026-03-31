package org.example.sansrus.sansrusmod.client.deathlog;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

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

    // ── Состояние ──────────────────────────────────────────────────────────────
    private int currentIndex  = 0;
    private int previousIndex = 0;

    // ── Анимация ───────────────────────────────────────────────────────────────

    private float slideOffset = 0f;
    private int   slideDir    = 0;

    // ── Геометрия GUI ──────────────────────────────────────────────────────────
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

    // ── Навигация ──────────────────────────────────────────────────────────────
    private void navigate(int dir) {
        int size = DeathHistoryManager.currentSnapshots.size();
        if (size <= 1) return;

        int next = currentIndex + dir;
        if (next < 0 || next >= size) return;

        // Запоминаем уходящий снимок и направление
        previousIndex = currentIndex;
        slideDir      = dir;
        currentIndex  = next;

        // Новый снимок начинает со стороны, откуда едет
        slideOffset = dir > 0 ? this.width : -this.width;
    }

    // ── Клавиатура ────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT)  { navigate(-1); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { navigate(1);  return true; }

        // Delete — удалить текущий снимок
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            int size = DeathHistoryManager.currentSnapshots.size();
            if (size > 0) {
                DeathHistoryManager.deleteSnapshot(currentIndex);
                // Корректируем индекс: если удалили последний — сдвигаемся назад
                if (currentIndex >= DeathHistoryManager.currentSnapshots.size()) {
                    currentIndex = Math.max(0, currentIndex - 1);
                }
                previousIndex = currentIndex;
                slideOffset = 0f;
            }
            return true;
        }

        // Ctrl+C — скопировать рендер инвентаря в буфер обмена
        if (keyCode == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            pendingClipboardCopy = true;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    // ── Рендер ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // 1. Тёмный фон
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // 2. Кнопки
        super.render(context, mouseX, mouseY, delta);

        // 3. Нет снимков
        if (DeathHistoryManager.currentSnapshots.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer, Text.translatable("sansrusmod.deathHistory.noDeaths").getString(),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAAAA);
            return;
        }

        // 4. Анимация — плавное приближение к 0
        if (Math.abs(slideOffset) > 0.5f) {
            slideOffset += (0f - slideOffset) * 0.3f * delta;
        } else {
            slideOffset = 0f;
            slideDir    = 0;
        }

        boolean animating = Math.abs(slideOffset) >= 0.5f;

        // 5. Рисуем УХОДЯЩИЙ снимок
        if (animating && slideDir != 0) {
            int prevDrawX = (int) (bgX + slideOffset - slideDir * this.width);
            DeathHistoryManager.DeathSnapshot prevSnap =
                    DeathHistoryManager.currentSnapshots.get(previousIndex);
            drawInventoryPanel(context, prevSnap, prevDrawX, -1, -1);
        }

        // 6. Рисуем ТЕКУЩИЙ снимок
        int drawX = (int) (bgX + slideOffset);
        DeathHistoryManager.DeathSnapshot snap =
                DeathHistoryManager.currentSnapshots.get(currentIndex);
        ItemStack hovered = drawInventoryPanel(context, snap, drawX, mouseX, mouseY);

        if (pendingClipboardCopy && !animating) {
            pendingClipboardCopy = false;
            copyInventoryToClipboard(drawX);
        }

        // 7. Дата и время
        String timeLabel = (snap.time == null || snap.time.isBlank() || snap.time.equals("неизвестно"))
                ? Text.translatable("sansrusmod.deathHistory.noDate").getString()
                : snap.time;
        context.drawCenteredTextWithShadow(
                this.textRenderer, timeLabel,
                this.width / 2, bgY - 22, 0xFFFFD700);

        // 8. Индекс — поверх всего, снизу по центру экрана
        String counter = (currentIndex + 1) + " / " + DeathHistoryManager.currentSnapshots.size();
        context.drawCenteredTextWithShadow(
                this.textRenderer, counter,
                this.width / 2, bgY + BG_H + 8, 0xFFAAAAAA);

        // 9. Тултип — самым последним
        if (!hovered.isEmpty() && !animating) {
            context.drawItemTooltip(this.textRenderer, hovered, mouseX, mouseY);
        }

    }

    // ── Рендер одной панели инвентаря ─────────────────────────────────────────
    // Возвращает ItemStack под курсором (или EMPTY если это уходящая панель)
    private ItemStack drawInventoryPanel(DrawContext context,
                                         DeathHistoryManager.DeathSnapshot snap,
                                         int drawX, int mouseX, int mouseY) {
        // Текстура инвентаря
        context.drawTexture(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE,
                drawX, bgY, 0, 0, BG_W, BG_H, 256, 256);

        // Кукла игрока
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

        // Слоты
        ItemStack hovered = ItemStack.EMPTY;

        // Хотбар: 0-8
        for (int i = 0; i < 9; i++)
            hovered = drawSlot(context, snap.inventory[i],
                    drawX + 8 + i * 18, bgY + 142, mouseX, mouseY, hovered);

        // Основной инвентарь: 9-35
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                hovered = drawSlot(context, snap.inventory[9 + row * 9 + col],
                        drawX + 8 + col * 18, bgY + 84 + row * 18, mouseX, mouseY, hovered);

        // Броня: 36-39
        for (int i = 0; i < 4; i++) {
            int sx = drawX + 8;
            int sy = bgY + 62 - i * 18;
            ItemStack armorStack = snap.inventory[36 + i];

            // Спрайт пустого слота — только если слот пуст (ванильное поведение)
            if (armorStack == null || armorStack.isEmpty()) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                        ARMOR_SLOT_SPRITES[i], sx, sy, 16, 16);
            }

            hovered = drawSlot(context, armorStack, sx, sy, mouseX, mouseY, hovered);
        }

        // Офхенд: 40
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

    // ── Слот ──────────────────────────────────────────────────────────────────
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

    private void copyInventoryToClipboard(int drawX) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();

        // Координаты в реальных пикселях окна
        int sx = (int) (drawX * scale);
        int sy = (int) (bgY  * scale);
        int sw = (int) (BG_W * scale);
        int sh = (int) (BG_H * scale);

        // OpenGL читает снизу вверх, поэтому вычисляем Y от низа окна
        int windowHeight = mc.getWindow().getHeight();
        int glY = windowHeight - sy - sh;

        // Читаем пиксели из текущего фреймбуфера
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocateDirect(sw * sh * 4);
        org.lwjgl.opengl.GL11.glReadPixels(sx, glY, sw, sh,
                org.lwjgl.opengl.GL11.GL_RGBA,
                org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, buf);

        // Конвертируем в BufferedImage с вертикальным флипом (GL читает снизу вверх)
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(sw, sh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < sh; py++) {
            for (int px = 0; px < sw; px++) {
                int i = (py * sw + px) * 4;
                int r = buf.get(i)     & 0xFF;
                int g = buf.get(i + 1) & 0xFF;
                int b = buf.get(i + 2) & 0xFF;
                int a = buf.get(i + 3) & 0xFF;
                image.setRGB(px, sh - 1 - py, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // Кладём в буфер обмена
        try {
            java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new ImageSelection(image), null);

            if (mc.player != null) {
                mc.player.sendMessage(
                        Text.translatable("sansrusmod.deathHistory.inventoryCopied").formatted(Formatting.GREEN), true);
            }
            DeathHistoryManager.LOGGER.info("Снимок инвентаря скопирован в буфер обмена");
        } catch (Exception e) {
            DeathHistoryManager.LOGGER.info("Ошибка копирования в буфер обмена: {}", e.getMessage());
        }
    }

    // Вспомогательный класс для передачи изображения в буфер обмена
    private static class ImageSelection implements java.awt.datatransfer.Transferable {
        private final java.awt.Image image;

        ImageSelection(java.awt.Image image) { this.image = image; }

        @Override
        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
            return new java.awt.datatransfer.DataFlavor[]{
                    java.awt.datatransfer.DataFlavor.imageFlavor
            };
        }

        @Override
        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
            return java.awt.datatransfer.DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(java.awt.datatransfer.DataFlavor flavor)
                throws java.awt.datatransfer.UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor))
                throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
            return image;
        }
    }

}
