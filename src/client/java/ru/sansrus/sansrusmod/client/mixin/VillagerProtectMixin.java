package ru.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class VillagerProtectMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void sansrus$protectVillager(Entity target, CallbackInfo ci) {
        if (!SansrusModClient.config.protectVillage) return;
        if (target.getType() == net.minecraft.world.entity.EntityType.VILLAGER
                && !Minecraft.getInstance().options.keyShift.isDown()) {
            ci.cancel();
        }
    }
}
