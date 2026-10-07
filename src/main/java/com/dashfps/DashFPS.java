package com.dashfps;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DashFPS implements ClientModInitializer {
    public static final String MOD_ID = "dashfps";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    private static DashFPSConfig config;
    private static DashFPSManager manager;
    private static KeyMapping fpsModeKey;
    private static KeyMapping hudKey;
    private static KeyMapping presetKey;
    private static KeyMapping pacingKey;

    @Override
    public void onInitializeClient() {
        config = DashFPSConfig.load();
        ModDetector.scan();
        manager = new DashFPSManager(config);

        fpsModeKey = key("key.dashfps.toggle", InputConstants.KEY_F8);
        hudKey = key("key.dashfps.hud", InputConstants.KEY_F9);
        presetKey = key("key.dashfps.preset", InputConstants.KEY_F10);
        pacingKey = key("key.dashfps.pacing", InputConstants.KEY_F11);

        FpsHud.register(manager);
        ClientTickEvents.END_CLIENT_TICK.register(client -> CrashGuard.run("client_tick", () -> clientTick(client)));
        registerCommands();

        LOGGER.info("DashFPS initialized. Detected optimization mods: {}", ModDetector.names());
        LOGGER.info("DashFPS config: {}", DashFPSConfig.path());
        if (config.chunkTickReduction || config.entityTickReduction) {
            CrashGuard.log("tick_reduction", "Requested tick-reduction flags are not applied: Lithium/Sodium touch the relevant world tick classes, so DashFPS skips them to honor its no-conflict rule", null);
        }
    }

    private static KeyMapping key(String translation, int keyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(translation, keyCode, CATEGORY));
    }

    private static void clientTick(Minecraft minecraft) {
        while (fpsModeKey.consumeClick()) manager.toggleFpsMode(minecraft);
        while (hudKey.consumeClick()) {
            config.hudEnabled = !config.hudEnabled;
            config.save();
            DashFPSManager.actionBar(minecraft, "DashFPS HUD " + (config.hudEnabled ? "ON" : "OFF"));
        }
        while (presetKey.consumeClick()) manager.cyclePreset(minecraft);
        while (pacingKey.consumeClick()) manager.cyclePacing(minecraft);

        if (minecraft.player != null) {
            if (config.fpsModeOn) manager.setFpsMode(minecraft, true);
            manager.applyFirstLaunchMobileIfNeeded(minecraft);
        }
        manager.tick(minecraft);
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("dashfps")
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal(
                                    "DashFPS | FPS mode=" + config.fpsModeOn + " | HUD=" + config.hudEnabled
                                            + " | preset=" + config.currentPreset + " | pacing=" + config.pacingMode));
                            return 1;
                        })
                        .then(ClientCommands.literal("mods").executes(context -> {
                            context.getSource().sendFeedback(Component.literal("Detected: " + ModDetector.names()));
                            context.getSource().sendFeedback(Component.literal(ModDetector.delegationSummary()));
                            context.getSource().sendFeedback(Component.literal(
                                    "Tick reduction: intentionally skipped because Lithium/Sodium touch the required tick/world classes."));
                            if (!CrashGuard.disabledFeatures().isEmpty()) {
                                context.getSource().sendFeedback(Component.literal("CrashGuard disabled: " + String.join(", ", CrashGuard.disabledFeatures())));
                            }
                            return 1;
                        }))
                        .then(ClientCommands.literal("preset")
                                .then(ClientCommands.argument("name", StringArgumentType.word()).executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    try {
                                        manager.applyPreset(Minecraft.getInstance(), DashFPSManager.Preset.parse(name), true);
                                        return 1;
                                    } catch (IllegalArgumentException ex) {
                                        context.getSource().sendError(Component.literal("Use: potato, performance, balanced, quality, or max"));
                                        return 0;
                                    }
                                })))
                        .then(ClientCommands.literal("hud")
                                .then(ClientCommands.literal("position")
                                        .then(ClientCommands.argument("corner", StringArgumentType.word()).executes(context -> {
                                            String corner = StringArgumentType.getString(context, "corner").toLowerCase();
                                            if (!corner.equals("top_left") && !corner.equals("top_right")
                                                    && !corner.equals("bottom_left") && !corner.equals("bottom_right")) {
                                                context.getSource().sendError(Component.literal("Use: top_left, top_right, bottom_left, bottom_right"));
                                                return 0;
                                            }
                                            config.hudPosition = corner;
                                            config.save();
                                            context.getSource().sendFeedback(Component.literal("DashFPS HUD position: " + corner));
                                            return 1;
                                        })))
                                .then(ClientCommands.literal("toggle").executes(context -> {
                                    config.hudEnabled = !config.hudEnabled;
                                    config.save();
                                    context.getSource().sendFeedback(Component.literal("DashFPS HUD " + (config.hudEnabled ? "enabled" : "disabled")));
                                    return 1;
                                })))
                        .then(ClientCommands.literal("pacing")
                                .then(ClientCommands.argument("mode", StringArgumentType.word()).executes(context -> {
                                    String value = StringArgumentType.getString(context, "mode");
                                    try {
                                        manager.setPacing(Minecraft.getInstance(), DashFPSManager.Pacing.parse(value), true);
                                        return 1;
                                    } catch (IllegalArgumentException ex) {
                                        context.getSource().sendError(Component.literal("Use: off, smooth, or max"));
                                        return 0;
                                    }
                                })))
                        .then(ClientCommands.literal("mobile")
                                .then(ClientCommands.literal("optimize").executes(context -> {
                                    manager.applyMobileOptimize(Minecraft.getInstance(), true);
                                    return 1;
                                })))
                        .then(ClientCommands.literal("config")
                                .then(ClientCommands.literal("save").executes(context -> {
                                    config.save();
                                    context.getSource().sendFeedback(Component.literal("Saved " + DashFPSConfig.path()));
                                    return 1;
                                }))
                                .then(ClientCommands.literal("reload").executes(context -> {
                                    Minecraft minecraft = Minecraft.getInstance();
                                    manager.forceRestore(minecraft);
                                    config = DashFPSConfig.load();
                                    manager.setConfig(config);
                                    if (config.fpsModeOn) manager.setFpsMode(minecraft, true);
                                    context.getSource().sendFeedback(Component.literal("Reloaded " + DashFPSConfig.path()));
                                    return 1;
                                }))
                                .then(ClientCommands.literal("reset").executes(context -> {
                                    Minecraft minecraft = Minecraft.getInstance();
                                    manager.forceRestore(minecraft);
                                    config = DashFPSConfig.reset();
                                    manager.setConfig(config);
                                    context.getSource().sendFeedback(Component.literal("DashFPS config reset"));
                                    return 1;
                                })))
        ));
    }

    public static DashFPSConfig config() {
        if (config == null) config = DashFPSConfig.load();
        return config;
    }
}