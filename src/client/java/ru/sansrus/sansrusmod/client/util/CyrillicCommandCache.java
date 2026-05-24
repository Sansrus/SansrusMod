package ru.sansrus.sansrusmod.client.util;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class CyrillicCommandCache {

    private static final Map<String, List<StringRange>> PROTECTED_RANGES = new HashMap<>();

    public static List<StringRange> getProtectedRanges(String input) {
        if (input == null) return null;
        return PROTECTED_RANGES.get(normalizeKey(input));
    }

    public static void cacheRanges(String input, CommandContextBuilder context) {
        if (input == null || context == null) return;

        input = normalizeKey(input);

        List<StringRange> freeTextRanges = new ArrayList<>();
        collectFreeTextRanges(context, freeTextRanges);

        PROTECTED_RANGES.put(input, freeTextRanges);
    }

    public static void remove(String input) {
        if (input == null) return;
        PROTECTED_RANGES.remove(normalizeKey(input));
    }

    public static String normalizeKey(String input) {
        if (input == null) return null;
        return input.startsWith("/") ? input : "/" + input;
    }

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

    private static boolean isStringLikeType(Object argumentType) {
        if (argumentType instanceof StringArgumentType) return true;
        String className = argumentType.getClass().getName();
        return className.contains("MessageArgument")
                || className.contains("ComponentArgument");
    }
}
