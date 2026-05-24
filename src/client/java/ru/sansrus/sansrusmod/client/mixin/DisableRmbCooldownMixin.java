package ru.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.Minecraft;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class DisableRmbCooldownMixin {

    @Shadow
    private int rightClickDelay;
    @Unique
    private int rightClickHoldTicks = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        int threshold = SansrusModClient.config.rmbCooldownThreshold;
        Minecraft client = (Minecraft) (Object) this;

        if (threshold <= 0) {
            if (client.options.keyUse.isDown()) {
                rightClickDelay = 0;
            }
            return;
        }

        if (client.options.keyUse.isDown()) {
            rightClickHoldTicks++;
            if (rightClickHoldTicks > threshold) {
                rightClickDelay = 0;
            }
        } else {
            rightClickHoldTicks = 0;
        }
    }
}
