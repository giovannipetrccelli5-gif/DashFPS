package com.dashfps;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;

import java.util.Locale;

public final class DashFPSManager {
    public enum Preset {
        POTATO, PERFORMANCE, BALANCED, QUALITY, MAX;

        public static Preset parse(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }

        public Preset next() {
            Preset[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum Pacing {
        OFF, SMOOTH, MAX;

        public static Pacing parse(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }

        public Pacing next() {
            Pacing[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private final ModIntegrationManager integrations = new ModIntegrationManager();
    private DashFPSConfig config;
    private VanillaSnapshot fpsModeSnapshot;
    private final Object[] adaptiveOld = new Object[9];
    private int adaptiveStep;
    private int tickCounter;
    private int highFpsSeconds;
    private Boolean originalVsync;
    private Integer originalFpsLimit;

    public DashFPSManager(DashFPSConfig config) {
        this.config = config;
    }

    public void setConfig(DashFPSConfig config) {
        this.config = config;
    }

    public int adaptiveStep() {
        return adaptiveStep;
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.options == null || minecraft.player == null) {
            tickCounter = 0;
            highFpsSeconds = 0;
            return;
        }

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        if (config.adaptiveQuality) {
            int fps = FpsHud.averageFpsLastSecond();
            if (fps > 0 && fps < config.lowFpsThreshold) {
                highFpsSeconds = 0;
                stepDown(minecraft, fps);
            } else if (fps > config.highFpsThreshold) {
                highFpsSeconds++;
                if (highFpsSeconds >= config.highFpsSeconds) {
                    highFpsSeconds = 0;
                    stepUp(minecraft, fps);
                }
            } else {
                highFpsSeconds = 0;
            }
        } else {
            highFpsSeconds = 0;
        }
    }

    public void toggleFpsMode(Minecraft minecraft) {
        setFpsMode(minecraft, !config.fpsModeOn);
    }

    public void setFpsMode(Minecraft minecraft, boolean enabled) {
        if (minecraft.options == null) return;
        if (enabled == config.fpsModeOn && (enabled ? fpsModeSnapshot != null : fpsModeSnapshot == null)) return;

        if (enabled) {
            fpsModeSnapshot = VanillaSnapshot.capture(minecraft.options);
            config.fpsModeOn = true;
            CrashGuard.run("fps_mode_vanilla", () -> {
                Options o = minecraft.options;
                o.cloudStatus().set(CloudStatus.OFF);
                o.particles().set(ParticleStatus.MINIMAL);
                o.entityShadows().set(false);
                o.ambientOcclusion().set(false);
                o.renderDistance().set(Math.max(config.minRenderDistance, Math.min(8, o.renderDistance().get())));
                o.save();
            });
            integrations.applyFpsMode();
            actionBar(minecraft, "DashFPS: FPS Mode ON");
        } else {
            restoreAdaptiveAll(minecraft);
            integrations.restoreFpsMode();
            if (fpsModeSnapshot != null) {
                VanillaSnapshot snapshot = fpsModeSnapshot;
                CrashGuard.run("fps_mode_restore", () -> snapshot.restore(minecraft.options));
            }
            fpsModeSnapshot = null;
            config.fpsModeOn = false;
            actionBar(minecraft, "DashFPS: FPS Mode OFF");
        }
        config.save();
    }

    public void applyPreset(Minecraft minecraft, Preset preset, boolean announce) {
        CrashGuard.run("preset", () -> {
            Options o = minecraft.options;
            int render;
            int sim;
            int biome;
            CloudStatus clouds;
            ParticleStatus particles;
            boolean shadows;
            boolean smooth;

            switch (preset) {
                case POTATO -> {
                    render = 4; sim = 5; biome = 0; clouds = CloudStatus.OFF; particles = ParticleStatus.MINIMAL; shadows = false; smooth = false;
                }
                case PERFORMANCE -> {
                    render = 6; sim = 5; biome = 0; clouds = CloudStatus.OFF; particles = ParticleStatus.MINIMAL; shadows = false; smooth = false;
                }
                case BALANCED -> {
                    render = 8; sim = 6; biome = 2; clouds = CloudStatus.FAST; particles = ParticleStatus.DECREASED; shadows = false; smooth = true;
                }
                case QUALITY -> {
                    render = Math.min(16, config.maxRenderDistance); sim = 10; biome = 5; clouds = CloudStatus.FANCY; particles = ParticleStatus.ALL; shadows = true; smooth = true;
                }
                case MAX -> {
                    render = config.maxRenderDistance; sim = 12; biome = 7; clouds = CloudStatus.FANCY; particles = ParticleStatus.ALL; shadows = true; smooth = true;
                }
                default -> throw new IllegalStateException("Unexpected preset: " + preset);
            }

            o.cloudStatus().set(clouds);
            o.particles().set(particles);
            o.entityShadows().set(shadows);
            o.ambientOcclusion().set(smooth);
            o.renderDistance().set(Math.max(config.minRenderDistance, render));
            o.simulationDistance().set(Math.max(5, sim));
            o.biomeBlendRadius().set(biome);
            o.save();
            integrations.applyPreset(preset);
            config.currentPreset = preset.name().toLowerCase(Locale.ROOT);
            config.save();
            if (announce) actionBar(minecraft, "DashFPS preset: " + config.currentPreset);
        });
    }

    public void cyclePreset(Minecraft minecraft) {
        Preset current;
        try {
            current = Preset.parse(config.currentPreset);
        } catch (Throwable ignored) {
            current = Preset.BALANCED;
        }
        applyPreset(minecraft, current.next(), true);
    }

    public void setPacing(Minecraft minecraft, Pacing pacing, boolean announce) {
        CrashGuard.run("frame_pacing", () -> {
            Options o = minecraft.options;
            if (originalVsync == null) originalVsync = o.enableVsync().get();
            if (originalFpsLimit == null) originalFpsLimit = o.framerateLimit().get();

            switch (pacing) {
                case OFF -> {
                    if (originalVsync != null) o.enableVsync().set(originalVsync);
                    if (originalFpsLimit != null) o.framerateLimit().set(originalFpsLimit);
                }
                case SMOOTH -> {
                    o.enableVsync().set(false);
                    o.framerateLimit().set(60);
                }
                case MAX -> {
                    o.enableVsync().set(false);
                    o.framerateLimit().set(260);
                }
            }
            o.save();
            config.pacingMode = pacing.name().toLowerCase(Locale.ROOT);
            config.save();
            if (announce) actionBar(minecraft, "DashFPS pacing: " + config.pacingMode);
        });
    }

    public void cyclePacing(Minecraft minecraft) {
        Pacing current;
        try {
            current = Pacing.parse(config.pacingMode);
        } catch (Throwable ignored) {
            current = Pacing.OFF;
        }
        setPacing(minecraft, current.next(), true);
    }

    public void applyMobileOptimize(Minecraft minecraft, boolean announce) {
        CrashGuard.run("mobile_optimize", () -> {
            minecraft.options.guiScale().set(2);
            minecraft.options.renderDistance().set(8);
            applyPreset(minecraft, Preset.BALANCED, false);
            minecraft.options.save();
            CrashGuard.log("mobile_menu_animations", "No stable public 26.3 API exists for menu animations; left unchanged to avoid a conflicting GUI mixin", null);
            if (announce) actionBar(minecraft, "DashFPS mobile optimization applied");
        });
    }

    public void applyFirstLaunchMobileIfNeeded(Minecraft minecraft) {
        if (!config.mobileTweaks || config.mobileFirstLaunchDone || !isPojav()) return;
        applyMobileOptimize(minecraft, false);
        config.mobileFirstLaunchDone = true;
        config.save();
        actionBar(minecraft, "DashFPS detected PojavLauncher: balanced mobile preset applied");
    }

    public static boolean isPojav() {
        String pojav = System.getProperty("pojav");
        String classPath = System.getProperty("java.class.path", "").toLowerCase(Locale.ROOT);
        String vendor = System.getProperty("java.vm.vendor", "").toLowerCase(Locale.ROOT);
        return pojav != null || classPath.contains("pojav") || vendor.contains("pojav");
    }

    private void stepDown(Minecraft minecraft, int fps) {
        if (adaptiveStep >= 8) return;
        int next = adaptiveStep + 1;
        CrashGuard.run("adaptive_step_" + next, () -> applyAdaptiveStep(minecraft.options, next));
        if (!CrashGuard.isDisabled("adaptive_step_" + next)) {
            adaptiveStep = next;
            minecraft.options.save();
            actionBar(minecraft, "DashFPS quality - step " + adaptiveStep + " (" + fps + " FPS)");
        }
    }

    private void stepUp(Minecraft minecraft, int fps) {
        if (adaptiveStep <= 0) return;
        int step = adaptiveStep;
        CrashGuard.run("adaptive_restore_" + step, () -> restoreAdaptiveStep(minecraft.options, step));
        if (!CrashGuard.isDisabled("adaptive_restore_" + step)) {
            adaptiveStep--;
            minecraft.options.save();
            actionBar(minecraft, "DashFPS quality + step " + adaptiveStep + " (" + fps + " FPS)");
        }
    }

    private void applyAdaptiveStep(Options o, int step) {
        switch (step) {
            case 1 -> { adaptiveOld[1] = o.cloudStatus().get(); o.cloudStatus().set(CloudStatus.OFF); }
            case 2 -> { adaptiveOld[2] = o.particles().get(); o.particles().set(ParticleStatus.MINIMAL); }
            case 3 -> { adaptiveOld[3] = o.entityShadows().get(); o.entityShadows().set(false); }
            case 4 -> { adaptiveOld[4] = o.renderDistance().get(); o.renderDistance().set(Math.max(config.minRenderDistance, o.renderDistance().get() - 2)); }
            case 5 -> { adaptiveOld[5] = o.simulationDistance().get(); o.simulationDistance().set(Math.max(5, o.simulationDistance().get() - 2)); }
            case 6 -> { adaptiveOld[6] = o.ambientOcclusion().get(); o.ambientOcclusion().set(false); }
            case 7 -> { adaptiveOld[7] = o.renderDistance().get(); o.renderDistance().set(Math.max(config.minRenderDistance, o.renderDistance().get() - 2)); }
            case 8 -> { adaptiveOld[8] = o.biomeBlendRadius().get(); o.biomeBlendRadius().set(0); }
            default -> { }
        }
    }

    private void restoreAdaptiveStep(Options o, int step) {
        Object old = adaptiveOld[step];
        if (old == null) return;
        switch (step) {
            case 1 -> o.cloudStatus().set((CloudStatus) old);
            case 2 -> o.particles().set((ParticleStatus) old);
            case 3 -> o.entityShadows().set((Boolean) old);
            case 4, 7 -> o.renderDistance().set((Integer) old);
            case 5 -> o.simulationDistance().set((Integer) old);
            case 6 -> o.ambientOcclusion().set((Boolean) old);
            case 8 -> o.biomeBlendRadius().set((Integer) old);
            default -> { }
        }
        adaptiveOld[step] = null;
    }

    public void restoreAdaptiveAll(Minecraft minecraft) {
        while (adaptiveStep > 0) {
            restoreAdaptiveStep(minecraft.options, adaptiveStep);
            adaptiveStep--;
        }
        minecraft.options.save();
        highFpsSeconds = 0;
    }

    public void forceRestore(Minecraft minecraft) {
        if (config.fpsModeOn || fpsModeSnapshot != null) setFpsMode(minecraft, false);
        restoreAdaptiveAll(minecraft);
    }

    public static void actionBar(Minecraft minecraft, String text) {
        if (minecraft.player != null) minecraft.gui.hud.setOverlayMessage(Component.literal(text), false);
    }

    private record VanillaSnapshot(
            CloudStatus clouds,
            ParticleStatus particles,
            boolean shadows,
            boolean smoothLighting,
            int renderDistance,
            int simulationDistance,
            int biomeBlend
    ) {
        static VanillaSnapshot capture(Options o) {
            return new VanillaSnapshot(
                    o.cloudStatus().get(), o.particles().get(), o.entityShadows().get(), o.ambientOcclusion().get(),
                    o.renderDistance().get(), o.simulationDistance().get(), o.biomeBlendRadius().get()
            );
        }

        void restore(Options o) {
            o.cloudStatus().set(clouds);
            o.particles().set(particles);
            o.entityShadows().set(shadows);
            o.ambientOcclusion().set(smoothLighting);
            o.renderDistance().set(renderDistance);
            o.simulationDistance().set(simulationDistance);
            o.biomeBlendRadius().set(biomeBlend);
            o.save();
        }
    }
}