package ru.sansrus.sansrusmod.client.mixin.deathlog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import ru.sansrus.sansrusmod.client.deathlog.DeathHistoryScreen;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @Unique
    private static final Identifier DEATH_BTN_TEX = Identifier.fromNamespaceAndPath("sansrusmod", "death_button");
    @Unique
    private static final Identifier DEATH_BTN_HLGD_TEX = Identifier.fromNamespaceAndPath("sansrusmod", "death_button_hover");
    @Unique
    private Button sansrus$deathHistoryButton;

    public InventoryScreenMixin(InventoryMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sansrus$addDeathHistoryButton(CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;

        if (sansrus$deathHistoryButton != null) {
            this.removeWidget(sansrus$deathHistoryButton);
        }

        int xPos = this.leftPos + 104 + 22;
        int yPos = this.height / 2 - 22;

        sansrus$deathHistoryButton = new Button(
                xPos, yPos,
                18, 18,
                Component.literal("Лог смертей"),
                btn -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null) {
                        mc.setScreen(new DeathHistoryScreen(this));
                    }
                },
                (btn) -> Component.empty()
        ) {
            @Override
            protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
                Identifier tex = this.isHoveredOrFocused() ? DEATH_BTN_HLGD_TEX : DEATH_BTN_TEX;
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), this.width, this.height);
            }
        };

        this.addRenderableWidget(sansrus$deathHistoryButton);
    }

    @Inject(method = "onRecipeBookButtonClick", at = @At("TAIL"))
    private void sansrus$updateButtonPosition(CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;
        if (sansrus$deathHistoryButton == null) return;

        int xPos = this.leftPos + 104 + 22;
        int yPos = this.height / 2 - 22;
        sansrus$deathHistoryButton.setPosition(xPos, yPos);
    }
}
