package co.RabbitTale.luckyRabbitFoot.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.gui.LootboxListGUI;
import co.RabbitTale.luckyRabbitFoot.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class LootboxCommand implements CommandExecutor {
    private final LuckyRabbitFoot plugin;
    private static final TextColor ERROR_COLOR = NamedTextColor.RED;
    private static final TextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    private static final TextColor INFO_COLOR = NamedTextColor.YELLOW;
    private static final TextColor SEPARATOR_COLOR = TextColor.color(105, 109, 119);
    private static final TextColor COMMAND_COLOR = NamedTextColor.GRAY;
    private static final TextColor ACTION_COLOR = TextColor.color(246, 135, 66);
    private static final TextColor ITEM_COLOR = TextColor.color(246, 66, 97);
    private static final TextColor TARGET_COLOR = TextColor.color(66, 198, 246);
    private static final TextColor NAME_COLOR = TextColor.color(236, 231, 64);
    private static final TextColor DESCRIPTION_COLOR = NamedTextColor.GRAY;

    public LootboxCommand(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            // Show list GUI for all players
            LootboxListGUI.openGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        try {
            switch (subCommand) {
                case "create" -> handleCreate(player, args);
                case "delete" -> handleDelete(player, args);
                case "item" -> handleItem(player, args);
                case "place" -> handlePlace(player, args);
                case "key" -> handleKey(player, args);
                case "list" -> LootboxListGUI.openGUI(player);
                default -> showHelp(player);
            }
        } catch (Exception e) {
            Logger.error("Error executing command: " + subCommand, e);
            player.sendMessage(Component.text("An error occurred while executing the command!")
                    .color(ERROR_COLOR));
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbitfoot.admin.create")) {
            player.sendMessage(Component.text("You don't have permission to create lootboxes!")
                    .color(ERROR_COLOR));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /lootbox create <name> [animation]")
                    .color(ERROR_COLOR));
            return;
        }

        String name = args[1];
        AnimationType animationType = args.length > 2 ?
            AnimationType.valueOf(args[2].toUpperCase()) :
            AnimationType.valueOf(plugin.getConfig().getString("animations.default-type", "HORIZONTAL"));

        try {
            plugin.getLootboxManager().createLootbox(name, animationType);
            player.sendMessage(Component.text("Successfully created lootbox: " + name)
                    .color(SUCCESS_COLOR));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text(e.getMessage()).color(ERROR_COLOR));
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbitfoot.admin.delete")) {
            player.sendMessage(Component.text("You don't have permission to delete lootboxes!")
                    .color(ERROR_COLOR));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /lootbox delete <id>")
                    .color(ERROR_COLOR));
            return;
        }

        String id = args[1];
        plugin.getLootboxManager().deleteLootbox(id);
        player.sendMessage(Component.text("Successfully deleted lootbox: " + id)
                .color(SUCCESS_COLOR));
    }

    private void handleItem(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbitfoot.admin.item")) {
            player.sendMessage(Component.text("You don't have permission to manage lootbox items!")
                    .color(ERROR_COLOR));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /lootbox item <add/remove> <id>")
                    .color(ERROR_COLOR));
            return;
        }

        String action = args[1].toLowerCase();
        String id = args[2];

        switch (action) {
            case "add" -> plugin.getLootboxManager().addItem(player, id);
            case "remove" -> plugin.getLootboxManager().removeItem(player, id);
            default -> player.sendMessage(Component.text("Invalid action! Use 'add' or 'remove'")
                    .color(ERROR_COLOR));
        }
    }

    private void handlePlace(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbitfoot.admin.place")) {
            player.sendMessage(Component.text("You don't have permission to place lootboxes!")
                    .color(ERROR_COLOR));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /lootbox place <id>")
                    .color(ERROR_COLOR));
            return;
        }

        String id = args[1];
        plugin.getLootboxManager().placeLootbox(player, id);
    }

    private void handleKey(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbitfoot.admin.key")) {
            player.sendMessage(Component.text("You don't have permission to manage keys!")
                    .color(ERROR_COLOR));
            return;
        }

        if (args.length < 5) {
            player.sendMessage(Component.text("Usage: /lootbox key <add/remove> <player> <id> <amount>")
                    .color(ERROR_COLOR));
            return;
        }

        String action = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);
        String id = args[3];
        int amount;

        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount! Please enter a number.")
                    .color(ERROR_COLOR));
            return;
        }

        if (target == null) {
            player.sendMessage(Component.text("Player not found!")
                    .color(ERROR_COLOR));
            return;
        }

        switch (action) {
            case "add" -> {
                plugin.getLootboxManager().addKeys(target.getUniqueId(), id, amount);
                // Message for admin
                Component adminMessage = Component.text("You gave ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(amount + " key(s)")
                        .color(NamedTextColor.GOLD))
                    .append(Component.text(" for lootbox ")
                        .color(NamedTextColor.GRAY))
                    .append(Component.text(id)
                        .color(NamedTextColor.YELLOW))
                    .append(Component.text(" to ")
                        .color(NamedTextColor.GRAY))
                    .append(Component.text(target.getName())
                        .color(NamedTextColor.AQUA));
                player.sendMessage(adminMessage);
            }
            case "remove" -> {
                plugin.getLootboxManager().removeKeys(target.getUniqueId(), id, amount);
                player.sendMessage(Component.text("Successfully removed " + amount + " key(s) from " + target.getName())
                    .color(SUCCESS_COLOR));
            }
            default -> player.sendMessage(Component.text("Invalid action! Use 'add' or 'remove'")
                    .color(ERROR_COLOR));
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.empty());

        // Basic commands
        player.sendMessage(Component.text()
            .append(Component.text("/lootbox ", SEPARATOR_COLOR))
            .append(Component.text("list", ACTION_COLOR))
            .append(Component.text(" » ", SEPARATOR_COLOR))
            .append(Component.text("View list of lootboxes", DESCRIPTION_COLOR))
            .build());

        if (player.hasPermission("luckyrabbitfoot.admin")) {
            // Create command
            player.sendMessage(Component.text()
                .append(Component.text("/lootbox ", SEPARATOR_COLOR))
                .append(Component.text("create ", COMMAND_COLOR))
                .append(Component.text("name ", ITEM_COLOR))
                .append(Component.text("animation ", ACTION_COLOR))
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("Create a new lootbox", DESCRIPTION_COLOR))
                .build());

            // Delete command
            player.sendMessage(Component.text()
                .append(Component.text("/lootbox ", SEPARATOR_COLOR))
                .append(Component.text("delete ", COMMAND_COLOR))
                .append(Component.text("id ", ITEM_COLOR))
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("Delete a lootbox", DESCRIPTION_COLOR))
                .build());

            // Item command
            player.sendMessage(Component.text()
                .append(Component.text("/lootbox ", SEPARATOR_COLOR))
                .append(Component.text("item ", COMMAND_COLOR))
                .append(Component.text("add/remove ", ACTION_COLOR))
                .append(Component.text("id ", ITEM_COLOR))
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("Manage lootbox items", DESCRIPTION_COLOR))
                .build());

            // Place command
            player.sendMessage(Component.text()
                .append(Component.text("/lootbox ", SEPARATOR_COLOR))
                .append(Component.text("place ", COMMAND_COLOR))
                .append(Component.text("id ", ITEM_COLOR))
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("Place a lootbox", DESCRIPTION_COLOR))
                .build());

            // Key command
            player.sendMessage(Component.text()
                .append(Component.text("/lootbox ", SEPARATOR_COLOR))
                .append(Component.text("key ", COMMAND_COLOR))
                .append(Component.text("add/remove ", ACTION_COLOR))
                .append(Component.text("player ", TARGET_COLOR))
                .append(Component.text("id ", ITEM_COLOR))
                .append(Component.text("amount ", NAME_COLOR))
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("Manage lootbox keys", DESCRIPTION_COLOR))
                .build());

            // Animation types
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Available Animations:")
                .color(SEPARATOR_COLOR));

            for (AnimationType type : AnimationType.values()) {
                player.sendMessage(Component.text()
                    .append(Component.text("» ", SEPARATOR_COLOR))
                    .append(Component.text(type.name(), ACTION_COLOR))
                    .append(Component.text(" - ", SEPARATOR_COLOR))
                    .append(Component.text(type.getDescription(), DESCRIPTION_COLOR))
                    .build());
            }
        }

        player.sendMessage(Component.empty());
    }
}
