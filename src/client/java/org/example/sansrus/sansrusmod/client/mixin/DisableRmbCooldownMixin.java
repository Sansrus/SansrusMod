package org.example.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class DisableRmbCooldownMixin {

    @Shadow
    private int itemUseCooldown;

    // Счетчик тиков удержания ПКМ
    @Unique
    private int rightClickHoldTicks = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!SansrusModClient.config.disableRmbCooldown) return;
        MinecraftClient client = (MinecraftClient) (Object) this;

        // Проверяем, зажата ли правая кнопка мыши через options
        if (client.options.useKey.isPressed()) {
            rightClickHoldTicks++;

            // Если ПКМ зажата больше 20 тиков (1 секунда)
            if (rightClickHoldTicks > 20) {
                // Обнуляем задержку использования предмета
                itemUseCooldown = 0;
            }
        } else {
            // Сбрасываем счетчик, если кнопка отпущена
            rightClickHoldTicks = 0;
        }
    }
}
