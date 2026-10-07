package com.dashfps.mixin;

import com.dashfps.CullingUtil;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashfps$smartEntityCull(
        Entity entity,
        Frustum frustum,
        double camX,
        double camY,
        double camZ,
        float partialTicks,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (CullingUtil.shouldCullEntity(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
}
