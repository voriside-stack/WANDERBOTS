package dev.voriside.wanderbots;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketFlow;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

final class BotManager {
    private final Plugin plugin;
    private final SkinService skinService;
    private final MinecraftServer server;
    private final Map<UUID, BotRecord> bots = new LinkedHashMap<>();
    private final Set<String> usedNames = NameGenerator.newUsedSet();
    private int controllerTask = -1;

    BotManager(Plugin plugin, SkinService skinService) {
        this.plugin = plugin;
        this.skinService = skinService;
        this.server = ((org.bukkit.craftbukkit.CraftServer) Bukkit.getServer()).getServer();
    }

    void start() {
        controllerTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 1L, 1L);
    }

    void tick() {
        List<BotRecord> snapshot;
        synchronized (bots) {
            snapshot = new ArrayList<>(bots.values());
        }

        long now = System.currentTimeMillis();
        for (BotRecord record : snapshot) {
            ServerPlayer bot = record.player();
            if (bot == null || bot.isRemoved()) {
                continue;
            }

            if (bot.getHealth() <= 0.0f) {
                if (record.respawnAtMillis() == 0L) {
                    long respawnDelayTicks = Math.max(20L, plugin.getConfig().getLong("movement.respawn-after-death-ticks", 100L));
                    record.respawnAtMillis(now + respawnDelayTicks * 50L);
                } else if (now >= record.respawnAtMillis()) {
                    UUID oldId = bot.getUUID();
                    Location origin = record.origin();
                    String name = record.name();
                    remove(oldId, false);
                    spawnAt(origin, name, true);
                }
                continue;
            }

            BotController.control(bot, record);
        }
    }

    void spawn(CommandSender sender, int amount) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(WanderBotsPlugin.msg("&cRun this command in-game.").toString());
            return;
        }

        int max = Math.max(1, plugin.getConfig().getInt("max-bots", 100));
        int available = Math.max(0, max - bots.size());
        int count = Math.min(Math.max(1, amount), available);
        if (count <= 0) {
            sender.sendMessage(WanderBotsPlugin.msg("&cYou already have the maximum number of bots (&f" + max + "&c)."));
            return;
        }

        Location base = player.getLocation().clone();
        sender.sendMessage(WanderBotsPlugin.msg("&7Spawning &f" + count + "&7 bot(s) with random names and skins..."));

        for (int i = 0; i < count; i++) {
            final String name;
            synchronized (usedNames) {
                name = NameGenerator.next(usedNames);
            }
            final Location spawnLocation = base.clone().add(
                    ThreadLocalRandom.current().nextDouble(-2.5, 2.5),
                    0,
                    ThreadLocalRandom.current().nextDouble(-2.5, 2.5)
            );

            skinService.createProfile(name).thenAccept(profile -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (bots.size() >= max) {
                    synchronized (usedNames) {
                        usedNames.remove(name);
                    }
                    return;
                }
                try {
                    spawnProfile(spawnLocation, profile);
                } catch (Throwable error) {
                    synchronized (usedNames) {
                        usedNames.remove(name);
                    }
                    plugin.getLogger().severe("Could not spawn bot " + name + ": " + error.getMessage());
                }
            }));
        }
    }

    private void spawnAt(Location location, String requestedName, boolean keepName) {
        final String name;
        synchronized (usedNames) {
            if (keepName && !usedNames.contains(requestedName)) {
                usedNames.add(requestedName);
                name = requestedName;
            } else {
                name = NameGenerator.next(usedNames);
            }
        }
        skinService.createProfile(name).thenAccept(profile -> Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                spawnProfile(location, profile);
            } catch (Throwable error) {
                synchronized (usedNames) {
                    usedNames.remove(name);
                }
            }
        }));
    }

    private void spawnProfile(Location location, GameProfile profile) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Spawn location has no world");
        }
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        ServerLevel level = craftWorld.getHandle();

        ServerPlayer bot = new ServerPlayer(
                server,
                level,
                profile,
                ClientInformation.createDefault()
        );

        bot.setPos(location.getX(), location.getY(), location.getZ());
        bot.setRot(location.getYaw(), 0.0f);
        bot.setYHeadRot(location.getYaw());
        bot.setHealth(20.0f);
        bot.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);

        Connection connection = createMemoryConnection(bot);
        new EmbeddedChannel(connection);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);

        server.getPlayerList().placeNewPlayer(connection, bot, cookie);
        server.getConnection().getConnections().add(connection);

        BotRecord record = new BotRecord(bot, connection, location.clone(), bot.getYRot(), profile.name());
        synchronized (bots) {
            bots.put(bot.getUUID(), record);
        }

        // Prime the random item state without making every bot start with an obvious weapon.
        if (ThreadLocalRandom.current().nextDouble() < 0.55) {
            RandomEquipment.holdRandom(bot, plugin.getConfig().getDouble("items.random-item-chance-stack", 0.2));
        }
    }

    private Connection createMemoryConnection(ServerPlayer bot) {
        return new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public void tick() {
                super.tick();
                bot.resetLastActionTime();
            }

            @Override
            public boolean isMemoryConnection() {
                return true;
            }

            @Override
            public void send(Packet<?> packet, ChannelFutureListener sendListener, boolean flush) {
                super.send(packet, sendListener, flush);
                if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
                    bot.connection.handleKeepAlive(new ServerboundKeepAlivePacket(keepAlive.getId()));
                }
            }
        };
    }

    void equip(String target) {
        forEachTarget(target, bot -> RandomEquipment.equipRandomArmor(bot));
    }

    void hold(String target) {
        double stackChance = plugin.getConfig().getDouble("items.random-item-chance-stack", 0.2);
        forEachTarget(target, bot -> RandomEquipment.holdRandom(bot, stackChance));
    }

    void respawn(String target) {
        List<BotRecord> targets = findTargets(target);
        for (BotRecord record : targets) {
            if (record.player().getHealth() > 0) {
                continue;
            }
            Location origin = record.origin().clone();
            String oldName = record.name();
            remove(record.player().getUUID(), false);
            spawnAt(origin, oldName, true);
        }
    }

    void remove(String target) {
        if ("all".equalsIgnoreCase(target)) {
            removeAll(true);
            return;
        }
        BotRecord record = findByName(target);
        if (record != null) {
            remove(record.player().getUUID(), true);
        }
    }

    void remove(UUID uuid, boolean announce) {
        BotRecord record;
        synchronized (bots) {
            record = bots.remove(uuid);
        }
        if (record == null) {
            return;
        }

        ServerPlayer bot = record.player();
        try {
            server.getPlayerList().remove(bot);
        } catch (Throwable ignored) {
            try {
                bot.remove(Entity.RemovalReason.DISCARDED);
            } catch (Throwable ignoredAgain) {
                // Last-resort cleanup only.
            }
        }

        try {
            server.getConnection().getConnections().remove(record.connection());
        } catch (Throwable ignored) {
            // Connection removal is best-effort; PlayerList.remove normally handles it.
        }

        synchronized (usedNames) {
            usedNames.remove(record.name());
        }

        if (announce) {
            plugin.getLogger().info("Removed bot " + record.name());
        }
    }

    void removeAll(boolean announce) {
        List<UUID> ids;
        synchronized (bots) {
            ids = new ArrayList<>(bots.keySet());
        }
        for (UUID id : ids) {
            remove(id, false);
        }
        synchronized (usedNames) {
            usedNames.clear();
        }
        if (announce) {
            plugin.getLogger().info("Removed " + ids.size() + " bot(s).");
        }
        if (controllerTask != -1 && !Bukkit.getScheduler().isCurrentlyRunning(controllerTask)) {
            Bukkit.getScheduler().cancelTask(controllerTask);
            controllerTask = -1;
        }
    }

    boolean isBot(UUID uuid) {
        synchronized (bots) {
            return bots.containsKey(uuid);
        }
    }

    int size() {
        synchronized (bots) {
            return bots.size();
        }
    }

    int skinCandidateCount() {
        return skinService.candidateCount();
    }

    Collection<BotRecord> snapshot() {
        synchronized (bots) {
            return List.copyOf(bots.values());
        }
    }

    private void forEachTarget(String target, java.util.function.Consumer<ServerPlayer> action) {
        for (BotRecord record : findTargets(target)) {
            action.accept(record.player());
        }
    }

    private List<BotRecord> findTargets(String target) {
        synchronized (bots) {
            if ("all".equalsIgnoreCase(target)) {
                return new ArrayList<>(bots.values());
            }
            BotRecord record = findByName(target);
            return record == null ? List.of() : List.of(record);
        }
    }

    private BotRecord findByName(String name) {
        synchronized (bots) {
            for (BotRecord record : bots.values()) {
                if (record.name().equalsIgnoreCase(name)) {
                    return record;
                }
            }
            return null;
        }
    }

    static final class BotRecord {
        private final ServerPlayer player;
        private final Connection connection;
        private final Location origin;
        private final float startingYaw;
        private final String name;
        private long respawnAtMillis;

        BotRecord(ServerPlayer player, Connection connection, Location origin, float startingYaw, String name) {
            this.player = player;
            this.connection = connection;
            this.origin = origin;
            this.startingYaw = startingYaw;
            this.name = name;
        }

        ServerPlayer player() { return player; }
        Connection connection() { return connection; }
        Location origin() { return origin; }
        float startingYaw() { return startingYaw; }
        String name() { return name; }

        void respawnAtMillis(long value) { this.respawnAtMillis = value; }
        long respawnAtMillis() { return respawnAtMillis; }
    }
}
