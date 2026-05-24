package org.example.sansrus.sansrusmod.client.mixin.chat;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.example.sansrus.sansrusmod.client.chatcoord.ChatPipelineFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatMessageCounterMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("SansrusMod/ChatCounter");
    
    @Shadow private List<ChatHudLine> messages;
    @Shadow private List<ChatHudLine.Visible> visibleMessages;

    @Unique private String  sansrus$lastRawText = null;
    @Unique private int     sansrus$count       = 1;
    @Unique private boolean sansrus$injecting   = false;

    @Shadow
    public abstract void addMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator);

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sansrus$countDuplicates(Text message, MessageSignatureData signatureData,
                                         MessageIndicator indicator, CallbackInfo ci) {
        if (!SansrusModClient.config.chatMessageCounter) return;
        if (sansrus$injecting) return;
        if (ChatPipelineFlags.coordReinjecting) return;

        String incoming = message.getString();

        if (incoming.equals(sansrus$lastRawText)) {
            sansrus$count++;

            if (!messages.isEmpty()) {
                messages.remove(0);
            }
            if (!visibleMessages.isEmpty()) {
                visibleMessages.remove(0);
            }

            Text counted = Text.empty()
                    .copy()
                    .append(message)
                    .append(Text.literal(" §7*" + sansrus$count).formatted(Formatting.GRAY));

            ci.cancel();
            sansrus$injecting = true;
            addMessage(counted, signatureData, indicator);
            sansrus$injecting = false;

        } else {
            sansrus$lastRawText = incoming;
            sansrus$count = 1;
        }
    }
}
