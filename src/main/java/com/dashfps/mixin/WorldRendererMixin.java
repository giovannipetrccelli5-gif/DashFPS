package com.dashfps.mixin;

import com.dashfps.CullingUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Unique
    private final Deque<ObjectArrayList<SectionRenderDispatcher.RenderSection>> dashfps$sectionBackups =
        new ArrayDeque<>();

    @Inject(
        method = {"prepareChunkRenders", "prepareChunkRendersIndirect"},
        at = @At("HEAD"),
        require = 0
    )
    private void dashfps$filterChunksOutsideFov(CallbackInfoReturnable<?> cir) {
        ObjectArrayList<SectionRenderDispatcher.RenderSection> backup =
            new ObjectArrayList<>(this.visibleSections);
        this.dashfps$sectionBackups.push(backup);
        this.visibleSections.removeIf(section -> CullingUtil.shouldCullChunk(section.getRenderOrigin()));
    }

    @Inject(
        method = {"prepareChunkRenders", "prepareChunkRendersIndirect"},
        at = @At("RETURN"),
        require = 0
    )
    private void dashfps$restoreVisibleChunks(CallbackInfoReturnable<?> cir) {
        if (this.dashfps$sectionBackups.isEmpty()) {
            return;
        }

        ObjectArrayList<SectionRenderDispatcher.RenderSection> backup =
            this.dashfps$sectionBackups.pop();
        this.visibleSections.clear();
        this.visibleSections.addAll(backup);
    }
}
