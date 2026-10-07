package com.dashfps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DashFPSConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("dashfps.json");

    public boolean fpsModeOn = false;
    public boolean dynamicRenderDistance = true;
    public int maxViewDistance = 32;
    public int minFps = 45;
    public int maxFps = 90;
    public double fovCullAngle = 90.0;

    public static DashFPSConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            DashFPSConfig config = new DashFPSConfig();
            config.validate();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            DashFPSConfig config = GSON.fromJson(reader, DashFPSConfig.class);
            if (config == null) {
                config = new DashFPSConfig();
            }
            config.validate();
            config.save();
            return config;
        } catch (Exception exception) {
            DashFPS.LOGGER.warn("Could not read {}, using defaults", CONFIG_PATH, exception);
            DashFPSConfig config = new DashFPSConfig();
            config.validate();
            config.save();
            return config;
        }
    }

    public static DashFPSConfig reset() {
        DashFPSConfig config = new DashFPSConfig();
        config.validate();
        config.save();
        return config;
    }

    public void save() {
        validate();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            DashFPS.LOGGER.warn("Could not save {}", CONFIG_PATH, exception);
        }
    }

    private void validate() {
        maxViewDistance = Math.max(4, Math.min(500, maxViewDistance));
        minFps = Math.max(1, Math.min(500, minFps));
        maxFps = Math.max(minFps + 1, Math.min(1000, maxFps));
        fovCullAngle = Math.max(30.0, Math.min(180.0, fovCullAngle));
    }

    public static Path path() {
        return CONFIG_PATH;
    }
}
