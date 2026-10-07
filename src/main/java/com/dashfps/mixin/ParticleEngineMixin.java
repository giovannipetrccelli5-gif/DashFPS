package com.dashfps.mixin;

import com.dashfps.DashFPS;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void dashfps$disableMobSpawnParticles(
        ParticleOptions options,
        double x,
        double y,
        double z,
        double xa,
        double ya,
        double za,
        CallbackInfoReturnable<Particle> cir
    ) {
        if (DashFPS.isAggressiveCullingActive()
            && (options.getType() == ParticleTypes.POOF
                || options.getType() == ParticleTypes.OMINOUS_SPAWNING
                || options.getType() == ParticleTypes.PAUSE_MOB_GROWTH
                || options.getType() == ParticleTypes.RESET_MOB_GROWTH)) {
            cir.setReturnValue(null);
        }
    }
}
