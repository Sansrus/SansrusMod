package org.example.sansrus.sansrusmod.client.mixin.nauseafix;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class NauseaOverlayFixMixin {
    
    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelNauseaOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        // Отменяем рендер nausea overlay если включен флаг в конфиге
        if (SansrusModClient.config.disableNauseaOverlay) {
            ci.cancel();
        }
    }
}
