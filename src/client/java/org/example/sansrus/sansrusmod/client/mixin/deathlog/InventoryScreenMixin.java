package org.example.sansrus.sansrusmod.client.mixin.deathlog;

import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.example.sansrus.sansrusmod.client.deathlog.DeathHistoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {

    @Unique
    private static final Identifier DEATH_BTN_TEX = Identifier.of("sansrusmod", "death_button");
    @Unique
    private static final Identifier DEATH_BTN_HLGD_TEX = Identifier.of("sansrusmod", "death_button_hover");

    @Unique
    private static final ButtonTextures TEXTURES = new ButtonTextures(
            DEATH_BTN_TEX,
            DEATH_BTN_HLGD_TEX
    );

    @Unique
    private TexturedButtonWidget sansrus$deathHistoryButton;

    public InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sansrus$addDeathHistoryButton(CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;

        // Удаляем старую кнопку, если она существует
        if (sansrus$deathHistoryButton != null) {
            this.remove(sansrus$deathHistoryButton);
        }

        int xPos = this.x + 104 + 22;
        int yPos = this.height / 2 - 22;

        sansrus$deathHistoryButton = new TexturedButtonWidget(
                xPos, yPos,
                18, 18,
                TEXTURES,
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(new DeathHistoryScreen(this));
                    }
                },
                Text.literal("Лог смертей")
        );

        this.addDrawableChild(sansrus$deathHistoryButton);
    }

    @Inject(method = "onRecipeBookToggled", at = @At("TAIL"))
    private void sansrus$updateButtonPosition(CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;
        if (sansrus$deathHistoryButton == null) return;

        // Обновляем позицию кнопки после переключения книги рецептов
        int xPos = this.x + 104 + 22;
        int yPos = this.height / 2 - 22;
        sansrus$deathHistoryButton.setPosition(xPos, yPos);
    }
}
