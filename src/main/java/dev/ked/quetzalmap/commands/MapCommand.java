package dev.ked.quetzalmap.commands;

import dev.ked.quetzalmap.QuetzalMapPlugin;
import dev.ked.quetzalmap.model.MarkerType;
import dev.ked.quetzalmap.model.PlayerMapData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapCommand implements CommandExecutor, TabCompleter {

    private final QuetzalMapPlugin plugin;

    public MapCommand(QuetzalMapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return handleInfo(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "toggle":
                return handleToggle(sender, args);
            case "zoom":
                return handleZoom(sender, args);
            case "reload":
                return handleReload(sender);
            case "give":
                return handleGive(sender, args);
            case "info":
                return handleInfo(sender);
            default:
                sender.sendMessage("§cUnknown subcommand. Use /qmap for help.");
                return false;
        }
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        PlayerMapData data = plugin.getPlayerDataManager().getPlayerData(player);

        sender.sendMessage("§6§l=== QuetzalMap Info ===");
        sender.sendMessage("§7Zoom Level: §e" + data.getZoomLevel() + " blocks");
        sender.sendMessage("§7Total Markers: §e" + plugin.getMarkerManager().getMarkerCount());
        sender.sendMessage("");
        sender.sendMessage("§7Marker Visibility:");

        for (MarkerType type : MarkerType.values()) {
            if (type == MarkerType.CUSTOM) continue;
            String key = type.getConfigKey();
            boolean visible = data.isMarkerVisible(key);
            int count = plugin.getMarkerManager().getMarkerCountByType(key);
            String status = visible ? "§a✓" : "§c✗";
            sender.sendMessage("§7  " + status + " §e" + key + " §7(" + count + ")");
        }

        sender.sendMessage("");
        sender.sendMessage("§7Commands:");
        sender.sendMessage("§e/qmap toggle <type> §7- Toggle marker visibility");
        sender.sendMessage("§e/qmap zoom <level> §7- Change zoom level");
        sender.sendMessage("§e/qmap info §7- Show this info");

        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!sender.hasPermission("quetzalmap.toggle")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /qmap toggle <marker-type>");
            return false;
        }

        Player player = (Player) sender;
        String markerType = args[1].toLowerCase();
        PlayerMapData data = plugin.getPlayerDataManager().getPlayerData(player);

        // Check if marker type exists
        boolean found = false;
        for (MarkerType type : MarkerType.values()) {
            if (type.getConfigKey().equals(markerType)) {
                found = true;
                break;
            }
        }

        if (!found) {
            sender.sendMessage("§cUnknown marker type: §e" + markerType);
            return false;
        }

        // Toggle the marker
        data.toggleMarker(markerType);
        boolean visible = data.isMarkerVisible(markerType);

        String status = visible ? "§aenabled" : "§cdisabled";
        sender.sendMessage("§7Marker type §e" + markerType + " §7is now " + status + "§7.");

        return true;
    }

    private boolean handleZoom(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!sender.hasPermission("quetzalmap.zoom")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /qmap zoom <level>");
            return false;
        }

        Player player = (Player) sender;

        try {
            int zoomLevel = Integer.parseInt(args[1]);

            // Validate zoom level
            List<Integer> validZooms = plugin.getConfig().getIntegerList("map.zoom-levels");
            if (!validZooms.contains(zoomLevel)) {
                sender.sendMessage("§cInvalid zoom level. Valid levels: " + validZooms);
                return false;
            }

            PlayerMapData data = plugin.getPlayerDataManager().getPlayerData(player);
            data.setZoomLevel(zoomLevel);

            sender.sendMessage("§7Zoom level set to §e" + zoomLevel + " blocks§7.");

        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: " + args[1]);
            return false;
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("quetzalmap.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        plugin.reloadConfig();

        // Reload integrations
        plugin.getIntegrationManager().unloadIntegrations();
        plugin.getIntegrationManager().loadIntegrations();

        sender.sendMessage("§aQuetzalMap configuration reloaded!");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quetzalmap.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /qmap give <player>");
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return false;
        }

        boolean success = plugin.getMapManager().giveMapToPlayer(target);
        if (success) {
            sender.sendMessage("§7Given QuetzalMap to §e" + target.getName() + "§7.");
        } else {
            sender.sendMessage("§c" + target.getName() + " already has a QuetzalMap.");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("toggle", "zoom", "reload", "give", "info")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("toggle")) {
                List<String> types = new ArrayList<>();
                for (MarkerType type : MarkerType.values()) {
                    if (type != MarkerType.CUSTOM) {
                        types.add(type.getConfigKey());
                    }
                }
                return types.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("zoom")) {
                return plugin.getConfig().getIntegerList("map.zoom-levels")
                    .stream()
                    .map(String::valueOf)
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("give")) {
                return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
