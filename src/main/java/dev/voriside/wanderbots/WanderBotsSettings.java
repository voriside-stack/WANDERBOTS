package dev.voriside.wanderbots;

final class WanderBotsSettings {
    static double speed = 0.115;
    static double radius = 32.0;
    static double turnChance = 0.025;
    static double minTurn = 35.0;
    static double maxTurn = 125.0;
    static double lookDistance = 1.05;
    static boolean jumpOverOneBlock = true;

    private WanderBotsSettings() {}

    static void load(WanderBotsPlugin plugin) {
        speed = plugin.getConfig().getDouble("movement.speed", 0.115);
        radius = plugin.getConfig().getDouble("movement.wander-radius", 32.0);
        turnChance = plugin.getConfig().getDouble("movement.random-turn-chance-per-tick", 0.025);
        minTurn = plugin.getConfig().getDouble("movement.minimum-turn-degrees", 35.0);
        maxTurn = plugin.getConfig().getDouble("movement.maximum-turn-degrees", 125.0);
        lookDistance = plugin.getConfig().getDouble("movement.obstacle-look-distance", 1.05);
        jumpOverOneBlock = plugin.getConfig().getBoolean("movement.jump-over-one-block-obstacles", true);
    }
}
