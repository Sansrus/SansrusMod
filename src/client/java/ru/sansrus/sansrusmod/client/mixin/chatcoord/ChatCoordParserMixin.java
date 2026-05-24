package ru.sansrus.sansrusmod.client.mixin.chatcoord;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import ru.sansrus.sansrusmod.client.chatcoord.ChatPipelineFlags;
import ru.sansrus.sansrusmod.client.chatcoord.CoordParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatCoordParserMixin {

    @Shadow
    public abstract void addPlayerMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator);

    @Unique
    private boolean sansrus$parsingCoords = false;

    @Inject(
            method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true
    )
    private void sansrus$parseCoords(Component message, MessageSignature signatureData,
                                     GuiMessageTag indicator, CallbackInfo ci) {
        if (sansrus$parsingCoords) return;
        if (ChatPipelineFlags.deathMessageActive) return;
        if (ChatPipelineFlags.coordReinjecting) return;
        if (!SansrusModClient.isXaeroMinimapLoaded) return;

        List<CoordParser.CoordMatch> matches = CoordParser.findAll(message.getString());
        if (matches.isEmpty()) return;

        MutableComponent result = sansrus$transformNode(message);

        ci.cancel();
        sansrus$parsingCoords = true;
        ChatPipelineFlags.coordReinjecting = true;
        addPlayerMessage(result, signatureData, indicator);
        ChatPipelineFlags.coordReinjecting = false;
        sansrus$parsingCoords = false;
    }

    @Unique
    private static MutableComponent sansrus$transformNode(Component node) {
        ComponentContents content = node.getContents();
        Style style = node.getStyle();
        MutableComponent out;

        if (content instanceof PlainTextContents plain) {
            out = sansrus$transformLiteralLocal(plain.text(), style);

        } else if (content instanceof TranslatableContents tr) {
            Object[] newArgs = Arrays.stream(tr.getArgs())
                    .map(arg -> {
                        if (arg instanceof Component textArg) {
                            return sansrus$transformNode(textArg);
                        }
                        if (arg instanceof String s) {
                            return sansrus$transformLiteralLocal(s, style);
                        }
                        return arg;
                    })
                    .toArray();

            out = Component.translatableWithFallback(tr.getKey(), tr.getFallback(), newArgs)
                    .setStyle(style);
        } else {
            out = MutableComponent.create(content).setStyle(style);
        }

        for (Component sibling : node.getSiblings()) {
            out.append(sansrus$transformNode(sibling));
        }

        return out;
    }

    @Unique
    private static MutableComponent sansrus$transformLiteralLocal(String string, Style style) {
        List<CoordParser.CoordMatch> matches = CoordParser.findAll(string);
        if (matches.isEmpty()) {
            return Component.literal(string).setStyle(style);
        }

        int local = 0;
        MutableComponent out = Component.empty().setStyle(style);

        for (CoordParser.CoordMatch m : matches) {
            int mStart = m.start;
            int mEnd = m.end;

            if (mStart > local) {
                out.append(Component.literal(string.substring(local, mStart)).setStyle(style));
            }

            int clickY = m.hasY ? m.y : 64;
            String cmd = String.format("/create_coord_waypoint %d %d %d", m.x, clickY, m.z);
            Component hov = m.hasY
                    ? Component.translatable("sansrusmod.coordParser.createWaypoint", m.x, m.y, m.z)
                    : Component.translatable("sansrusmod.coordParser.createWaypointNoY", m.x, m.z);

            out.append(
                    Component.literal(string.substring(mStart, mEnd))
                            .setStyle(style)
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(st -> st
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent.RunCommand(cmd))
                                    .withHoverEvent(new HoverEvent.ShowText(hov)))
            );

            local = mEnd;
        }

        if (local < string.length()) {
            out.append(Component.literal(string.substring(local)).setStyle(style));
        }

        return out;
    }
}
