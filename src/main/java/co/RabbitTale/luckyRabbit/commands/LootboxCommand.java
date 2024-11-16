package co.RabbitTale.luckyRabbit.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.api.FeatureManager;
import co.RabbitTale.luckyRabbit.api.LicenseManager;
import co.RabbitTale.luckyRabbit.effects.CreatorEffects;
import co.RabbitTale.luckyRabbit.gui.LootboxListGUI;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbit.lootbox.entity.LootboxEntity;
import co.RabbitTale.luckyRabbit.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LootboxCommand implements CommandExecutor {

    private final LuckyRabbit plugin;
    public static final TextColor ERROR_COLOR = NamedTextColor.RED;
    public static final TextColor SUCCESS_COLOR = NamedTextColor.GREEN;
    public static final TextColor INFO_COLOR = TextColor.color(255, 200, 80);
    public static final TextColor SEPARATOR_COLOR = TextColor.color(105, 109, 119);
    public static final TextColor COMMAND_COLOR = TextColor.color(100, 180, 255);
    public static final TextColor ACTION_COLOR = TextColor.color(230, 120, 230);
    public static final TextColor ITEM_COLOR = TextColor.color(120, 230, 120);
    public static final TextColor TARGET_COLOR = TextColor.color(255, 200, 80);
    public static final TextColor NAME_COLOR = TextColor.color(131, 255, 207);
    public static final TextColor DESCRIPTION_COLOR = TextColor.color(180, 180, 180);

    private static final int ITEMS_PER_PAGE = 6; // Number of commands per page

    public LootboxCommand(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                showHelp((Player) sender, 1);
            } else {
                showConsoleHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        try {
            switch (subCommand) {
                case "help" -> {
                    if (sender instanceof Player) {
                        int page = 1;
                        if (args.length > 1) {
                            try {
                                page = Integer.parseInt(args[1]);
                            } catch (NumberFormatException ignored) {
                                // Use default page 1 if invalid number
                            }
                        }
                        showHelp((Player) sender, page);
                    } else {
                        showConsoleHelp(sender);
                    }
                }
                case "key" -> {
                    if (args.length == 1) {
                        List<Component> usage = LootboxTabCompleter.getCommandUsage("key");
                        sender.sendMessage(Component.empty());
                        for (Component line : usage) {
                            sender.sendMessage(line);
                        }
                        sender.sendMessage(Component.empty());
                    } else {
                        handleKey(sender, args);
                    }
                }
                case "reload" ->
                    handleReload(sender);
                case "animations" ->
                    showAnimations(sender);
                case "license" ->
                    showLicenseInfo(sender);
                case "config" -> {
                    if (!sender.hasPermission("luckyrabbit.admin.config")) {
                        sender.sendMessage(Component.text("You don't have permission to modify the configuration!")
                                .color(ERROR_COLOR));
                        return true;
                    }

                    if (args.length < 3) {
                        List<Component> usage = LootboxTabCompleter.getCommandUsage("config");
                        sender.sendMessage(Component.empty());
                        for (Component line : usage) {
                            sender.sendMessage(line);
                        }
                        sender.sendMessage(Component.empty());
                        return true;
                    }

                    String category = args[1].toLowerCase();
                    String action = args[2].toLowerCase();

                    if (category.equals("license-key")) {
                        switch (action) {
                            case "add" -> {
                                if (args.length < 4) {
                                    sender.sendMessage(Component.text("Usage: /lb config license-key add <key>")
                                            .color(ERROR_COLOR));
                                    return true;
                                }
                                String key = args[3];

                                // Save old key for comparison
                                String oldKey = plugin.getConfig().getString("license-key", "");

                                // Update config
                                plugin.getConfig().set("license-key", key);
                                plugin.saveConfig();

                                // Send initial message
                                if (!oldKey.equals(key)) {
                                    if (sender instanceof Player) {
                                        sender.sendMessage(Component.text()
                                                .append(Component.text("License key added: ", SUCCESS_COLOR))
                                                .append(Component.text(key, TARGET_COLOR))
                                                .build());
                                    } else {
                                        sender.sendMessage(Component.text("License key added: " + key));
                                    }
                                } else {
                                    if (sender instanceof Player) {
                                        sender.sendMessage(Component.text()
                                                .append(Component.text("License key unchanged: ", INFO_COLOR))
                                                .append(Component.text(key, TARGET_COLOR))
                                                .build());
                                    } else {
                                        sender.sendMessage(Component.text("License key unchanged: " + key));
                                    }
                                }

                                // Verify new license - this will trigger reload internally
                                plugin.getLicenseManager().verifyLicense(key, true);
                            }
                            case "remove" -> {
                                // Save old key for logging
                                String oldKey = plugin.getConfig().getString("license-key", "");

                                if (oldKey.isEmpty()) {
                                    sender.sendMessage(Component.text("No license key to remove!")
                                            .color(ERROR_COLOR));
                                    return true;
                                }

                                // Remove key
                                plugin.getConfig().set("license-key", "");
                                plugin.saveConfig();

                                // Send confirmation message
                                if (sender instanceof Player) {
                                    sender.sendMessage(Component.text()
                                            .append(Component.text("License key removed: ", SUCCESS_COLOR))
                                            .append(Component.text(oldKey, TARGET_COLOR))
                                            .build());
                                } else {
                                    sender.sendMessage(Component.text("License key removed: " + oldKey));
                                }

                                // Verify empty license - this will trigger reload internally
                                plugin.getLicenseManager().verifyLicense("", true);
                            }
                            default -> {
                                List<Component> usage = LootboxTabCompleter.getCommandUsage("config license-key");
                                sender.sendMessage(Component.empty());
                                for (Component line : usage) {
                                    sender.sendMessage(line);
                                }
                                sender.sendMessage(Component.empty());
                            }
                        }
                    } else {
                        sender.sendMessage(Component.text("Unknown config category: " + category)
                                .color(ERROR_COLOR));
                    }
                }
                default -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("This command can only be used by players!")
                                .color(ERROR_COLOR));
                        return true;
                    }
                    // Handle player-only commands
                    handlePlayerCommands(player, subCommand, args);
                }
            }
        } catch (Exception e) {
            Logger.error("Error executing command: " + subCommand, e);
            sender.sendMessage(Component.text("An error occurred while executing the command!")
                    .color(ERROR_COLOR));
        }

        return true;
    }

    // New method for console help
    private void showConsoleHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("=== LuckyRabbit Console Commands ===").color(INFO_COLOR));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("» /lb key add/remove <player> <id> <amount>").color(COMMAND_COLOR));
        sender.sendMessage(Component.text("» /lb config license_key <key>").color(COMMAND_COLOR)
                .append(Component.text(" - Configure plugin settings").color(DESCRIPTION_COLOR)));
        sender.sendMessage(Component.text("» /lb reload").color(COMMAND_COLOR));
        sender.sendMessage(Component.text("» /lb license").color(COMMAND_COLOR));
        sender.sendMessage(Component.text("» /lb animations").color(COMMAND_COLOR));
        sender.sendMessage(Component.empty());
    }

    // Modified methods to accept CommandSender instead of Player
    private void handleKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("luckyrabbit.admin.key")) {
            sender.sendMessage(Component.text("You don't have permission to manage keys!")
                    .color(ERROR_COLOR));
            return;
        }

        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /lootbox key <add/remove> <player> <id> <amount>")
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
            sender.sendMessage(Component.text("Invalid amount! Please enter a number.")
                    .color(ERROR_COLOR));
            return;
        }

        if (target == null) {
            sender.sendMessage(Component.text("Player not found!")
                    .color(ERROR_COLOR));
            return;
        }

        // Get lootbox to access formatted name
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
        if (lootbox == null) {
            sender.sendMessage(Component.text("Lootbox not found: " + id)
                    .color(ERROR_COLOR));
            return;
        }

        // Parse the formatted display name
        Component lootboxName = MiniMessage.miniMessage().deserialize(lootbox.getDisplayName());

        switch (action) {
            case "add" -> {
                plugin.getUserManager().addKeys(target.getUniqueId(), id, amount);
                // Message for admin
                Component adminMessage = Component.text("You gave ")
                        .color(DESCRIPTION_COLOR)
                        .append(Component.text(amount + "x ", ITEM_COLOR))
                        .append(Component.text("key(s) for ", DESCRIPTION_COLOR))
                        .append(lootboxName)
                        .append(Component.text(" to ", DESCRIPTION_COLOR))
                        .append(Component.text(target.getName(), TARGET_COLOR));
                sender.sendMessage(adminMessage);

                // Message for target player
                Component targetMessage = Component.text("You received ")
                        .color(DESCRIPTION_COLOR)
                        .append(Component.text(amount + "x ", ITEM_COLOR))
                        .append(Component.text("key(s) for ", DESCRIPTION_COLOR))
                        .append(lootboxName);
                target.sendMessage(targetMessage);
            }
            case "remove" -> {
                plugin.getUserManager().removeKeys(target.getUniqueId(), id, amount);
                Component message = Component.text("Successfully removed ")
                        .color(SUCCESS_COLOR)
                        .append(Component.text(amount + "x ", ITEM_COLOR))
                        .append(Component.text("key(s) for ", SUCCESS_COLOR))
                        .append(lootboxName)
                        .append(Component.text(" from ", SUCCESS_COLOR))
                        .append(Component.text(target.getName(), TARGET_COLOR));
                sender.sendMessage(message);
            }
            default ->
                sender.sendMessage(Component.text("Invalid action! Use 'add' or 'remove'")
                        .color(ERROR_COLOR));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("luckyrabbit.admin.reload")) {
            sender.sendMessage(Component.text("You don't have permission to reload the plugin!")
                    .color(ERROR_COLOR));
            return;
        }

        try {
            // Save current data
            plugin.getUserManager().saveAllUsers();
            plugin.getLootboxManager().saveAll();

            // Reload the plugin
            plugin.reload();

            // Send success message with current mode
            String planType = LicenseManager.isPremium() ? "PREMIUM"
                    : LicenseManager.isTrialActive() ? "TRIAL" : "FREE";
            int maxLootboxes = FeatureManager.getMaxLootboxes();

            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("Plugin reloaded successfully!", SUCCESS_COLOR));
            sender.sendMessage(Component.text()
                    .append(Component.text("Running in ", DESCRIPTION_COLOR))
                    .append(Component.text(planType, planType.equals("PREMIUM") ? SUCCESS_COLOR
                            : planType.equals("TRIAL") ? INFO_COLOR : ERROR_COLOR))
                    .append(Component.text(" mode", DESCRIPTION_COLOR))
                    .build());
            sender.sendMessage(Component.text()
                    .append(Component.text("Maximum lootboxes allowed: ", DESCRIPTION_COLOR))
                    .append(Component.text(maxLootboxes == -1 ? "Unlimited" : String.valueOf(maxLootboxes),
                            maxLootboxes == -1 ? SUCCESS_COLOR : INFO_COLOR))
                    .build());
            sender.sendMessage(Component.empty());
        } catch (Exception e) {
            sender.sendMessage(Component.text("An error occurred while reloading: " + e.getMessage())
                    .color(ERROR_COLOR));
            Logger.error("Error during reload:", e);
        }
    }

    private void showAnimations(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Available Animations:")
                .color(INFO_COLOR));

        sender.sendMessage(Component.text()
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("HORIZONTAL", ACTION_COLOR))
                .append(Component.text(" - ", SEPARATOR_COLOR))
                .append(Component.text("Classic horizontal spin animation", DESCRIPTION_COLOR))
                .build());

        sender.sendMessage(Component.text()
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("CIRCLE", ACTION_COLOR))
                .append(Component.text(" - ", SEPARATOR_COLOR))
                .append(Component.text("Items spin in a circle pattern", DESCRIPTION_COLOR))
                .build());

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("More animations coming soon!", INFO_COLOR));
        sender.sendMessage(Component.empty());
    }

    private void showLicenseInfo(CommandSender sender) {
        if (!sender.hasPermission("luckyrabbit.admin")) {
            sender.sendMessage(Component.text("You don't have permission to view license info!")
                    .color(ERROR_COLOR));
            return;
        }

        String licenseKey = plugin.getConfig().getString("license-key", "");
        boolean isPremium = LicenseManager.isPremium();
        boolean isTrial = LicenseManager.isTrialActive();

        // Header
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("License Information")
                .color(INFO_COLOR));

        // Status with proper color formatting
        Component statusText = Component.text()
                .append(Component.text("» ", SEPARATOR_COLOR))
                .append(Component.text("Status: ", DESCRIPTION_COLOR))
                .append(Component.text(isPremium ? "PREMIUM" : isTrial ? "TRIAL" : "FREE")
                        .color(isPremium ? SUCCESS_COLOR : isTrial ? INFO_COLOR : ERROR_COLOR)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                .build();
        sender.sendMessage(statusText);

        // License Key
        if (!licenseKey.isEmpty()) {
            Component keyMessage = Component.text("» ", SEPARATOR_COLOR)
                    .append(Component.text("License Key: ", DESCRIPTION_COLOR))
                    .append(Component.text(licenseKey, TARGET_COLOR));

            Component copyButton = Component.text(" [Copy]", SUCCESS_COLOR)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(licenseKey))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            Component.text("Click to copy license key")));

            sender.sendMessage(keyMessage.append(copyButton));
        } else {
            sender.sendMessage(Component.text("» ", SEPARATOR_COLOR)
                    .append(Component.text("License Key: ", DESCRIPTION_COLOR))
                    .append(Component.text("Not set", ERROR_COLOR)));
        }

        // Features
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Features:", INFO_COLOR));

        // Max Lootboxes
        int maxLootboxes = FeatureManager.getMaxLootboxes();
        sender.sendMessage(Component.text("» ", SEPARATOR_COLOR)
                .append(Component.text("Max Lootboxes: ", DESCRIPTION_COLOR))
                .append(Component.text(maxLootboxes == -1 ? "Unlimited" : String.valueOf(maxLootboxes),
                        maxLootboxes == -1 ? SUCCESS_COLOR : INFO_COLOR)));

        // Custom Animations
        boolean customAnimations = plugin.getFeatureManager().canUseCustomAnimations();
        sender.sendMessage(Component.text("» ", SEPARATOR_COLOR)
                .append(Component.text("Custom Animations: ", DESCRIPTION_COLOR))
                .append(Component.text(customAnimations ? "Yes" : "No",
                        customAnimations ? SUCCESS_COLOR : ERROR_COLOR)));

        // Advanced Features
        boolean advancedFeatures = plugin.getFeatureManager().canUseAdvancedFeatures();
        sender.sendMessage(Component.text("» ", SEPARATOR_COLOR)
                .append(Component.text("Advanced Features: ", DESCRIPTION_COLOR))
                .append(Component.text(advancedFeatures ? "Yes" : "No",
                        advancedFeatures ? SUCCESS_COLOR : ERROR_COLOR)));

        sender.sendMessage(Component.empty());
    }

    // New method to handle player-only commands
    private void handlePlayerCommands(Player player, String subCommand, String[] args) {
        switch (subCommand) {
            case "list" -> {
                if (args.length == 2) {
                    try {
                        int page = Integer.parseInt(args[1]);
                        LootboxListGUI.openGUI(player, page);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Invalid page number!", ERROR_COLOR));
                    }
                } else {
                    LootboxListGUI.openGUI(player, 1);
                }
            }
            case "create" ->
                handleCreate(player, args);
            case "delete" ->
                handleDelete(player, args);
            case "item" ->
                handleItem(player, args);
            case "place" ->
                handlePlace(player, args);
            case "particles" -> {
                // Only allow creators to use this command
                if (!CreatorEffects.isCreator(player.getUniqueId())) {
                    player.sendMessage(Component.text("This command is only for creators!")
                            .color(ERROR_COLOR));
                    return;
                }

                // Toggle particles visibility
                boolean newState = plugin.getCreatorEffects().toggleParticlesVisibility(player);
                player.sendMessage(Component.text("Creator particles are now ")
                        .color(DESCRIPTION_COLOR)
                        .append(Component.text(newState ? "visible" : "hidden")
                                .color(newState ? SUCCESS_COLOR : ERROR_COLOR))
                        .append(Component.text(" for you", DESCRIPTION_COLOR)));
            }
            case "creator" -> {
                // Only allow creators to use this command
                if (!CreatorEffects.isCreator(player.getUniqueId())) {
                    player.sendMessage(Component.text("This command is only for creators!")
                            .color(ERROR_COLOR));
                    return;
                }

                if (args.length == 1) {
                    showCreatorHelp(player);
                    return;
                }

                String creatorAction = args[1].toLowerCase();
                if (creatorAction.equals("particles")) {
                    if (args.length > 2 && args[2].equalsIgnoreCase("toggle")) {
                        boolean newState = plugin.getCreatorEffects().toggleParticlesVisibility(player);
                        player.sendMessage(Component.text("Creator particles are now ")
                                .color(DESCRIPTION_COLOR)
                                .append(Component.text(newState ? "visible" : "hidden")
                                        .color(newState ? SUCCESS_COLOR : ERROR_COLOR))
                                .append(Component.text(" for you", DESCRIPTION_COLOR)));
                    } else {
                        showCreatorHelp(player);
                    }
                } else {
                    showCreatorHelp(player);
                }
            }
            case "entity" -> {
                if (!player.hasPermission("luckyrabbit.admin.entity")) {
                    player.sendMessage(Component.text("You don't have permission to manage lootbox entities!")
                            .color(ERROR_COLOR));
                    return;
                }

                if (args.length < 2) {
                    // Show entity command help
                    List<Component> usage = LootboxTabCompleter.getCommandUsage("entity");
                    player.sendMessage(Component.empty());
                    for (Component line : usage) {
                        player.sendMessage(line);
                    }
                    player.sendMessage(Component.empty());
                    return;
                }

                String action = args[1].toLowerCase();
                switch (action) {
                    case "spawn" -> {
                        if (args.length < 3) {
                            List<Component> usage = LootboxTabCompleter.getCommandUsage("entity spawn");
                            player.sendMessage(Component.empty());
                            for (Component line : usage) {
                                player.sendMessage(line);
                            }
                            player.sendMessage(Component.empty());
                            return;
                        }
                        String id = args[2];
                        plugin.getLootboxManager().placeLootbox(player, id);
                    }
                    case "despawn" -> {
                        LootboxEntity targetEntity = plugin.getLootboxManager().getLootboxEntityAtTarget(player);
                        if (targetEntity != null) {
                            LootboxManager.RemoveResult result = plugin.getLootboxManager().removeLootboxEntity(targetEntity);
                            if (result != null) {
                                player.sendMessage(Component.text("Successfully despawned ")
                                        .color(SUCCESS_COLOR)
                                        .append(result.displayName())
                                        .append(Component.text(" ")
                                                .color(SUCCESS_COLOR))
                                        .append(result.locationText()));
                            }
                        } else {
                            player.sendMessage(Component.text("You must be looking at a lootbox entity!")
                                    .color(ERROR_COLOR));
                        }
                    }
                    default -> {
                        List<Component> usage = LootboxTabCompleter.getCommandUsage("entity");
                        player.sendMessage(Component.empty());
                        for (Component line : usage) {
                            player.sendMessage(line);
                        }
                        player.sendMessage(Component.empty());
                    }
                }
            }
            default ->
                showHelp(player, 1);
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbit.admin.create")) {
            player.sendMessage(Component.text("You don't have permission to create lootboxes!")
                    .color(ERROR_COLOR));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: ", DESCRIPTION_COLOR)
                    .append(Component.text("/lb ", SEPARATOR_COLOR))
                    .append(Component.text("create ", ACTION_COLOR))
                    .append(Component.text("<name>", ITEM_COLOR)));
            return;
        }

        // Get the name by joining all arguments except the first (command) and last (animation if exists)
        String name;
        AnimationType animationType;

        if (args.length > 2 && isAnimationType(args[args.length - 1])) {
            // If last argument is an animation type, join everything in between
            name = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
            try {
                animationType = AnimationType.valueOf(args[args.length - 1].toUpperCase());
            } catch (IllegalArgumentException e) {
                animationType = AnimationType.valueOf(plugin.getConfig().getString("animations.default-type", "HORIZONTAL"));
            }
        } else {
            // If no animation type, join all arguments after "create"
            name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            animationType = AnimationType.valueOf(plugin.getConfig().getString("animations.default-type", "HORIZONTAL"));
        }

        // Remove all color codes and formatting to check if name is empty
        String strippedName = name.replaceAll("<[^>]*>", "").trim();
        if (strippedName.isEmpty()) {
            player.sendMessage(Component.text("Lootbox name cannot be empty!", ERROR_COLOR));
            return;
        }

        try {
            plugin.getLootboxManager().createLootbox(name, animationType);
            player.sendMessage(Component.text("Successfully created lootbox: ", SUCCESS_COLOR)
                    .append(MiniMessage.miniMessage().deserialize(name)));
        } catch (IllegalStateException e) {
            // Handle limit reached error
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Cannot create more lootboxes!", ERROR_COLOR));
            player.sendMessage(Component.text(e.getMessage(), DESCRIPTION_COLOR));
            player.sendMessage(Component.empty());
        } catch (IllegalArgumentException e) {
            // Handle other errors (like duplicate names)
            player.sendMessage(Component.text(e.getMessage(), ERROR_COLOR));
        }
    }

    private boolean isAnimationType(String type) {
        try {
            AnimationType.valueOf(type.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbit.admin.delete")) {
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
        if (!player.hasPermission("luckyrabbit.admin.item")) {
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
            case "add" -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType().isAir()) {
                    player.sendMessage(Component.text("You must hold an item to add!")
                            .color(ERROR_COLOR));
                    return;
                }

                // Default values for rarity and chance
                String rarity = args.length > 3 ? args[3].toUpperCase() : "COMMON";
                double chance = args.length > 4 ? Double.parseDouble(args[4]) : 100.0;

                plugin.getLootboxManager().addItem(player, id, item, rarity, chance);
            }
            case "remove" ->
                plugin.getLootboxManager().removeItem(player, id);
            default ->
                player.sendMessage(Component.text("Invalid action! Use 'add' or 'remove'")
                        .color(ERROR_COLOR));
        }
    }

    private void handlePlace(Player player, String[] args) {
        if (!player.hasPermission("luckyrabbit.admin.place")) {
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

    private void showHelp(Player player, int page) {
        List<Component> commands = new ArrayList<>();

        // Basic commands - only show list and help for regular players
        commands.add(createCommandComponent("/lb list", "View list of lootboxes", null));
        commands.add(createCommandComponent("/lb help [page]", "Show this help menu", null));

        // Only show animations and license commands for admins
        if (player.hasPermission("luckyrabbit.admin")) {
            commands.add(createCommandComponent("/lb animations", "Show available animations", null));
            commands.add(createCommandComponent("/lb license", "Show license information", null));
        }

        if (player.hasPermission("luckyrabbit.admin")) {
            // Admin commands with colored parameters
            commands.add(createCommandComponent("/lb create", "Create a new lootbox",
                    Map.of("<name>", ITEM_COLOR, "[animation]", NAME_COLOR)));
            commands.add(createCommandComponent("/lb delete", "Delete a lootbox",
                    Map.of("<id>", ITEM_COLOR)));
            commands.add(createCommandComponent("/lb item add/remove", "Manage lootbox items",
                    Map.of("<id>", ITEM_COLOR)));
            commands.add(createCommandComponent("/lb entity spawn/despawn", "Manage lootbox entities",
                    Map.of("<id>", ITEM_COLOR)));
            commands.add(createCommandComponent("/lb key add/remove", "Manage lootbox keys",
                    Map.of("<player>", TARGET_COLOR, "<id>", ITEM_COLOR, "<amount>", NAME_COLOR)));
            commands.add(createCommandComponent("/lb reload", "Reload all configurations", null));
            commands.add(createCommandComponent("/lb config", "Configure plugin settings",
                    Map.of("license_key", ITEM_COLOR, "<key>", TARGET_COLOR)));
        }

        // Calculate total pages
        int totalPages = (int) Math.ceil(commands.size() / (double) ITEMS_PER_PAGE);
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        // Calculate start and end index for current page
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, commands.size());

        // Send header with page info
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text()
                .append(Component.text("=== ", SEPARATOR_COLOR))
                .append(Component.text("LuckyRabbitFoot Help ", INFO_COLOR))
                .append(Component.text("(Page " + page + "/" + totalPages + ") ", DESCRIPTION_COLOR))
                .append(Component.text("===", SEPARATOR_COLOR))
                .build());
        player.sendMessage(Component.empty());

        // Send commands for current page
        for (int i = startIndex; i < endIndex; i++) {
            player.sendMessage(commands.get(i));
        }

        player.sendMessage(Component.empty());

        // Add navigation buttons if there are multiple pages
        if (totalPages > 1) {
            var navigationBuilder = Component.text()
                    .append(Component.text("=== ", SEPARATOR_COLOR));

            // Previous page button
            if (page > 1) {
                navigationBuilder.append(
                        Component.text("[Previous] ", SUCCESS_COLOR)
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/lb help " + (page - 1)))
                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                        Component.text("Click to go to previous page")
                                ))
                );
            }

            // Page indicator
            navigationBuilder.append(Component.text("Page " + page + "/" + totalPages + " ", DESCRIPTION_COLOR));

            // Next page button
            if (page < totalPages) {
                navigationBuilder.append(
                        Component.text("[Next]", SUCCESS_COLOR)
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/lb help " + (page + 1)))
                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                        Component.text("Click to go to next page")
                                ))
                );
            }

            navigationBuilder.append(Component.text(" ===", SEPARATOR_COLOR));
            player.sendMessage(navigationBuilder.build());
        }

        player.sendMessage(Component.empty());
    }

    private Component createCommandComponent(String command, String description, Map<String, TextColor> paramColors) {
        Component commandComponent = Component.text("» ", SEPARATOR_COLOR);

        String[] parts = command.split(" ");
        if (paramColors != null) {
            // Split command into parts and color parameters
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                TextColor color;

                // Check if this part is a parameter
                if (paramColors.containsKey(part)) {
                    color = paramColors.get(part);
                } else if (part.startsWith("/lb")) {
                    // Color the base command with separator color
                    color = SEPARATOR_COLOR;
                } else if (part.equals("add") || part.equals("remove")
                        || part.equals("create") || part.equals("delete")
                        || part.equals("place") || part.equals("reload")
                        || part.equals("list") || part.equals("help")
                        || part.equals("animations") || part.equals("license")) {
                    // Color action words
                    color = ACTION_COLOR;
                } else {
                    // Check for parameter patterns
                    color = null;
                    for (Map.Entry<String, TextColor> entry : paramColors.entrySet()) {
                        if (part.contains(entry.getKey())) {
                            color = entry.getValue();
                            break;
                        }
                    }
                    if (color == null) {
                        color = COMMAND_COLOR;
                    }
                }

                // Add the part with appropriate color
                commandComponent = copyCommand(command, commandComponent, part, color, i, parts);
            }
        } else {
            // Split simple command to color the action part
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                TextColor color;

                if (part.startsWith("/lb")) {
                    color = SEPARATOR_COLOR;
                } else {
                    color = ACTION_COLOR;  // Color the action word
                }

                commandComponent = copyCommand(command, commandComponent, part, color, i, parts);
            }
        }

        return commandComponent
                .append(Component.text(" » ", SEPARATOR_COLOR)) // Changed from " - " to " » "
                .append(Component.text(description, DESCRIPTION_COLOR));
    }

    private static @NotNull
    Component copyCommand(String command, Component commandComponent, String part, TextColor color, int i, String[] parts) {
        commandComponent = commandComponent.append(
                Component.text(part, color)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(command))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                Component.text("Click to copy command")
                        ))
        );

        // Add space between parts
        if (i < parts.length - 1) {
            commandComponent = commandComponent.append(Component.text(" ", COMMAND_COLOR));
        }
        return commandComponent;
    }

    private void showCreatorHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Creator Commands ===")
                .color(INFO_COLOR));
        player.sendMessage(Component.empty());

        // Add creator commands with descriptions
        Component particlesCommand = Component.text("» ", SEPARATOR_COLOR)
                .append(Component.text("/lb ", SEPARATOR_COLOR))
                .append(Component.text("creator ", ACTION_COLOR))
                .append(Component.text("particles ", ITEM_COLOR))
                .append(Component.text("toggle", TARGET_COLOR))
                .append(Component.text(" - Toggle particle visibility", DESCRIPTION_COLOR));

        player.sendMessage(particlesCommand);
        // Add more creator commands here in the future

        player.sendMessage(Component.empty());
    }
}
