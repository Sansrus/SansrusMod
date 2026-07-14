package ru.sansrus.sansrusmod.client.mixin.chat;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.ChatFormatting;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import ru.sansrus.sansrusmod.client.chatcoord.ChatPipelineFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatMessageCounterMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("SansrusMod/ChatCounter");
    
    @Shadow private List<GuiMessage> allMessages;
    @Shadow private List<GuiMessage.Line> trimmedMessages;

    @Unique private String  sansrus$lastRawText = null;
    @Unique private int     sansrus$count       = 1;
    @Unique private boolean sansrus$injecting   = false;

    @Shadow
    public abstract void addPlayerMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator);

    @Inject(
            method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sansrus$countDuplicates(Component message, MessageSignature signatureData,
                                         GuiMessageTag indicator, CallbackInfo ci) {
        if (!SansrusModClient.config.chatMessageCounter) return;
        if (sansrus$injecting) return;
        if (ChatPipelineFlags.coordReinjecting) return;

        String incoming = message.getString();

        if (incoming.equals(sansrus$lastRawText)) {
            sansrus$count++;

            if (!allMessages.isEmpty()) {
                allMessages.remove(0);
            }
            if (!trimmedMessages.isEmpty()) {
                trimmedMessages.remove(0);
            }

            Component counted = Component.empty()
                    .copy()
                    .append(message)
                    .append(Component.literal(" §7*" + sansrus$count).withStyle(ChatFormatting.GRAY));

            ci.cancel();
            sansrus$injecting = true;
            addPlayerMessage(counted, signatureData, indicator);
            sansrus$injecting = false;

        } else {
            sansrus$lastRawText = incoming;
            sansrus$count = 1;
        }
    }
}
