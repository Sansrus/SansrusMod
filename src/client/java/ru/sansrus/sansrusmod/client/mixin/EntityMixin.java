package ru.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ru.sansrus.sansrusmod.client.SansrusModClient;
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
        if ((Object)this instanceof Player) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void cancelInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.disableInvisibility) return;
        if (!isAllowedPlayer()) return;
        if ((Object)this instanceof Player) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    private boolean isAllowedPlayer() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        String playerName = client.player.getName().getString();
        return playerName.equals("Sansrus") || playerName.equals("EN403");
    }
}
