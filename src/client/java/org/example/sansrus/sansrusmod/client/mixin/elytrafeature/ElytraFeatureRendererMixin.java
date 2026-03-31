package org.example.sansrus.sansrusmod.client.mixin.elytrafeature;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.example.sansrus.sansrusmod.client.SansrusModClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraFeatureRendererMixin<S extends BipedEntityRenderState, M> {

    @Final
    @Shadow
    private ElytraEntityModel model;
    @Final
    @Shadow
    private ElytraEntityModel babyModel;
    @Final
    @Shadow
    private EquipmentRenderer equipmentRenderer;

    @Inject(method = "render*", at = @At("TAIL"))
    private void onRender(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i,
                          S bipedEntityRenderState, float f, float g, CallbackInfo ci) {
        if (!SansrusModClient.config.elytrafeaturerender) return;

        // Проверяем все слоты экипировки на наличие компонента GLIDER
        ItemStack gliderStack = null;
        
        if (bipedEntityRenderState.equippedChestStack != null 
                && !bipedEntityRenderState.equippedChestStack.isEmpty()
                && bipedEntityRenderState.equippedChestStack.contains(DataComponentTypes.GLIDER)) {
            gliderStack = bipedEntityRenderState.equippedChestStack;
        } else if (bipedEntityRenderState.equippedHeadStack != null 
                && !bipedEntityRenderState.equippedHeadStack.isEmpty()
                && bipedEntityRenderState.equippedHeadStack.contains(DataComponentTypes.GLIDER)) {
            gliderStack = bipedEntityRenderState.equippedHeadStack;
        } else if (bipedEntityRenderState.equippedLegsStack != null 
                && !bipedEntityRenderState.equippedLegsStack.isEmpty()
                && bipedEntityRenderState.equippedLegsStack.contains(DataComponentTypes.GLIDER)) {
            gliderStack = bipedEntityRenderState.equippedLegsStack;
        } else if (bipedEntityRenderState.equippedFeetStack != null 
                && !bipedEntityRenderState.equippedFeetStack.isEmpty()
                && bipedEntityRenderState.equippedFeetStack.contains(DataComponentTypes.GLIDER)) {
            gliderStack = bipedEntityRenderState.equippedFeetStack;
        }

        if (gliderStack == null) return;

        ElytraEntityModel elytraModel = bipedEntityRenderState.baby ? this.babyModel : this.model;
        elytraModel.setAngles(bipedEntityRenderState);

        ItemStack elytraStack = Items.ELYTRA.getDefaultStack();
        EquippableComponent equippable = elytraStack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) return;

        RegistryKey<EquipmentAsset> assetId = equippable.assetId().get();
        Identifier texture = getTextureFromState(bipedEntityRenderState);

        matrixStack.push();
        matrixStack.translate(0.0F, 0.0F, 0.125F);
        this.equipmentRenderer.render(
                EquipmentModel.LayerType.WINGS,
                assetId,
                elytraModel,
                gliderStack,
                matrixStack,
                vertexConsumerProvider,
                i,
                texture
        );
        matrixStack.pop();
    }

    @Unique
    private static Identifier getTextureFromState(BipedEntityRenderState state) {
        if (state instanceof PlayerEntityRenderState playerState) {
            SkinTextures skinTextures = playerState.skinTextures;
            if (skinTextures.elytraTexture() != null) {
                return skinTextures.elytraTexture();
            }
            if (skinTextures.capeTexture() != null && playerState.capeVisible) {
                return skinTextures.capeTexture();
            }
        }
        return null;
    }
}
