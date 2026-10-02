package dev.voriside.wanderbots;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class WanderBotsPlugin extends JavaPlugin implements Listener {
    private BotManager botManager;
    private SkinService skinService;
    private BotCommand command;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        skinService = new SkinService(this);
        skinService.startDiscovery();

        WanderBotsSettings.load(this);
        botManager = new BotManager(this, skinService);
        command = new BotCommand(this, botManager);

        PluginCommand pluginCommand = getCommand("wanderbots");
        if (pluginCommand == null) {
            getLogger().severe("wanderbots command is missing from plugin.yml");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(this, this);
        botManager.start();

        getLogger().info("WanderBots enabled on Paper 1.21.11.");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.removeAll(false);
        }
        if (skinService != null) {
            skinService.saveCache();
        }
    }

    @EventHandler
    public void onBotJoin(PlayerJoinEvent event) {
        if (botManager != null && botManager.isBot(event.getPlayer().getUniqueId())) {
            event.joinMessage(null);
        }
    }

    @EventHandler
    public void onBotQuit(PlayerQuitEvent event) {
        if (botManager != null && botManager.isBot(event.getPlayer().getUniqueId())) {
            event.quitMessage(null);
        }
    }

    public static String msg(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
