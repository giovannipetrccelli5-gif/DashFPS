package com.dashfps.mixin;

import com.dashfps.CrashGuard;
import com.dashfps.DashFPS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void dashfps$cullDistantSounds(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (CrashGuard.isDisabled("sound_culling")) return;
        try {
            var config = DashFPS.config();
            if (!config.fpsModeOn || !config.soundCulling || sound.isRelative()) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            double dx = minecraft.player.getX() - sound.getX();
            double dy = minecraft.player.getY() - sound.getY();
            double dz = minecraft.player.getZ() - sound.getZ();
            double limit = config.soundCullDistance;
            if ((dx * dx + dy * dy + dz * dz) > limit * limit) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            }
        } catch (Throwable t) {
            CrashGuard.log("sound_culling", "Sound culling disabled after an exception", t);
        }
    }
}