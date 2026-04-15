package org.example.sansrus.sansrusmod.client.mixin.cyrilliccommands;

import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

@Mixin(ChatInputSuggestor.class)
public class ChatInputSuggestorMixin {

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

    @Redirect(
            method = "refresh",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getText()Ljava/lang/String;"
            )
    )
    private String redirectGetTextForSuggestions(TextFieldWidget textField) {
        String original = textField.getText();
        if (!SansrusModClient.config.cyrillicCommands) return original;
        return convertCommandInput(original);
    }

    @Unique
    private static String convertCommandInput(String input) {
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
