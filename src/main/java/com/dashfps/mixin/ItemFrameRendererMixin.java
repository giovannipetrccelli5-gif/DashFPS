package com.dashfps.mixin;

import com.dashfps.DashFPS;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void dashfps$disableItemFrameMaps(
        ItemFrame entity,
        ItemFrameRenderState state,
        float partialTicks,
        CallbackInfo ci
    ) {
        if (DashFPS.isAggressiveCullingActive()) {
            state.mapId = null;
        }
    }
}
