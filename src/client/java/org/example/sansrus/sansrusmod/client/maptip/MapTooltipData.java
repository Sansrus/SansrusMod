package org.example.sansrus.sansrusmod.client.maptip;

import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipData;

public record MapTooltipData(MapIdComponent mapId, MapState state) implements TooltipData {}
