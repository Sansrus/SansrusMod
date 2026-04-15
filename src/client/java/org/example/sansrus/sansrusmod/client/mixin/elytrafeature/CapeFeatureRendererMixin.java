package org.example.sansrus.sansrusmod.client.mixin.elytrafeature;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelCapeWhenGliderPresent(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, 
                                              int i, PlayerEntityRenderState playerEntityRenderState,
                                              float f, float g, CallbackInfo ci) {
        if (!SansrusModClient.config.elytrafeaturerender) return;

        if (hasGliderComponent(playerEntityRenderState.equippedChestStack)
                || hasGliderComponent(playerEntityRenderState.equippedHeadStack)
                || hasGliderComponent(playerEntityRenderState.equippedLegsStack)
                || hasGliderComponent(playerEntityRenderState.equippedFeetStack)) {
            ci.cancel();
        }
    }
    
    @Unique
    private boolean hasGliderComponent(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.contains(DataComponentTypes.GLIDER);
    }
}
