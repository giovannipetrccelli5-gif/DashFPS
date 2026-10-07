package com.example.fpsplus;

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

public class FPSPlus implements ClientModInitializer {
    public static final String MOD_ID = "fpsplus";

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    private static boolean fpsMode = false;
    private static KeyMapping toggleFpsMode;

    private static CloudStatus previousCloudStatus = CloudStatus.FANCY;
    private static ParticleStatus previousParticleStatus = ParticleStatus.ALL;
    private static boolean previousEntityShadows = true;
    private static int previousRenderDistance = 12;

    @Override
    public void onInitializeClient() {
        toggleFpsMode = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.fpsplus.toggle",
            InputConstants.KEY_F8,
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleFpsMode.consumeClick()) {
                toggleFpsMode(client);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
            dispatcher.register(ClientCommands.literal("fpsplus")
                .then(ClientCommands.literal("renderdistance")
                    .then(ClientCommands.argument("distance", IntegerArgumentType.integer(2, 500))
                        .executes(context -> {
                            int distance = IntegerArgumentType.getInteger(context, "distance");
                            Minecraft minecraft = Minecraft.getInstance();
                            minecraft.options.renderDistance().set(distance);
                            minecraft.options.save();
                            context.getSource().sendFeedback(Component.literal("Render distance set to " + distance));
                            return 1;
                        })
                    )
                )
            );
        });
    }

    private static void toggleFpsMode(Minecraft minecraft) {
        Options options = minecraft.options;
        fpsMode = !fpsMode;

        if (fpsMode) {
            previousCloudStatus = options.cloudStatus().get();
            previousParticleStatus = options.particles().get();
            previousEntityShadows = options.entityShadows().get();
            previousRenderDistance = options.renderDistance().get();

            options.cloudStatus().set(CloudStatus.OFF);
            options.particles().set(ParticleStatus.MINIMAL);
            options.entityShadows().set(false);
            options.renderDistance().set(8);
        } else {
            options.cloudStatus().set(previousCloudStatus);
            options.particles().set(previousParticleStatus);
            options.entityShadows().set(previousEntityShadows);
            options.renderDistance().set(previousRenderDistance);
        }

        options.save();
    }
}
