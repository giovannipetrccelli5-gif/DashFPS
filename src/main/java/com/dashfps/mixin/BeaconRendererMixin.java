package com.dashfps.mixin;

import com.dashfps.DashFPS;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconRenderer.class)
public abstract class BeaconRendererMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashfps$disableBeaconBeam(CallbackInfo ci) {
        if (DashFPS.isAggressiveCullingActive()) {
            ci.cancel();
        }
    }
}
