package com.dashfps.mixin;

import com.dashfps.DashFPS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRendererMixin {
    @Inject(
        method = "tryExtractRenderState",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void dashfps$cullDistantBlockEntities(
        BlockEntity blockEntity,
        float partialTicks,
        ModelFeatureRenderer.CrumblingOverlay breakProgress,
        boolean isGloballyRendered,
        CallbackInfoReturnable<BlockEntityRenderState> cir
    ) {
        if (dashfps$shouldCull(blockEntity)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
        method = "tryExtractRenderState",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void dashfps$cullDistantBlockEntitiesWithFrustum(
        BlockEntity blockEntity,
        float partialTicks,
        ModelFeatureRenderer.CrumblingOverlay breakProgress,
        boolean isGloballyRendered,
        Frustum frustum,
        CallbackInfoReturnable<BlockEntityRenderState> cir
    ) {
        if (dashfps$shouldCull(blockEntity)) {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private static boolean dashfps$shouldCull(BlockEntity blockEntity) {
        if (!DashFPS.isAggressiveCullingActive()) {
            return false;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Vec3 center = Vec3.atCenterOf(blockEntity.getBlockPos());
        return center.distanceToSqr(camera) > 16.0 * 16.0;
    }
}
