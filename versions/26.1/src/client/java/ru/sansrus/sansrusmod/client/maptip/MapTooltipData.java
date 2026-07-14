package ru.sansrus.sansrusmod.client.maptip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public record MapTooltipData(MapId mapId, MapItemSavedData state) implements TooltipComponent {}
