package ru.sansrus.sansrusmod.client.mixin.deathlog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class DeathInventoryCaptureMixin {

    @Unique
    @Final private Minecraft client = Minecraft.getInstance();

    @Unique
    private boolean sansrus$caughtThisDeath = false;

    @Inject(method = "handleSetHealth", at = @At("HEAD"))
    private void sansrus$captureOnHealth(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;
        if (client.player == null) return;

        if (packet.getHealth() <= 0.0F && !sansrus$caughtThisDeath) {
            SansrusModClient.captureDeathSnapshotNow(client.player);
            sansrus$caughtThisDeath = true;
        } else if (packet.getHealth() > 0.0F) {
            sansrus$caughtThisDeath = false;
        }
    }

    @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"))
    private void sansrus$captureOnDeathMessage(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        if (!SansrusModClient.config.deathLogbool) return;
        if (client.player == null) return;

        if (packet.playerId() == client.player.getId() && !sansrus$caughtThisDeath) {
            SansrusModClient.captureDeathSnapshotNow(client.player);
            sansrus$caughtThisDeath = true;
        }
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void sansrus$resetDeathFlag(ClientboundRespawnPacket packet, CallbackInfo ci) {
        sansrus$caughtThisDeath = false;
    }
}
