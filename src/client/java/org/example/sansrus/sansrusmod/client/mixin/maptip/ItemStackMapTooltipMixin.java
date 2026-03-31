package org.example.sansrus.sansrusmod.client.mixin.maptip;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipData;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.example.sansrus.sansrusmod.client.maptip.MapTooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackMapTooltipMixin {

    @Inject(method = "getTooltipData", at = @At("HEAD"), cancellable = true)
    private void sansrus$injectMapPreview(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        ItemStack self = (ItemStack) (Object) this;

        // Проверяем, что предмет — это карта
        if (!(self.getItem() instanceof FilledMapItem)) return;
        if (!SansrusModClient.config.tooltipmap) return;

        MapIdComponent mapId = self.get(DataComponentTypes.MAP_ID);
        if (mapId == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        MapState state = mc.world.getMapState(mapId);
        if (state == null) return;

        cir.setReturnValue(Optional.of(new MapTooltipData(mapId, state)));
    }
}
