package org.example.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class VillagerProtectMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void sansrus$protectVillager(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (!SansrusModClient.config.protectVillage) return;
        if (target instanceof VillagerEntity && !player.isSneaking()) {
            ci.cancel();
        }
    }
}
