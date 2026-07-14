package ru.sansrus.sansrusmod.client.mixin.cyrilliccommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import ru.sansrus.sansrusmod.client.util.CyrillicCommandCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
@Mixin(ClientPacketListener.class)
public class ChatScreenMixin {

    @Unique
    private static final Map<Character, Character> CYRILLIC_TO_LATIN = new HashMap<>();

    static {
        CYRILLIC_TO_LATIN.put('й', 'q'); CYRILLIC_TO_LATIN.put('ц', 'w');
        CYRILLIC_TO_LATIN.put('у', 'e'); CYRILLIC_TO_LATIN.put('к', 'r');
        CYRILLIC_TO_LATIN.put('е', 't'); CYRILLIC_TO_LATIN.put('н', 'y');
        CYRILLIC_TO_LATIN.put('г', 'u'); CYRILLIC_TO_LATIN.put('ш', 'i');
        CYRILLIC_TO_LATIN.put('щ', 'o'); CYRILLIC_TO_LATIN.put('з', 'p');
        CYRILLIC_TO_LATIN.put('ф', 'a'); CYRILLIC_TO_LATIN.put('ы', 's');
        CYRILLIC_TO_LATIN.put('в', 'd'); CYRILLIC_TO_LATIN.put('а', 'f');
        CYRILLIC_TO_LATIN.put('п', 'g'); CYRILLIC_TO_LATIN.put('р', 'h');
        CYRILLIC_TO_LATIN.put('о', 'j'); CYRILLIC_TO_LATIN.put('л', 'k');
        CYRILLIC_TO_LATIN.put('д', 'l');
        CYRILLIC_TO_LATIN.put('я', 'z'); CYRILLIC_TO_LATIN.put('ч', 'x');
        CYRILLIC_TO_LATIN.put('с', 'c'); CYRILLIC_TO_LATIN.put('м', 'v');
        CYRILLIC_TO_LATIN.put('и', 'b'); CYRILLIC_TO_LATIN.put('т', 'n');
        CYRILLIC_TO_LATIN.put('ь', 'm');
    }

    @ModifyVariable(
            method = "sendCommand",
            at = @At("HEAD"),
            argsOnly = true
    )
    private String sansrus$interceptCommand(String command) {
        if (!SansrusModClient.config.cyrillicCommands) return command;
        return sansrus$convertSmart(command);
    }

    @Unique
    private String sansrus$convertSmart(String command) {
        if (command.isEmpty()) return command;

        List<StringRange> protectedRanges = CyrillicCommandCache.getProtectedRanges(command);
        if (protectedRanges != null) {
            String fullyTranslated = sansrus$translateAll(command);
            if (protectedRanges.isEmpty()) return fullyTranslated;
            return sansrus$restoreRangesFromCache(command, fullyTranslated, protectedRanges);
        }

        String parsedResult = sansrus$tryParse(command);
        if (parsedResult != null) return parsedResult;

        return sansrus$translateFirstWord(command);
    }

    @Unique
    private String sansrus$tryParse(String command) {
        CommandDispatcher dispatcher = sansrus$getDispatcher();
        if (dispatcher == null) return null;

        Object source = sansrus$getCommandSource();
        if (source == null) return null;

        String fullyTranslated = sansrus$translateAll(command);

        try {
            ParseResults parsed = dispatcher.parse(fullyTranslated, source);
            List<ParsedCommandNode> nodes = (List<ParsedCommandNode>) parsed.getContext().getNodes();
            if (nodes.isEmpty()) return null;

            List<StringRange> freeTextRanges = new ArrayList<>();
            collectFreeTextRanges(parsed.getContext(), freeTextRanges);

            if (freeTextRanges.isEmpty()) return fullyTranslated;

            return sansrus$restoreRangesNoShift(command, fullyTranslated, freeTextRanges);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private static void collectFreeTextRanges(CommandContextBuilder context,
                                              List<StringRange> ranges) {
        if (context == null) return;

        for (ParsedCommandNode parsed : (List<ParsedCommandNode>) context.getNodes()) {
            CommandNode node = parsed.getNode();
            if (node instanceof ArgumentCommandNode argNode) {
                Object argType = argNode.getType();
                if (isStringLikeType(argType)) {
                    ranges.add(parsed.getRange());
                }
            }
        }

        if (context.getChild() != null) {
            collectFreeTextRanges(context.getChild(), ranges);
        }
    }

    @Unique
    private static boolean isStringLikeType(Object argumentType) {
        if (argumentType instanceof StringArgumentType) return true;
        String className = argumentType.getClass().getName();
        return className.contains("MessageArgument")
                || className.contains("ComponentArgument");
    }

    @Unique
    private String sansrus$restoreRangesNoShift(String original, String translated,
                                                List<StringRange> protectedRanges) {
        StringBuilder out = new StringBuilder(translated.length());
        for (int i = 0; i < translated.length(); i++) {
            boolean protectedChar = false;
            for (StringRange range : protectedRanges) {
                if (i >= range.getStart() && i < range.getEnd()) {
                    protectedChar = true;
                    break;
                }
            }
            out.append(protectedChar ? original.charAt(i) : translated.charAt(i));
        }
        return out.toString();
    }

    @Unique
    private String sansrus$restoreRangesFromCache(String original, String translated,
                                                  List<StringRange> protectedRanges) {
        StringBuilder out = new StringBuilder(translated.length());
        for (int i = 0; i < translated.length(); i++) {
            boolean protectedChar = false;
            for (StringRange range : protectedRanges) {
                int start = range.getStart() - 1;
                int end = range.getEnd() - 1;
                if (i >= start && i < end) {
                    protectedChar = true;
                    break;
                }
            }
            out.append(protectedChar ? original.charAt(i) : translated.charAt(i));
        }
        return out.toString();
    }

    @Unique
    private String sansrus$translateAll(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            out.append(CYRILLIC_TO_LATIN.getOrDefault(c, c));
        }
        return out.toString();
    }

    @Unique
    private String sansrus$translateFirstWord(String command) {
        int firstSpace = command.indexOf(' ');
        if (firstSpace == -1) return sansrus$translateAll(command);
        String first = sansrus$translateAll(command.substring(0, firstSpace));
        return first + command.substring(firstSpace);
    }

    @Unique
    private Object sansrus$getCommandSource() {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener handler = client.getConnection();
        if (handler == null) return null;

        try {
            Field f = ClientPacketListener.class.getDeclaredField("commandSource");
            f.setAccessible(true);
            return f.get(handler);
        } catch (Exception ignored) {}

        return null;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private CommandDispatcher sansrus$getDispatcher() {
        try {
            Field f = ClientPacketListener.class.getDeclaredField("commandDispatcher");
            f.setAccessible(true);
            return (CommandDispatcher) f.get(this);
        } catch (Exception e) {
            return null;
        }
    }
}
