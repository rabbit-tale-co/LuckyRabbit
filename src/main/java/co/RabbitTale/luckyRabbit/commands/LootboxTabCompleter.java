package co.RabbitTale.luckyRabbit.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import net.kyori.adventure.text.Component;

public class LootboxTabCompleter implements TabCompleter {
    private final LuckyRabbit plugin;
    private static final List<String> RARITIES = Arrays.asList("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY");
    private static final List<String> CHANCES = Arrays.asList("5", "10", "15", "20", "25", "30", "35", "40", "45", "50");
    private static final List<String> SUPPORTED_ANIMATIONS = Arrays.asList("HORIZONTAL", "CIRCLE");
    private static final List<String> KEY_ACTIONS = Arrays.asList("add", "remove");
    private static final List<String> ITEM_ACTIONS = Arrays.asList("add", "remove");

    public LootboxTabCompleter(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Base commands
            List<String> commands = new ArrayList<>(Arrays.asList("list", "help", "animations", "license"));
            if (sender.hasPermission("luckyrabbit.admin")) {
                commands.addAll(Arrays.asList("create", "delete", "item", "place", "key", "reload"));
            }
            return filterCompletions(commands, args[0]);
        }

        if (!sender.hasPermission("luckyrabbit.admin")) {
            return completions;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length == 2) {
                    completions.add("<name>");
                    return filterCompletions(completions, args[1]);
                }
                if (args.length == 3) {
                    return filterCompletions(SUPPORTED_ANIMATIONS, args[2]);
                }
            }
            case "key" -> {
                if (args.length == 2) {
                    return filterCompletions(KEY_ACTIONS, args[1]);
                }
                if (args.length == 3) {
                    return filterCompletions(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()), args[2]);
                }
                if (args.length == 4) {
                    return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[3]);
                }
                if (args.length == 5) {
                    return filterCompletions(Arrays.asList("1", "5", "10", "25", "50", "100"), args[4]);
                }
            }
            case "item" -> {
                if (args.length == 2) {
                    return filterCompletions(ITEM_ACTIONS, args[1]);
                }
                if (args.length == 3) {
                    return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[2]);
                }
                if (args.length == 4 && args[1].equals("add")) {
                    return filterCompletions(RARITIES, args[3]);
                }
                if (args.length == 5 && args[1].equals("add")) {
                    return filterCompletions(CHANCES, args[4]);
                }
            }
            case "delete", "place" -> {
                if (args.length == 2) {
                    return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[1]);
                }
            }
            case "list" -> {
                if (args.length == 2) {
                    return filterCompletions(Arrays.asList("1", "2", "3"), args[1]);
                }
            }
            case "license" -> {
                if (args.length == 2) {
                    return filterCompletions(List.of("info"), args[1]);
                }
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static List<Component> getCommandUsage(String command) {
        return switch (command.toLowerCase()) {
            case "item" -> Arrays.asList(
                Component.text("Available item commands:", LootboxCommand.INFO_COLOR),
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("item add ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("lootbox_id ", LootboxCommand.ITEM_COLOR))
                    .append(Component.text("rarity ", LootboxCommand.NAME_COLOR))
                    .append(Component.text("chance", LootboxCommand.TARGET_COLOR))
                    .append(Component.text(" - Add held item to lootbox", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("item remove ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("lootbox_id", LootboxCommand.ITEM_COLOR))
                    .append(Component.text(" - Remove held item from lootbox", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("\nType the command for more information", LootboxCommand.DESCRIPTION_COLOR)
            );
            case "key" -> Arrays.asList(
                Component.text("Available key commands:", LootboxCommand.INFO_COLOR),
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("key add ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("player ", LootboxCommand.TARGET_COLOR))
                    .append(Component.text("lootbox_id ", LootboxCommand.ITEM_COLOR))
                    .append(Component.text("amount", LootboxCommand.NAME_COLOR))
                    .append(Component.text(" - Give keys to a player", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("key remove ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("player ", LootboxCommand.TARGET_COLOR))
                    .append(Component.text("lootbox_id ", LootboxCommand.ITEM_COLOR))
                    .append(Component.text("amount", LootboxCommand.NAME_COLOR))
                    .append(Component.text(" - Remove keys from a player", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("\nType the command for more information", LootboxCommand.DESCRIPTION_COLOR)
            );
            case "list" -> Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("list ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("[number]", LootboxCommand.NAME_COLOR)),
                Component.text("Shows a list of all available lootboxes", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Options:", LootboxCommand.INFO_COLOR),
                Component.text("- number: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Optional page number (default: 1)", LootboxCommand.NAME_COLOR))
            );
            case "create" -> Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("create ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("name", LootboxCommand.ITEM_COLOR))
                    .append(Component.text(" [", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("animation", LootboxCommand.NAME_COLOR))
                    .append(Component.text("]", LootboxCommand.SEPARATOR_COLOR)),
                Component.text("Creates a new lootbox", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Required:", LootboxCommand.INFO_COLOR),
                Component.text("- name: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Name of the lootbox", LootboxCommand.ITEM_COLOR)),
                Component.text("Optional:", LootboxCommand.INFO_COLOR),
                Component.text("- animation: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Animation type (HORIZONTAL, CIRCLE)", LootboxCommand.NAME_COLOR))
            );
            case "key add" -> Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("key ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("add ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("player", LootboxCommand.TARGET_COLOR))
                    .append(Component.text(" ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("id", LootboxCommand.ITEM_COLOR))
                    .append(Component.text(" ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("amount", LootboxCommand.NAME_COLOR)),
                Component.text("Gives lootbox keys to a player", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Required:", LootboxCommand.INFO_COLOR),
                Component.text("- player: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Target player name", LootboxCommand.TARGET_COLOR)),
                Component.text("- id: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Lootbox ID", LootboxCommand.ITEM_COLOR)),
                Component.text("- amount: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Number of keys to give", LootboxCommand.NAME_COLOR))
            );
            case "key remove" -> Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                    .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("key ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("remove ", LootboxCommand.ACTION_COLOR))
                    .append(Component.text("player", LootboxCommand.TARGET_COLOR))
                    .append(Component.text(" ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("id", LootboxCommand.ITEM_COLOR))
                    .append(Component.text(" ", LootboxCommand.SEPARATOR_COLOR))
                    .append(Component.text("amount", LootboxCommand.NAME_COLOR)),
                Component.text("Removes lootbox keys from a player", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Required:", LootboxCommand.INFO_COLOR),
                Component.text("- player: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Target player name", LootboxCommand.TARGET_COLOR)),
                Component.text("- id: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Lootbox ID", LootboxCommand.ITEM_COLOR)),
                Component.text("- amount: ", LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text("Number of keys to remove", LootboxCommand.NAME_COLOR))
            );
            // Add other command usages with similar color formatting
            default -> List.of(
                    Component.text("Unknown command. Type /lb help for a list of commands.", LootboxCommand.ERROR_COLOR)
            );
        };
    }
}
