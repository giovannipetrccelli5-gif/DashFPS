package com.dashfps.mixin;

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
        index = 1,
        require = 0
    )
    private int dashfps$increaseMaxRenderDistance(int originalMax) {
        return 500;
    }
}
