package org.example.sansrus.sansrusmod.client.mixin.deathlog;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class DeathInventoryCaptureMixin {

    @Unique
    @Final private MinecraftClient client = MinecraftClient.getInstance();

    @Unique
    private boolean sansrus$caughtThisDeath = false;

    @Inject(method = "onHealthUpdate", at = @At("HEAD"))
    private void sansrus$captureOnHealth(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;
        if (client.player == null) return;

        if (packet.getHealth() <= 0.0F && !sansrus$caughtThisDeath) {
            SansrusModClient.captureDeathSnapshotNow(client.player);
            sansrus$caughtThisDeath = true;
        } else if (packet.getHealth() > 0.0F) {
            sansrus$caughtThisDeath = false;
        }
    }

    @Inject(method = "onDeathMessage", at = @At("HEAD"))
    private void sansrus$captureOnDeathMessage(DeathMessageS2CPacket packet, CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;
        if (client.player == null) return;

        if (packet.playerId() == client.player.getId() && !sansrus$caughtThisDeath) {
            SansrusModClient.captureDeathSnapshotNow(client.player);
            sansrus$caughtThisDeath = true;
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void sansrus$resetDeathFlag(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        sansrus$caughtThisDeath = false;
    }
}
