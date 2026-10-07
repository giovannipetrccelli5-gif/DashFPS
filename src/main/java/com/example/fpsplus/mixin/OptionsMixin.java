package com.example.fpsplus.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance$IntRange;<init>(IIZ)V",
            ordinal = 0
        ),
        index = 1
    )
    private int fpsplus$increaseMaxRenderDistance(int originalMax) {
        return 500;
    }
}
