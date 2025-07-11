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
import co.RabbitTale.luckyRabbit.effects.CreatorEffects;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import net.kyori.adventure.text.Component;

public class LootboxTabCompleter implements TabCompleter {

    private final LuckyRabbit plugin;
    private static final List<String> RARITIES = Arrays.asList("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY");
    private static final List<String> CHANCES = Arrays.asList("5", "10", "15", "20", "25", "30", "35", "40", "45", "50");
    private static final List<String> SUPPORTED_ANIMATIONS = Arrays.stream(AnimationType.values())
            .map(Enum::name)
            .collect(Collectors.toList());
    private static final List<String> KEY_ACTIONS = Arrays.asList("add", "remove", "generate");
    private static final List<String> ITEM_ACTIONS = Arrays.asList("add", "remove");

    public LootboxTabCompleter(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Base commands
            List<String> commands = new ArrayList<>(Arrays.asList("list", "help"));
            if (sender instanceof Player && CreatorEffects.isCreator(((Player) sender).getUniqueId())) {
                commands.add("creator");
            }
            if (sender.hasPermission("luckyrabbit.admin")) {
                commands.addAll(Arrays.asList("create", "delete", "item", "spawn", "despawn", "key", "reload", "config", "animations"));
            }
            return filterCompletions(commands, args[0]);
        }

        if (!sender.hasPermission("luckyrabbit.admin")) {
            return completions;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (args.length == 2) {
                    List<String> lootboxes = plugin.getLootboxManager().getLootboxNamesAdmin().stream()
                            .filter(id -> !plugin.getLootboxManager().isExampleLootbox(id))
                            .collect(Collectors.toList());
                    return filterCompletions(lootboxes, args[1]);
                }
            }
            case "despawn" -> {
                if (args.length == 2) {
                    List<String> lootboxes = plugin.getLootboxManager().getLootboxNamesAdmin().stream()
                            .filter(id -> !plugin.getLootboxManager().isExampleLootbox(id))
                            .collect(Collectors.toList());
                    return filterCompletions(lootboxes, args[1]);
                }
            }
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

                // Handle generate subcommand differently
                if (args.length >= 3 && args[1].equalsIgnoreCase("generate")) {
                    if (args.length == 3) {
                        // Animation IDs for generate
                        return filterCompletions(SUPPORTED_ANIMATIONS, args[2]);
                    }
                    if (args.length == 4) {
                        // Plugin versions for generate
                        return filterCompletions(Arrays.asList("1.1.0", "1.0.0"), args[3]);
                    }
                    return new ArrayList<>();
                }

                // Handle add/remove subcommands
                if (args.length == 3) {
                    return filterCompletions(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()), args[2]);
                }
                if (args.length == 4) {
                    if (sender.hasPermission("luckyrabbit.admin")) {
                        return filterCompletions(plugin.getLootboxManager().getLootboxNamesAdmin(), args[3]);
                    } else {
                        return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[3]);
                    }
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
                    if (sender.hasPermission("luckyrabbit.admin")) {
                        return filterCompletions(plugin.getLootboxManager().getLootboxNamesAdmin(), args[1]);
                    } else {
                        return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[1]);
                    }
                }
            }
            case "list" -> {
                if (args.length == 2) {
                    return filterCompletions(Arrays.asList("1", "2", "3"), args[1]);
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
            case "item" ->
                Arrays.asList(
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
            case "key" ->
                Arrays.asList(
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
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("key generate ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("[animation_id] [version]", LootboxCommand.ITEM_COLOR))
                .append(Component.text(" - Generate animation keys (creators only)", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("\nType the command for more information", LootboxCommand.DESCRIPTION_COLOR)
                );
            case "list" ->
                Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("list ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("[number]", LootboxCommand.NAME_COLOR)),
                Component.text("Shows a list of all available lootboxes", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Options:", LootboxCommand.INFO_COLOR),
                Component.text("- number: ", LootboxCommand.DESCRIPTION_COLOR)
                .append(Component.text("Optional page number (default: 1)", LootboxCommand.NAME_COLOR))
                );
            case "create" ->
                Arrays.asList(
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
            case "key add" ->
                Arrays.asList(
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
            case "key remove" ->
                Arrays.asList(
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
            case "spawn" ->
                Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("spawn ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("lootbox_id", LootboxCommand.ITEM_COLOR)),
                Component.text("Spawns a lootbox entity at your location", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Required:", LootboxCommand.INFO_COLOR),
                Component.text("- lootbox_id: ", LootboxCommand.DESCRIPTION_COLOR)
                .append(Component.text("ID of the lootbox to spawn", LootboxCommand.ITEM_COLOR))
                );
            case "despawn" ->
                Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("despawn", LootboxCommand.ACTION_COLOR)),
                Component.text("Removes the lootbox entity you're looking at", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Note: ", LootboxCommand.INFO_COLOR)
                .append(Component.text("You must be looking at a lootbox entity", LootboxCommand.DESCRIPTION_COLOR))
                );
            case "config" ->
                Arrays.asList(
                Component.text("Available config commands:", LootboxCommand.INFO_COLOR),
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("config ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("license-key ", LootboxCommand.ITEM_COLOR))
                .append(Component.text("<add/remove>", LootboxCommand.TARGET_COLOR))
                .append(Component.text(" - Manage license key", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("\nType ", LootboxCommand.INFO_COLOR)
                .append(Component.text("/lb config license-key", LootboxCommand.COMMAND_COLOR))
                .append(Component.text(" for more information", LootboxCommand.INFO_COLOR))
                );
            case "config license-key" ->
                Arrays.asList(
                Component.text("Available license key commands:", LootboxCommand.INFO_COLOR),
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("config ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("license-key ", LootboxCommand.ITEM_COLOR))
                .append(Component.text("add ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("<key>", LootboxCommand.TARGET_COLOR))
                .append(Component.text(" - Add or update license key", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("config ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("license-key ", LootboxCommand.ITEM_COLOR))
                .append(Component.text("remove", LootboxCommand.ACTION_COLOR))
                .append(Component.text(" - Remove current license key", LootboxCommand.DESCRIPTION_COLOR)),
                Component.text("\nExample: ", LootboxCommand.INFO_COLOR)
                .append(Component.text("/lb config license-key add XXXX-XXXX-XXXX-XX", LootboxCommand.DESCRIPTION_COLOR))
                );
            case "place" ->
                Arrays.asList(
                Component.text("Command: ", LootboxCommand.INFO_COLOR)
                .append(Component.text("/lb ", LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text("place ", LootboxCommand.ACTION_COLOR))
                .append(Component.text("lootbox_id", LootboxCommand.ITEM_COLOR)),
                Component.text("Places a lootbox entity at your current location", LootboxCommand.DESCRIPTION_COLOR),
                Component.text("Required:", LootboxCommand.INFO_COLOR),
                Component.text("- lootbox_id: ", LootboxCommand.DESCRIPTION_COLOR)
                .append(Component.text("ID of the lootbox to place", LootboxCommand.ITEM_COLOR))
                );
            // Add other command usages with similar color formatting
            default ->
                List.of(
                Component.text("Unknown command. Type /lb help for a list of commands.", LootboxCommand.ERROR_COLOR)
                );
        };
    }
}
