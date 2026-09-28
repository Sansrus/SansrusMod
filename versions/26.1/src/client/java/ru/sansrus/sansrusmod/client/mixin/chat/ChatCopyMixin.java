package ru.sansrus.sansrusmod.client.mixin.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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

        List<GuiMessage.Line> lines = accessor.getTrimmedMessages();
        if (lines.isEmpty()) return;

        float scale = client.options.chatScale().get().floatValue();
        if (scale <= 0f) return;

        int lineHeight = (int) (9.0 * (client.options.chatLineSpacing().get() + 1.0));
        if (lineHeight <= 0) return;

        int screenH = client.getWindow().getGuiScaledHeight();
        int chatWidth = Mth.ceil(ChatComponent.getWidth(client.options.chatWidth().get()) / scale);
        int baseY = Mth.floor((screenH - 40) / scale);

        double localX = event.x() / scale - 4.0;
        double localY = event.y() / scale;

        if (localX < -4.0 || localX >= chatWidth + 8.0) return;

        double distFromBottom = baseY - localY;
        if (distFromBottom <= 0.0) return;

        int row = Mth.ceil(distFromBottom / lineHeight) - 1;
        int scrollPos = accessor.getChatScrollbarPos();
        int visibleRows = Math.min(lines.size() - scrollPos, chat.getLinesPerPage());
        if (row < 0 || row >= visibleRows) return;

        int index = row + scrollPos;
        if (index < 0 || index >= lines.size()) return;

        String text = lines.get(index).parent().content().getString();
        client.keyboardHandler.setClipboard(text);
        client.player.sendOverlayMessage(
                Component.translatable("sansrusmod.chat.messageCopied").withStyle(ChatFormatting.GRAY));
        cir.setReturnValue(true);
    }
}
