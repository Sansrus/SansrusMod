package ru.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin {

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    private double toChatLineY(double y) {
        return 0;
    }

    @Shadow
    private double toChatLineX(double x) {
        return 0;
    }

    @Shadow
    private int getMessageLineIndex(double chatLineX, double chatLineY) {
        return 0;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!SansrusModClient.config.copyChatMessage) return;
        
        long window = Minecraft.getInstance().getWindow().handle();
        boolean shiftPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS 
                            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        
        if (shiftPressed) {
            double chatLineX = toChatLineX(mouseX);
            double chatLineY = toChatLineY(mouseY);

            int lineIndex = this.getMessageLineIndex(chatLineX, chatLineY);

            if (lineIndex >= 0 && lineIndex < this.trimmedMessages.size()) {
                int messageIndex = 0;

                for (int i = 0; i <= lineIndex; i++) {
                    if (this.trimmedMessages.get(i).endOfEntry()) {
                        messageIndex++;
                    }
                }

                messageIndex = Math.max(0, messageIndex - 1);

                if (messageIndex < this.allMessages.size()) {
                    GuiMessage message = this.allMessages.get(messageIndex);
                    String messageText = message.content().getString();

                    Minecraft.getInstance().keyboardHandler.setClipboard(messageText);

                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.sendSystemMessage(
                                Component.translatable("sansrusmod.chat.messageCopied").withStyle(ChatFormatting.GRAY)
                        );
                    }

                    cir.setReturnValue(true);
                }
            }
        }
    }
}
