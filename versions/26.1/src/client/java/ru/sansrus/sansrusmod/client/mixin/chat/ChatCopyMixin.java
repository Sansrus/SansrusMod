package ru.sansrus.sansrusmod.client.mixin.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public class ChatCopyMixin {

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean bool, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.copyChatMessage) return;
        if (!event.hasShiftDown()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ChatComponent chat = client.gui.getChat();
        ChatComponentAccessor accessor = (ChatComponentAccessor) chat;
        List<GuiMessage> allMessages = accessor.getAllMessages();
        if (allMessages.isEmpty()) return;

        List<GuiMessage.Line> lines = accessor.getTrimmedMessages();
        int lineHeight = client.font.lineHeight;
        int linesPerPage = chat.getLinesPerPage();
        int chatHeight = linesPerPage * lineHeight;
        int chatWidth = ChatComponent.getWidth(client.getWindow().getGuiScaledWidth());
        int screenH = client.getWindow().getGuiScaledHeight();
        int chatX = 2;
        int chatAreaBottom = screenH - chatHeight;
        int inputFieldH = 40;

        double mx = event.x();
        double my = event.y();

        if (mx >= chatX && mx < chatX + chatWidth && my >= chatAreaBottom && my < screenH - inputFieldH) {
            int clickedLine = (int) ((screenH - inputFieldH - my - 1) / lineHeight);
            if (clickedLine >= 0 && clickedLine < lines.size()) {
                int msgIdx = 0;
                for (int i = 0; i <= clickedLine; i++) {
                    if (lines.get(i).endOfEntry()) msgIdx++;
                }
                msgIdx = Math.max(0, msgIdx - 1);
                if (msgIdx < allMessages.size()) {
                    String text = allMessages.get(msgIdx).content().getString();
                    client.keyboardHandler.setClipboard(text);
                    client.player.sendOverlayMessage(
                            Component.translatable("sansrusmod.chat.messageCopied").withStyle(ChatFormatting.GRAY));
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

    }
}