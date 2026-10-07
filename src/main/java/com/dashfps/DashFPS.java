package com.dashfps;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DashFPS implements ClientModInitializer {
    public static final String MOD_ID = "dashfps";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    private static DashFPSConfig config;
    private static KeyMapping toggleFpsMode;

    private static boolean fpsSettingsApplied;
    private static boolean startupStateHandled;
    private static int dynamicTickCounter;
    private static int highFpsSamples;

    private static CloudStatus previousCloudStatus = CloudStatus.FANCY;
    private static ParticleStatus previousParticleStatus = ParticleStatus.ALL;
    private static boolean previousEntityShadows = true;
    private static int previousRenderDistance = 12;

    @Override
    public void onInitializeClient() {
        config = DashFPSConfig.load();

        toggleFpsMode = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dashfps.toggle",
            InputConstants.KEY_F8,
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(DashFPS::onClientTick);
        registerCommands();

        LOGGER.info("DashFPS initialized. Config: {}", DashFPSConfig.path());
    }

    private static void onClientTick(Minecraft minecraft) {
        if (minecraft.options == null) {
            return;
        }

        while (toggleFpsMode.consumeClick()) {
            setFpsMode(minecraft, !config.fpsModeOn, true);
        }

        if (minecraft.player != null && !startupStateHandled) {
            startupStateHandled = true;
            if (config.fpsModeOn) {
                applyFpsSettings(minecraft);
            }
        }

        if (minecraft.player == null) {
            startupStateHandled = false;
            dynamicTickCounter = 0;
            highFpsSamples = 0;
            return;
        }

        // Keep runtime state synchronized after config reload/reset.
        if (config.fpsModeOn && !fpsSettingsApplied) {
            applyFpsSettings(minecraft);
        } else if (!config.fpsModeOn && fpsSettingsApplied) {
            restoreFpsSettings(minecraft);
        }

        if (!config.dynamicRenderDistance) {
            dynamicTickCounter = 0;
            highFpsSamples = 0;
            return;
        }

        dynamicTickCounter++;
        if (dynamicTickCounter < 100) {
            return;
        }
        dynamicTickCounter = 0;

        int fps = minecraft.getFps();
        int current = minecraft.options.renderDistance().get();

        if (fps < config.minFps) {
            highFpsSamples = 0;
            int next = Math.max(4, current - 2);
            if (next != current) {
                setDynamicRenderDistance(minecraft, next, fps);
            }
        } else if (fps > config.maxFps) {
            highFpsSamples++;
            // Samples are 5 seconds apart; two samples means ~10 seconds above target.
            if (highFpsSamples >= 2) {
                highFpsSamples = 0;
                int next = Math.min(config.maxViewDistance, current + 2);
                if (next != current) {
                    setDynamicRenderDistance(minecraft, next, fps);
                }
            }
        } else {
            highFpsSamples = 0;
        }
    }

    private static void setDynamicRenderDistance(Minecraft minecraft, int chunks, int fps) {
        minecraft.options.renderDistance().set(chunks);
        minecraft.options.save();
        showActionBar(minecraft, "DashFPS: render distance " + chunks + " (" + fps + " FPS)");
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
            dispatcher.register(ClientCommands.literal("dashfps")
                .then(ClientCommands.literal("renderdistance")
                    .then(ClientCommands.argument("distance", IntegerArgumentType.integer(2, 500))
                        .executes(context -> {
                            int distance = IntegerArgumentType.getInteger(context, "distance");
                            Minecraft minecraft = Minecraft.getInstance();
                            config.maxViewDistance = Math.max(4, distance);
                            config.save();
                            minecraft.options.renderDistance().set(distance);
                            minecraft.options.save();
                            context.getSource().sendFeedback(
                                Component.literal("DashFPS render distance set to " + distance +
                                    "; dynamic maximum is now " + config.maxViewDistance)
                            );
                            return 1;
                        })
                    )
                )
                .then(ClientCommands.literal("config")
                    .then(ClientCommands.literal("reload")
                        .executes(context -> {
                            config = DashFPSConfig.load();
                            context.getSource().sendFeedback(
                                Component.literal("DashFPS config reloaded from " + DashFPSConfig.path())
                            );
                            return 1;
                        })
                    )
                    .then(ClientCommands.literal("reset")
                        .executes(context -> {
                            config = DashFPSConfig.reset();
                            context.getSource().sendFeedback(
                                Component.literal("DashFPS config reset to defaults.")
                            );
                            return 1;
                        })
                    )
                )
            );
        });
    }

    private static void setFpsMode(Minecraft minecraft, boolean enabled, boolean showMessage) {
        config.fpsModeOn = enabled;
        config.save();

        if (enabled) {
            applyFpsSettings(minecraft);
        } else {
            restoreFpsSettings(minecraft);
        }

        if (showMessage) {
            showActionBar(minecraft, enabled ? "DashFPS: FPS Mode ON" : "DashFPS: FPS Mode OFF");
        }
    }

    private static void applyFpsSettings(Minecraft minecraft) {
        if (fpsSettingsApplied) {
            return;
        }

        Options options = minecraft.options;
        previousCloudStatus = options.cloudStatus().get();
        previousParticleStatus = options.particles().get();
        previousEntityShadows = options.entityShadows().get();
        previousRenderDistance = options.renderDistance().get();

        config.maxViewDistance = Math.max(config.maxViewDistance, previousRenderDistance);
        config.save();

        options.cloudStatus().set(CloudStatus.OFF);
        options.particles().set(ParticleStatus.MINIMAL);
        options.entityShadows().set(false);
        options.renderDistance().set(Math.min(8, config.maxViewDistance));
        options.save();

        fpsSettingsApplied = true;
    }

    private static void restoreFpsSettings(Minecraft minecraft) {
        if (!fpsSettingsApplied) {
            return;
        }

        Options options = minecraft.options;
        options.cloudStatus().set(previousCloudStatus);
        options.particles().set(previousParticleStatus);
        options.entityShadows().set(previousEntityShadows);
        options.renderDistance().set(Math.min(500, previousRenderDistance));
        options.save();

        fpsSettingsApplied = false;
        highFpsSamples = 0;
    }

    private static void showActionBar(Minecraft minecraft, String message) {
        if (minecraft.player != null) {
            minecraft.gui.hud.setOverlayMessage(Component.literal(message), false);
        }
    }

    public static DashFPSConfig getConfig() {
        if (config == null) {
            config = DashFPSConfig.load();
        }
        return config;
    }

    public static boolean isAggressiveCullingActive() {
        return getConfig().fpsModeOn;
    }

    public static boolean isWorldFovCullingActive() {
        return getConfig().fpsModeOn && getConfig().dynamicRenderDistance;
    }
}
