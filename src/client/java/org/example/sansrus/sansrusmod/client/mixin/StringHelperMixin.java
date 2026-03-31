package org.example.sansrus.sansrusmod.client.mixin;

import net.minecraft.util.StringHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringHelper.class)
public class StringHelperMixin {

    @Inject(method = "isValidChar", at = @At("HEAD"), cancellable = true)
    private static void allowSectionSign(char c, CallbackInfoReturnable<Boolean> cir) {
        if (c == '§') {
            cir.setReturnValue(true);
        }
    }
}
