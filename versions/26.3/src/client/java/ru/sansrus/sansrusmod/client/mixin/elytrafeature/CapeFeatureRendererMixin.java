package ru.sansrus.sansrusmod.client.mixin.elytrafeature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class CapeFeatureRendererMixin {

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void cancelCapeWhenGliderPresent(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                              int i, AvatarRenderState avatarRenderState,
                                              float f, float g, CallbackInfo ci) {
        if (hasGliderComponent(avatarRenderState.chestEquipment)
                || hasGliderComponent(avatarRenderState.headEquipment)
                || hasGliderComponent(avatarRenderState.legsEquipment)
                || hasGliderComponent(avatarRenderState.feetEquipment)) {
            ci.cancel();
        }
    }
    
    @Unique
    private boolean hasGliderComponent(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.GLIDER);
    }
}
