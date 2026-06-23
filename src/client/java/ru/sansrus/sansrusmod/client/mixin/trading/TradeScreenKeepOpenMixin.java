package ru.sansrus.sansrusmod.client.mixin.trading;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class TradeScreenKeepOpenMixin {

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void sansrus$keepTradeScreenOpen(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        if (!SansrusModClient.config.shadowTrader) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MerchantScreen) {
            ci.cancel();
        }
    }
}
