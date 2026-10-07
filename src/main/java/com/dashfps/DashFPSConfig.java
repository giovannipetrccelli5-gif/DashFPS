package com.dashfps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DashFPSConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("dashfps.json");

    public boolean fpsModeOn = false;
    public boolean adaptiveQuality = true;
    public boolean hudEnabled = true;
    public String hudPosition = "top_left";
    public boolean modIntegrations = true;
    public boolean mobileTweaks = true;
    public boolean soundCulling = true;
    public double soundCullDistance = 24.0;
    public boolean chunkTickReduction = false;
    public boolean entityTickReduction = false;
    public int lowFpsThreshold = 30;
    public int highFpsThreshold = 75;
    public int highFpsSeconds = 10;
    public int minRenderDistance = 4;
    public int maxRenderDistance = 32;
    public String currentPreset = "balanced";
    public String pacingMode = "off";
    public boolean mobileFirstLaunchDone = false;

    public static DashFPSConfig load() {
        if (!Files.exists(PATH)) {
            DashFPSConfig config = new DashFPSConfig();
            config.validate();
            config.save();
            return config;
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            DashFPSConfig config = GSON.fromJson(reader, DashFPSConfig.class);
            if (config == null) config = new DashFPSConfig();
            config.validate();
            return config;
        } catch (Throwable t) {
            CrashGuard.log("config", "Failed to load config; using defaults", t);
            return new DashFPSConfig();
        }
    }

    public static DashFPSConfig reset() {
        DashFPSConfig config = new DashFPSConfig();
        config.save();
        return config;
    }

    public void save() {
        validate();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (Throwable t) {
            CrashGuard.log("config", "Failed to save config", t);
        }
    }

    private void validate() {
        hudPosition = switch (hudPosition == null ? "top_left" : hudPosition.toLowerCase()) {
            case "top_right", "bottom_left", "bottom_right" -> hudPosition.toLowerCase();
            default -> "top_left";
        };
        pacingMode = switch (pacingMode == null ? "off" : pacingMode.toLowerCase()) {
            case "smooth", "max" -> pacingMode.toLowerCase();
            default -> "off";
        };
        currentPreset = switch (currentPreset == null ? "balanced" : currentPreset.toLowerCase()) {
            case "potato", "performance", "quality", "max" -> currentPreset.toLowerCase();
            default -> "balanced";
        };
        soundCullDistance = Math.max(4.0, Math.min(256.0, soundCullDistance));
        lowFpsThreshold = Math.max(10, Math.min(240, lowFpsThreshold));
        highFpsThreshold = Math.max(lowFpsThreshold + 1, Math.min(500, highFpsThreshold));
        highFpsSeconds = Math.max(1, Math.min(60, highFpsSeconds));
        minRenderDistance = Math.max(2, Math.min(32, minRenderDistance));
        maxRenderDistance = Math.max(minRenderDistance, Math.min(64, maxRenderDistance));
    }

    public static Path path() {
        return PATH;
    }
}