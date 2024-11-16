package co.RabbitTale.luckyRabbit.lootbox.rewards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import co.RabbitTale.luckyRabbit.utils.Logger;

/*
 * RewardAction.java
 *
 * Handles execution of additional actions when a reward is given.
 * Supports command execution and permission management.
 *
 * Action Types:
 * - COMMAND: Executes one or more console commands
 * - PERMISSION: Manages LuckPerms group assignments
 *
 * Configuration Structure:
 * type: COMMAND/PERMISSION
 * commands:                  # For COMMAND type
 *   - "eco give {player} 1000"
 *   - "broadcast {player} won!"
 * group: "vip"              # For PERMISSION type
 * duration: "30d"           # For temporary permissions
 */
public record RewardAction(ActionType type, List<String> commands, String group, String duration) {

    /**
     * Creates a RewardAction from a configuration section.
     *
     * @param config Configuration section containing action data
     * @return New RewardAction instance or null if invalid
     */
    public static RewardAction fromConfig(ConfigurationSection config) {
        if (config == null) {
            return null;
        }

        String typeStr = config.getString("type");
        if (typeStr == null) {
            return null;
        }

        ActionType type = ActionType.valueOf(typeStr.toUpperCase());
        List<String> commands = config.getStringList("commands");
        String group = config.getString("group");
        String duration = config.getString("duration");

        return new RewardAction(type, commands, group, duration);
    }

    /**
     * Executes the action for a player. Handles both command execution and
     * permission management.
     *
     * @param player Target player
     */
    public void execute(Player player) {
        switch (type) {
            case COMMAND -> {
                if (commands != null) {
                    for (String command : commands) {
                        // Check if it's an economy command
                        if (command.startsWith("eco ")) {
                            // Check if Vault is installed
                            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                                Logger.warning("Attempted to execute economy command but Vault is not installed!");
                                Logger.warning("Command: " + command);
                                continue;
                            }
                        }

                        String processedCommand = command.replace("{player}", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                        Logger.debug("Executing command: " + processedCommand);
                    }
                }
            }
            case PERMISSION -> {
                if (group != null) {
                    String command;
                    if (duration != null && duration.equalsIgnoreCase("permanent")) {
                        command = "lp user " + player.getName() + " parent add " + group;
                    } else {
                        command = "lp user " + player.getName() + " parent addtemp " + group + " " + duration + " accumulate";
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    Logger.debug("Executing permission command: " + command);
                }
            }
        }
    }

    /**
     * Saves the action to a configuration section.
     *
     * @param config Configuration section to save to
     */
    public void save(ConfigurationSection config) {
        config.set("type", type.name());
        if (commands != null && !commands.isEmpty()) {
            config.set("commands", commands);
        }
        if (group != null) {
            config.set("group", group);
        }
        if (duration != null) {
            config.set("duration", duration);
        }
    }

    /**
     * Serializes the action to a map for storage.
     *
     * @return Map containing action data
     */
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.name());
        if (commands != null && !commands.isEmpty()) {
            data.put("commands", commands);
        }
        if (group != null) {
            data.put("group", group);
        }
        if (duration != null) {
            data.put("duration", duration);
        }
        return data;
    }

    /**
     * Available action types. COMMAND: Execute console commands PERMISSION:
     * Manage LuckPerms groups
     */
    public enum ActionType {
        COMMAND,
        PERMISSION
    }
}
