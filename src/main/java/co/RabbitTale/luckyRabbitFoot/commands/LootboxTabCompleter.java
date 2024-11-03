package co.RabbitTale.luckyRabbitFoot.commands;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.animation.AnimationType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LootboxTabCompleter implements TabCompleter {
    private static final List<String> MAIN_COMMANDS = Arrays.asList(
        "create", "delete", "item", "place", "key", "list"
    );
    private static final List<String> ITEM_ACTIONS = Arrays.asList("add", "remove");
    private static final List<String> KEY_ACTIONS = Arrays.asList("add", "remove");

    private final LuckyRabbitFoot plugin;

    public LootboxTabCompleter(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                              @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            // Basic command for all players
            completions.add("list");

            // Admin commands
            if (player.hasPermission("luckyrabbitfoot.admin")) {
                completions.addAll(MAIN_COMMANDS);
            }
            return filterCompletions(completions, args[0]);
        }

        // Subcommand completions
        if (args.length >= 2) {
            return switch (args[0].toLowerCase()) {
                case "create" -> handleCreateCompletion(args);
                case "delete", "place" -> handleLootboxIdCompletion(args);
                case "item" -> handleItemCompletion(args);
                case "key" -> handleKeyCompletion(args);
                default -> Collections.emptyList();
            };
        }

        return Collections.emptyList();
    }

    private List<String> handleCreateCompletion(String[] args) {
        if (args.length == 3) {
            return filterCompletions(
                Arrays.stream(AnimationType.values())
                    .map(type -> type.name().toLowerCase())
                    .collect(Collectors.toList()),
                args[2]
            );
        }
        return Collections.emptyList();
    }

    private List<String> handleLootboxIdCompletion(String[] args) {
        if (args.length == 2) {
            return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> handleItemCompletion(String[] args) {
        if (args.length == 2) {
            return filterCompletions(ITEM_ACTIONS, args[1]);
        } else if (args.length == 3) {
            return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> handleKeyCompletion(String[] args) {
        if (args.length == 2) {
            return filterCompletions(KEY_ACTIONS, args[1]);
        } else if (args.length == 3) {
            return filterCompletions(
                Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()),
                args[2]
            );
        } else if (args.length == 4) {
            return filterCompletions(plugin.getLootboxManager().getLootboxNames(), args[3]);
        } else if (args.length == 5) {
            return Arrays.asList("1", "5", "10", "25", "64");
        }
        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        if (input.isEmpty()) {
            return completions;
        }

        String lowerInput = input.toLowerCase();
        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(lowerInput))
            .collect(Collectors.toList());
    }
}
