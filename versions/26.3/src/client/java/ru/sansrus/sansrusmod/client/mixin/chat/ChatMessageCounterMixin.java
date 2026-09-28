package ru.sansrus.sansrusmod.client.mixin.chat;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.ChatFormatting;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import ru.sansrus.sansrusmod.client.chatcoord.ChatPipelineFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatMessageCounterMixin {

    @Shadow private List<GuiMessage> allMessages;
    @Shadow private List<GuiMessage.Line> trimmedMessages;

    @Unique private String  sansrus$lastRawText = null;
    @Unique private int     sansrus$count       = 1;
    @Unique private boolean sansrus$injecting   = false;

    @Shadow
    private void addMessage(Component message, MessageSignature signatureData,
                            GuiMessageSource source, GuiMessageTag indicator) {
        throw new AssertionError();
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sansrus$countDuplicates(Component message, MessageSignature signatureData,
                                         GuiMessageSource source, GuiMessageTag indicator,
                                         CallbackInfo ci) {
        if (!SansrusModClient.config.chatMessageCounter) return;
        if (sansrus$injecting) return;
        if (ChatPipelineFlags.coordReinjecting) return;
        if (source == GuiMessageSource.SYSTEM_CLIENT) return;

        String incoming = message.getString();

        if (!incoming.equals(sansrus$lastRawText)) {
            sansrus$lastRawText = incoming;
            sansrus$count = 1;
            return;
        }

        sansrus$count++;

        if (!allMessages.isEmpty()) {
            GuiMessage previous = allMessages.remove(0);
            trimmedMessages.removeIf(line -> line.parent() == previous);
        }

        Component counted = Component.empty()
                .append(message)
                .append(Component.literal(" §7*" + sansrus$count).withStyle(ChatFormatting.GRAY));

        ci.cancel();
        sansrus$injecting = true;
        addMessage(counted, signatureData, source, indicator);
        sansrus$injecting = false;
    }
}
