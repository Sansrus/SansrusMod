package org.example.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void cancelInvisibility(CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.disableInvisibility) return;
        if (!isAllowedPlayer()) return;
        if ((Object)this instanceof PlayerEntity) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void cancelInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.disableInvisibility) return;
        if (!isAllowedPlayer()) return;
        if ((Object)this instanceof PlayerEntity) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    private boolean isAllowedPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        String playerName = client.player.getName().getString();
        return playerName.equals("Sansrus") || playerName.equals("EN403");
    }
}
