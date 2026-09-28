package ru.sansrus.sansrusmod.client.mixin.maptip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import ru.sansrus.sansrusmod.client.SansrusModClient;
import ru.sansrus.sansrusmod.client.maptip.MapTooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackMapTooltipMixin {

    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void sansrus$injectMapPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() == Items.FILLED_MAP)) return;
        if (!SansrusModClient.config.tooltipmap) return;

        MapId mapId = self.get(DataComponents.MAP_ID);
        if (mapId == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        MapItemSavedData state = mc.level.getMapData(mapId);
        if (state == null) return;

        cir.setReturnValue(Optional.of(new MapTooltipData(mapId, state)));
    }
}
