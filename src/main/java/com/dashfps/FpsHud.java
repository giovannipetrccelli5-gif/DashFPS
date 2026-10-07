package com.dashfps;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public final class FpsHud {
    private static final Deque<FrameSample> FRAMES = new ArrayDeque<>();
    private static long lastFrameNanos;
    private static DashFPSManager manager;

    private FpsHud() {}

    public static void register(DashFPSManager dashManager) {
        manager = dashManager;
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(DashFPS.MOD_ID, "fps_hud"),
                (graphics, deltaTracker) -> render(graphics)
        );
    }

    private static void render(GuiGraphicsExtractor graphics) {
        recordFrame();
        DashFPSConfig config = DashFPS.config();
        if (!config.hudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        int current = mc.getFps();
        int low1 = percentileLow(0.99);
        int low01 = percentileLow(0.999);
        int view = mc.options.renderDistance().get();
        int active = ModDetector.count() + (config.fpsModeOn ? 1 : 0) + (manager == null ? 0 : manager.adaptiveStep())
                + (config.soundCulling && config.fpsModeOn ? 1 : 0);

        List<String> lines = new ArrayList<>();
        lines.add("DashFPS  " + current + " FPS");
        lines.add("1% low " + low1 + "  |  0.1% low " + low01);
        lines.add("View " + view + "  |  Active " + active);
        lines.add("Mods: " + ModDetector.names());
        if (DashFPSManager.isPojav()) lines.add("PojavLauncher detected");

        int maxTextWidth = 0;
        for (String line : lines) maxTextWidth = Math.max(maxTextWidth, mc.font.width(line));
        int graphWidth = 120;
        int width = Math.max(maxTextWidth, graphWidth) + 10;
        int graphHeight = 28;
        int height = lines.size() * 10 + graphHeight + 12;

        int x = switch (config.hudPosition) {
            case "top_right", "bottom_right" -> graphics.guiWidth() - width - 6;
            default -> 6;
        };
        int y = switch (config.hudPosition) {
            case "bottom_left", "bottom_right" -> graphics.guiHeight() - height - 6;
            default -> 6;
        };

        graphics.fill(x, y, x + width, y + height, 0xAA000000);
        int ty = y + 5;
        for (String line : lines) {
            graphics.text(mc.font, line, x + 5, ty, 0xFFFFFFFF);
            ty += 10;
        }

        List<Double> graph = latestFrameTimes(60);
        int gx = x + 5;
        int gy = y + height - graphHeight - 5;
        int gw = width - 10;
        graphics.fill(gx, gy, gx + gw, gy + graphHeight, 0x55000000);
        if (!graph.isEmpty()) {
            double barWidth = (double) gw / graph.size();
            for (int i = 0; i < graph.size(); i++) {
                double ms = Math.min(50.0, graph.get(i));
                int barHeight = Math.max(1, (int) Math.round((ms / 50.0) * (graphHeight - 2)));
                int bx = gx + (int) Math.floor(i * barWidth);
                int bx2 = Math.max(bx + 1, gx + (int) Math.ceil((i + 1) * barWidth));
                graphics.fill(bx, gy + graphHeight - barHeight, bx2, gy + graphHeight, 0xFFFFFFFF);
            }
        }
    }

    private static synchronized void recordFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            double ms = (now - lastFrameNanos) / 1_000_000.0;
            if (ms > 0.0 && ms < 10_000.0) FRAMES.addLast(new FrameSample(now, ms));
        }
        lastFrameNanos = now;
        long cutoff = now - 60_000_000_000L;
        while (!FRAMES.isEmpty() && FRAMES.peekFirst().timeNanos < cutoff) FRAMES.removeFirst();
    }

    public static synchronized int averageFpsLastSecond() {
        long cutoff = System.nanoTime() - 1_000_000_000L;
        double totalMs = 0.0;
        int count = 0;
        for (FrameSample sample : FRAMES) {
            if (sample.timeNanos >= cutoff) {
                totalMs += sample.frameMs;
                count++;
            }
        }
        if (count == 0 || totalMs <= 0.0) return Minecraft.getInstance().getFps();
        return (int) Math.round(1000.0 / (totalMs / count));
    }

    private static synchronized int percentileLow(double percentile) {
        if (FRAMES.isEmpty()) return 0;
        List<Double> times = FRAMES.stream().map(sample -> sample.frameMs).sorted(Comparator.naturalOrder()).toList();
        int index = Math.min(times.size() - 1, Math.max(0, (int) Math.ceil(percentile * times.size()) - 1));
        double frameMs = times.get(index);
        return frameMs <= 0.0 ? 0 : (int) Math.round(1000.0 / frameMs);
    }

    private static synchronized List<Double> latestFrameTimes(int count) {
        int skip = Math.max(0, FRAMES.size() - count);
        List<Double> result = new ArrayList<>(Math.min(count, FRAMES.size()));
        int i = 0;
        for (FrameSample sample : FRAMES) {
            if (i++ >= skip) result.add(sample.frameMs);
        }
        return result;
    }

    private record FrameSample(long timeNanos, double frameMs) {}
}