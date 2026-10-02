package dev.voriside.wanderbots;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

final class BotController {
    private BotController() {}

    static void control(ServerPlayer bot, BotManager.BotRecord record) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double speed = WanderBotsSettings.speed;
        double wanderRadius = WanderBotsSettings.radius;

        double distanceSquared = squaredHorizontalDistance(bot.getX(), bot.getZ(), record.origin().getX(), record.origin().getZ());
        float yaw = bot.getYRot();

        if (distanceSquared > wanderRadius * wanderRadius) {
            double dx = record.origin().getX() - bot.getX();
            double dz = record.origin().getZ() - bot.getZ();
            yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        } else if (bot.horizontalCollision || aheadBlocked(bot, yaw, 1.05)) {
            float turn = (float) random.nextDouble(WanderBotsSettings.minTurn, WanderBotsSettings.maxTurn);
            if (random.nextBoolean()) {
                turn = -turn;
            }
            yaw += turn;

            if (WanderBotsSettings.jumpOverOneBlock && bot.onGround && canJumpObstacle(bot, yaw)) {
                bot.jumpFromGround();
            }
        } else if (random.nextDouble() < WanderBotsSettings.turnChance) {
            yaw += (float) random.nextDouble(-30.0, 30.0);
        }

        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);
        bot.yRotO = yaw;

        double radians = Math.toRadians(yaw);
        double dx = -Math.sin(radians) * speed;
        double dz = Math.cos(radians) * speed;

        Vec3 current = bot.getDeltaMovement();
        // Preserve server-managed Y velocity so gravity/falling remains vanilla-like.
        bot.setDeltaMovement(dx, current.y, dz);
    }

    private static boolean aheadBlocked(ServerPlayer bot, float yaw, double distance) {
        double radians = Math.toRadians(yaw);
        double x = bot.getX() - Math.sin(radians) * distance;
        double z = bot.getZ() + Math.cos(radians) * distance;
        int y = (int) Math.floor(bot.getY() + 0.05);

        ServerLevel level = bot.serverLevel();
        return hasCollision(level, x, y, z) || hasCollision(level, x, y + 1, z);
    }

    private static boolean canJumpObstacle(ServerPlayer bot, float yaw) {
        double radians = Math.toRadians(yaw);
        double x = bot.getX() - Math.sin(radians) * 0.85;
        double z = bot.getZ() + Math.cos(radians) * 0.85;
        int y = (int) Math.floor(bot.getY());
        ServerLevel level = bot.serverLevel();
        boolean feetBlocked = hasCollision(level, x, y, z);
        boolean headBlocked = hasCollision(level, x, y + 1, z);
        boolean twoHigh = hasCollision(level, x, y + 2, z);
        return feetBlocked && !headBlocked && !twoHigh;
    }

    private static boolean hasCollision(ServerLevel level, double x, int y, double z) {
        BlockPos pos = new BlockPos((int) Math.floor(x), y, (int) Math.floor(z));
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static double squaredHorizontalDistance(double x, double z, double ox, double oz) {
        double dx = x - ox;
        double dz = z - oz;
        return dx * dx + dz * dz;
    }
}
