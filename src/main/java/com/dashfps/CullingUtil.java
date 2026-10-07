package com.dashfps;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class CullingUtil {
    private CullingUtil() {}

    public static boolean shouldCullEntity(Entity entity, double camX, double camY, double camZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!DashFPS.isAggressiveCullingActive() || minecraft.player == null || entity == minecraft.player) {
            return false;
        }

        double dx = entity.getX() - camX;
        double dy = entity.getEyeY() - camY;
        double dz = entity.getZ() - camZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        if (distanceSquared <= 32.0 * 32.0) {
            return false;
        }

        Vec3 look = minecraft.player.getLookAngle().normalize();
        Vec3 toEntity = new Vec3(dx, dy, dz).normalize();
        double dot = Math.max(-1.0, Math.min(1.0, look.dot(toEntity)));
        double angle = Math.toDegrees(Math.acos(dot));

        return angle > DashFPS.getConfig().fovCullAngle;
    }

    public static boolean shouldCullChunk(BlockPos renderOrigin) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!DashFPS.isWorldFovCullingActive() || minecraft.player == null) {
            return false;
        }

        Vec3 camera = minecraft.gameRenderer.mainCamera().position();
        double centerX = renderOrigin.getX() + 8.0;
        double centerY = renderOrigin.getY() + 8.0;
        double centerZ = renderOrigin.getZ() + 8.0;

        double dx = centerX - camera.x;
        double dy = centerY - camera.y;
        double dz = centerZ - camera.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        // Always keep nearby chunks to avoid obvious edge popping while turning.
        if (distanceSquared <= 32.0 * 32.0) {
            return false;
        }

        Vec3 look = minecraft.player.getLookAngle().normalize();
        Vec3 toChunk = new Vec3(dx, dy, dz).normalize();
        double dot = Math.max(-1.0, Math.min(1.0, look.dot(toChunk)));
        double angle = Math.toDegrees(Math.acos(dot));

        // Keep a little margin around the configured entity cone for chunk geometry.
        return angle > Math.min(179.0, DashFPS.getConfig().fovCullAngle + 15.0);
    }
}
