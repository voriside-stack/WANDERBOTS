package dev.voriside.wanderbots;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

final class BotCommand implements CommandExecutor, TabCompleter {
    private final WanderBotsPlugin plugin;
    private final BotManager manager;

    BotCommand(WanderBotsPlugin plugin, BotManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        WanderBotsSettings.load(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wanderbots.admin")) {
            sender.sendMessage(WanderBotsPlugin.msg("&cYou do not have permission to use this."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                int count = 1;
                if (args.length >= 2) {
                    try {
                        count = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                        sender.sendMessage(WanderBotsPlugin.msg("&cAmount must be a whole number."));
                        return true;
                    }
                }
                manager.spawn(sender, count);
            }
            case "remove", "kill" -> {
                if (args.length < 2) {
                    sender.sendMessage(WanderBotsPlugin.msg("&cUsage: /" + label + " remove <name|all>"));
                    return true;
                }
                manager.remove(args[1]);
                sender.sendMessage(WanderBotsPlugin.msg("&aRemoval requested."));
            }
            case "list" -> sender.sendMessage(WanderBotsPlugin.msg(
                    "&7Bots: &f" + manager.size() + " &8| &7Skin candidates: &f" + manager.skinCandidateCount()));
            case "equip", "armor", "armour" -> {
                if (args.length < 2) {
                    sender.sendMessage(WanderBotsPlugin.msg("&cUsage: /" + label + " equip <name|all>"));
                    return true;
                }
                manager.equip(args[1]);
                sender.sendMessage(WanderBotsPlugin.msg("&aRandom armor + enchantments applied."));
            }
            case "hold", "item" -> {
                if (args.length < 2) {
                    sender.sendMessage(WanderBotsPlugin.msg("&cUsage: /" + label + " hold <name|all>"));
                    return true;
                }
                manager.hold(args[1]);
                sender.sendMessage(WanderBotsPlugin.msg("&aRandom held items applied."));
            }
            case "respawn" -> {
                if (args.length < 2) {
                    sender.sendMessage(WanderBotsPlugin.msg("&cUsage: /" + label + " respawn <name|all>"));
                    return true;
                }
                manager.respawn(args[1]);
                sender.sendMessage(WanderBotsPlugin.msg("&aRespawn requested."));
            }
            case "reload" -> {
                plugin.reloadConfig();
                WanderBotsSettings.load(plugin);
                manager.removeAll(false);
                manager.start();
                plugin.getLogger().info("Configuration reloaded.");
                sender.sendMessage(WanderBotsPlugin.msg("&aWanderBots configuration reloaded."));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(WanderBotsPlugin.msg("&6&lWanderBots"));
        sender.sendMessage(WanderBotsPlugin.msg("&e/" + "bots spawn [amount] &7- spawn random bots"));
        sender.sendMessage(WanderBotsPlugin.msg("&e/" + "bots remove <name|all> &7- remove bots"));
        sender.sendMessage(WanderBotsPlugin.msg("&e/" + "bots list &7- show counts"));
        sender.sendMessage(WanderBotsPlugin.msg("&e/" + "bots equip <name|all> &7- random armor + enchants"));
        sender.sendMessage(WanderBotsPlugin.msg("&e/" + "bots hold <name|all> &7- random held item"));
        sender.sendMessage(WanderBotsPlugin.msg("&e/" + "bots respawn <name|all> &7- respawn dead bots"));
        sender.sendMessage(WanderBotsPlugin.msg("&e/" + "bots reload &7- reload config"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("spawn", "remove", "list", "equip", "hold", "respawn", "reload"));
        }
        if (args.length == 2 && List.of("remove", "equip", "hold", "respawn").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            names.add("all");
            for (BotManager.BotRecord record : manager.snapshot()) {
                names.add(record.name());
            }
            return partial(args[1], names);
        }
        return List.of();
    }

    private static List<String> partial(String input, List<String> values) {
        String lower = input.toLowerCase();
        return values.stream().filter(s -> s.toLowerCase().startsWith(lower)).sorted().toList();
    }
}
