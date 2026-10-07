package com.dashfps;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional, reflection-only compatibility adapters. DashFPS never links against
 * these mods at compile time and silently skips an adapter if its API changes.
 */
public final class ModIntegrationManager {
    private final List<FieldChange> fpsModeChanges = new ArrayList<>();

    public void applyFpsMode() {
        if (!DashFPS.config().modIntegrations) return;
        fpsModeChanges.clear();
        if (ModDetector.loaded("sodium")) safe("integration_sodium", () -> tuneSodium(true, true));
        if (ModDetector.loaded("entityculling")) safe("integration_entityculling", () -> tuneEntityCulling(true, true));
        if (ModDetector.loaded("moreculling")) safe("integration_moreculling", () -> tuneMoreCulling(true, true));
        if (ModDetector.loaded("immediatelyfast")) safe("integration_immediatelyfast", () -> tuneImmediatelyFast(true, true));
    }

    public void applyPreset(DashFPSManager.Preset preset) {
        if (!DashFPS.config().modIntegrations) return;
        boolean performance = preset == DashFPSManager.Preset.POTATO || preset == DashFPSManager.Preset.PERFORMANCE || preset == DashFPSManager.Preset.BALANCED;
        if (ModDetector.loaded("sodium")) safe("integration_sodium", () -> tuneSodium(performance, false));
        if (ModDetector.loaded("entityculling")) safe("integration_entityculling", () -> tuneEntityCulling(performance, false));
        if (ModDetector.loaded("moreculling")) safe("integration_moreculling", () -> tuneMoreCulling(true, false));
        if (ModDetector.loaded("immediatelyfast")) safe("integration_immediatelyfast", () -> tuneImmediatelyFast(true, false));
    }

    public void restoreFpsMode() {
        for (int i = fpsModeChanges.size() - 1; i >= 0; i--) {
            FieldChange change = fpsModeChanges.get(i);
            try {
                change.field.set(change.owner, change.oldValue);
            } catch (Throwable t) {
                CrashGuard.log("integration_restore", "Could not restore " + change.field.getName(), t);
            }
        }
        fpsModeChanges.clear();
    }

    private void tuneSodium(boolean performance, boolean capture) throws Exception {
        Class<?> clientMod = Class.forName("net.caffeinemc.mods.sodium.client.SodiumClientMod");
        Object options = clientMod.getMethod("options").invoke(null);
        set(options, "quality.hiddenFluidCulling", true, capture);
        set(options, "quality.improvedFluidShaping", !performance, capture);
        set(options, "quality.useClosestPointEntitySort", !performance, capture);
        set(options, "performance.animateOnlyVisibleTextures", true, capture);
        set(options, "performance.useEntityCulling", true, capture);
        set(options, "performance.useFogOcclusion", true, capture);
        set(options, "performance.useBlockFaceCulling", true, capture);
        saveSodium(options);
    }

    private void tuneEntityCulling(boolean performance, boolean capture) throws Exception {
        Class<?> base = Class.forName("dev.tr7zw.entityculling.EntityCullingModBase");
        Object instance = base.getField("instance").get(null);
        Object config = findField(base, "config").get(instance);
        set(config, "skipEntityCulling", false, capture);
        set(config, "skipBlockEntityCulling", false, capture);
        set(config, "blockEntityFrustumCulling", true, capture);
        set(config, "tickCulling", true, capture);
        if (performance) {
            Field tracing = findField(config.getClass(), "tracingDistance");
            int old = tracing.getInt(config);
            set(config, "tracingDistance", Math.max(old, 192), capture);
        }
        invokeNoArgIfPresent(instance, "writeConfig");
    }

    private void tuneMoreCulling(boolean enable, boolean capture) throws Exception {
        Class<?> main = Class.forName("ca.fxco.moreculling.MoreCulling");
        Object config = main.getField("CONFIG").get(null);
        String[] fields = {
                "signTextCulling", "rainCulling", "useBlockStateCulling", "useCustomItemFrameRenderer",
                "itemFrameMapCulling", "useItemFrameLOD", "useItemFrame3FaceCulling", "paintingCulling",
                "useOnModdedBlocksByDefault"
        };
        for (String field : fields) set(config, field, enable, capture);
        saveAutoConfig(config.getClass());
    }

    private void tuneImmediatelyFast(boolean enable, boolean capture) throws Exception {
        Class<?> main = Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast");
        Object config = findField(main, "config").get(null);
        String[] fields = {
                "enhanced_batching", "font_atlas_resizing", "map_atlas_generation", "skip_text_translucency_sorting",
                "fast_text_lookup", "avoid_redundant_framebuffer_switching", "fix_slow_buffer_upload_on_apple_gpu",
                "batch_animated_item_updates"
        };
        for (String field : fields) set(config, field, enable, capture);

        try {
            Object runtime = findField(main, "runtimeConfig").get(null);
            for (Field f : runtime.getClass().getFields()) {
                if (f.getType() == boolean.class && !Modifier.isFinal(f.getModifiers())) {
                    if (f.getName().contains("font_atlas") || f.getName().contains("map_atlas")
                            || f.getName().contains("framebuffer") || f.getName().contains("buffer_upload")) {
                        rememberAndSet(runtime, f, enable, capture);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Some versions construct a runtime config that is intentionally immutable.
        }
    }

    private void saveSodium(Object options) {
        try {
            Class<?> clazz = Class.forName("net.caffeinemc.mods.sodium.client.gui.SodiumOptions");
            clazz.getMethod("writeToDisk", clazz).invoke(null, options);
        } catch (Throwable t) {
            CrashGuard.log("integration_sodium_save", "Sodium settings changed in memory but could not be written", t);
        }
    }

    private void saveAutoConfig(Class<?> configClass) {
        try {
            Class<?> autoConfig = Class.forName("me.shedaniel.autoconfig.AutoConfig");
            Object holder = autoConfig.getMethod("getConfigHolder", Class.class).invoke(null, configClass);
            holder.getClass().getMethod("save").invoke(holder);
        } catch (Throwable t) {
            CrashGuard.log("integration_moreculling_save", "MoreCulling settings changed in memory but could not be written", t);
        }
    }

    private void set(Object root, String path, Object value, boolean capture) throws Exception {
        String[] parts = path.split("\\.");
        Object owner = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Field f = findField(owner.getClass(), parts[i]);
            owner = f.get(owner);
            if (owner == null) throw new IllegalStateException("Null config object at " + parts[i]);
        }
        Field field = findField(owner.getClass(), parts[parts.length - 1]);
        rememberAndSet(owner, field, value, capture);
    }

    private void rememberAndSet(Object owner, Field field, Object value, boolean capture) throws Exception {
        field.setAccessible(true);
        if (capture) fpsModeChanges.add(new FieldChange(owner, field, field.get(owner)));
        field.set(owner, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field f = cursor.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static void invokeNoArgIfPresent(Object target, String name) {
        try {
            Class<?> cursor = target.getClass();
            while (cursor != null) {
                try {
                    Method m = cursor.getDeclaredMethod(name);
                    m.setAccessible(true);
                    m.invoke(target);
                    return;
                } catch (NoSuchMethodException ignored) {
                    cursor = cursor.getSuperclass();
                }
            }
        } catch (Throwable t) {
            CrashGuard.log("integration_save", "Could not invoke " + name, t);
        }
    }

    private static void safe(String feature, ThrowingRunnable action) {
        CrashGuard.run(feature, () -> {
            try {
                action.run();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
    }

    private record FieldChange(Object owner, Field field, Object oldValue) {}

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}