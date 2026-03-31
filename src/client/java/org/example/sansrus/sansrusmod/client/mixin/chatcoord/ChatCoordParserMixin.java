package org.example.sansrus.sansrusmod.client.mixin.chatcoord;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.*;
import net.minecraft.text.PlainTextContent;
import net.minecraft.util.Formatting;
import org.example.sansrus.sansrusmod.client.chatcoord.ChatPipelineFlags;
import org.example.sansrus.sansrusmod.client.chatcoord.CoordParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ChatHud.class)
public abstract class ChatCoordParserMixin {

    @Shadow
    public abstract void addMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator);

    @Unique
    private boolean sansrus$parsingCoords = false;

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true
    )
    private void sansrus$parseCoords(Text message, MessageSignatureData signatureData,
                                     MessageIndicator indicator, CallbackInfo ci) {
        if (sansrus$parsingCoords) return;
        if (ChatPipelineFlags.deathMessageActive) return;
        if (ChatPipelineFlags.coordReinjecting) return;

        List<CoordParser.CoordMatch> matches = CoordParser.findAll(message.getString());
        if (matches.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();

        MutableText result = sansrus$transformNode(message);

        ci.cancel();
        sansrus$parsingCoords = true;
        ChatPipelineFlags.coordReinjecting = true;
        addMessage(result, signatureData, indicator);
        ChatPipelineFlags.coordReinjecting = false;
        sansrus$parsingCoords = false;
    }

    @Unique
    private static MutableText sansrus$transformNode(Text node) {
        TextContent content = node.getContent();
        Style style = node.getStyle();
        MutableText out;

        if (content instanceof PlainTextContent plain) {
            out = sansrus$transformLiteralLocal(plain.string(), style);

        } else if (content instanceof TranslatableTextContent tr) {
            Object[] newArgs = Arrays.stream(tr.getArgs())
                    .map(arg -> {
                        if (arg instanceof Text textArg) {
                            return sansrus$transformNode(textArg);
                        }
                        if (arg instanceof String s) {
                            return sansrus$transformLiteralLocal(s, style);
                        }
                        return arg;
                    })
                    .toArray();

            out = Text.translatableWithFallback(tr.getKey(), tr.getFallback(), newArgs)
                    .setStyle(style);
        } else {
            out = MutableText.of(content).setStyle(style);
        }

        for (Text sibling : node.getSiblings()) {
            out.append(sansrus$transformNode(sibling));
        }

        return out;
    }

    @Unique
    private static MutableText sansrus$transformLiteralLocal(String string, Style style) {
        List<CoordParser.CoordMatch> matches = CoordParser.findAll(string);
        if (matches.isEmpty()) {
            return Text.literal(string).setStyle(style);
        }

        int local = 0;
        MutableText out = Text.empty().setStyle(style);

        for (CoordParser.CoordMatch m : matches) {
            int mStart = m.start;
            int mEnd = m.end;

            if (mStart > local) {
                out.append(Text.literal(string.substring(local, mStart)).setStyle(style));
            }

            int clickY = m.hasY ? m.y : 64;
            String cmd = String.format("/create_coord_waypoint %d %d %d", m.x, clickY, m.z);
            Text hov = m.hasY
                    ? Text.translatable("sansrusmod.coordParser.createWaypoint", m.x, m.y, m.z)
                    : Text.translatable("sansrusmod.coordParser.createWaypointNoY", m.x, m.z);

            out.append(
                    Text.literal(string.substring(mStart, mEnd))
                            .setStyle(style)
                            .formatted(Formatting.AQUA)
                            .styled(st -> st
                                    .withUnderline(true)
                                    .withClickEvent(new ClickEvent.RunCommand(cmd))
                                    .withHoverEvent(new HoverEvent.ShowText(hov)))
            );

            local = mEnd;
        }

        if (local < string.length()) {
            out.append(Text.literal(string.substring(local)).setStyle(style));
        }

        return out;
    }
}
