package org.example.sansrus.sansrusmod.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow
    @Final
    private List<ChatHudLine> messages;

    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;

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
        
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        boolean shiftPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS 
                            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        
        if (shiftPressed) {
            double chatLineX = toChatLineX(mouseX);
            double chatLineY = toChatLineY(mouseY);

            int lineIndex = this.getMessageLineIndex(chatLineX, chatLineY);

            if (lineIndex >= 0 && lineIndex < this.visibleMessages.size()) {
                int messageIndex = 0;

                for (int i = 0; i <= lineIndex; i++) {
                    if (this.visibleMessages.get(i).endOfEntry()) {
                        messageIndex++;
                    }
                }

                messageIndex = Math.max(0, messageIndex - 1);

                if (messageIndex < this.messages.size()) {
                    ChatHudLine message = this.messages.get(messageIndex);
                    String messageText = message.content().getString();

                    MinecraftClient.getInstance().keyboard.setClipboard(messageText);

                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(
                                Text.translatable("sansrusmod.chat.messageCopied").formatted(Formatting.GRAY),
                                true
                        );
                    }

                    cir.setReturnValue(true);
                }
            }
        }
    }
}
