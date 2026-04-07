package org.example.sansrus.sansrusmod.client.mixin.cyrilliccommands;

import net.minecraft.client.gui.screen.ChatScreen;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashMap;
import java.util.Map;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Unique
    private static final Map<Character, Character> CYRILLIC_TO_LATIN = new HashMap<>();

    static {
        // Ряд 1
        CYRILLIC_TO_LATIN.put('й', 'q'); CYRILLIC_TO_LATIN.put('ц', 'w');
        CYRILLIC_TO_LATIN.put('у', 'e'); CYRILLIC_TO_LATIN.put('к', 'r');
        CYRILLIC_TO_LATIN.put('е', 't'); CYRILLIC_TO_LATIN.put('н', 'y');
        CYRILLIC_TO_LATIN.put('г', 'u'); CYRILLIC_TO_LATIN.put('ш', 'i');
        CYRILLIC_TO_LATIN.put('щ', 'o'); CYRILLIC_TO_LATIN.put('з', 'p');
        // Ряд 2
        CYRILLIC_TO_LATIN.put('ф', 'a'); CYRILLIC_TO_LATIN.put('ы', 's');
        CYRILLIC_TO_LATIN.put('в', 'd'); CYRILLIC_TO_LATIN.put('а', 'f');
        CYRILLIC_TO_LATIN.put('п', 'g'); CYRILLIC_TO_LATIN.put('р', 'h');
        CYRILLIC_TO_LATIN.put('о', 'j'); CYRILLIC_TO_LATIN.put('л', 'k');
        CYRILLIC_TO_LATIN.put('д', 'l');
        // Ряд 3
        CYRILLIC_TO_LATIN.put('я', 'z'); CYRILLIC_TO_LATIN.put('ч', 'x');
        CYRILLIC_TO_LATIN.put('с', 'c'); CYRILLIC_TO_LATIN.put('м', 'v');
        CYRILLIC_TO_LATIN.put('и', 'b'); CYRILLIC_TO_LATIN.put('т', 'n');
        CYRILLIC_TO_LATIN.put('ь', 'm');
    }

    @ModifyVariable(
            method = "sendMessage",
            at = @At("HEAD"),
            argsOnly = true
    )
    private String convertCyrillicBeforeSend(String message) {
        if (!SansrusModClient.config.cyrillicCommands) return message;
        return sansrus$convertCommandInput(message);
    }

    @Unique
    private static String sansrus$convertCommandInput(String input) {
        if (input.isEmpty() || input.charAt(0) != '/') {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            result.append(CYRILLIC_TO_LATIN.getOrDefault(c, c));
        }

        return result.toString();
    }
}
