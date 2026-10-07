package com.dashfps;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CrashGuard {
    private static final Set<String> DISABLED = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Path LOG_PATH = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("dashfps.log");

    private CrashGuard() {}

    public static void run(String feature, Runnable action) {
        if (DISABLED.contains(feature)) return;
        try {
            action.run();
        } catch (Throwable t) {
            DISABLED.add(feature);
            log(feature, "Feature disabled after an exception", t);
        }
    }

    public static boolean isDisabled(String feature) {
        return DISABLED.contains(feature);
    }

    public static Set<String> disabledFeatures() {
        synchronized (DISABLED) {
            return Set.copyOf(DISABLED);
        }
    }

    public static synchronized void log(String feature, String message, Throwable t) {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            String line = "[" + Instant.now() + "] [" + feature + "] " + message
                    + (t == null ? "" : " :: " + t.getClass().getName() + ": " + t.getMessage()) + System.lineSeparator();
            Files.writeString(LOG_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
        }
        if (t == null) {
            DashFPS.LOGGER.warn("[{}] {}", feature, message);
        } else {
            DashFPS.LOGGER.warn("[{}] {}", feature, message, t);
        }
    }
}