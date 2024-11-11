package co.RabbitTale.luckyRabbit.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import net.kyori.adventure.text.Component;

public class FeatureManager {

    private static LicenseManager licenseManager = null;
    private static String currentPlanType = "FREE";
    private static LuckyRabbit plugin;

    public FeatureManager(LicenseManager licenseManager, LuckyRabbit plugin) {
        FeatureManager.licenseManager = licenseManager;
        FeatureManager.plugin = plugin;
        updatePlanType();
    }

    public static void updatePlanType() {
        String newPlanType = LicenseManager.isPremium() ? "PREMIUM"
                : LicenseManager.isTrialActive() ? "TRIAL"
                : "FREE";

        if (!newPlanType.equals(currentPlanType)) {
            notifyPlanChange(currentPlanType, newPlanType);
            currentPlanType = newPlanType;

            // Schedule plugin reload on next tick to ensure all messages are sent
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Notify all admins about the reload
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("luckyrabbit.admin")) {
                        player.sendMessage(Component.empty());
                        player.sendMessage(Component.text("Reloading plugin to apply changes...")
                                .color(LootboxCommand.INFO_COLOR));
                        player.sendMessage(Component.empty());
                    }
                }

                // Perform plugin reload
                plugin.reload();

                // Send confirmation message after reload
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("luckyrabbit.admin")) {
                        player.sendMessage(Component.empty());
                        player.sendMessage(Component.text("Plugin has been reloaded successfully!")
                                .color(LootboxCommand.SUCCESS_COLOR));
                        player.sendMessage(Component.text("All lootbox entities have been respawned.")
                                .color(LootboxCommand.DESCRIPTION_COLOR));
                        player.sendMessage(Component.empty());
                    }
                }
            });
        }
    }

    private static void notifyPlanChange(String oldPlan, String newPlan) {
        // Create the main status change message
        Component message = Component.text("License status changed: ")
                .color(LootboxCommand.DESCRIPTION_COLOR)
                .append(Component.text(oldPlan)
                        .color(getStatusColor(oldPlan)))
                .append(Component.text(" → ")
                        .color(LootboxCommand.SEPARATOR_COLOR))
                .append(Component.text(newPlan)
                        .color(getStatusColor(newPlan)));

        // Send to all online admins
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("luckyrabbit.admin")) {
                player.sendMessage(Component.empty());
                player.sendMessage(message);

                // Show features info if downgrading
                if (isDowngrade(oldPlan, newPlan)) {
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("Some features have been disabled!")
                            .color(LootboxCommand.ERROR_COLOR));

                    // If downgrading from TRIAL to FREE, show purchase link
                    if (oldPlan.equals("TRIAL") && newPlan.equals("FREE")) {
                        player.sendMessage(Component.empty());
                        player.sendMessage(Component.text("Your trial has expired!")
                                .color(LootboxCommand.ERROR_COLOR));
                        player.sendMessage(Component.text("Enjoyed the plugin? Get the full version ")
                                .color(LootboxCommand.DESCRIPTION_COLOR)
                                .append(Component.text("[HERE]")
                                        .color(LootboxCommand.SUCCESS_COLOR)
                                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(
                                                "https://builtbybit.com/resources/lucky-rabbit-lootboxes-3-days-trial.53920/"))
                                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                                Component.text("Click to visit the plugin page!")))));
                    }

                    // Show feature limitations
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("Current limitations:", LootboxCommand.INFO_COLOR));
                    player.sendMessage(Component.text(" • ")
                            .color(LootboxCommand.SEPARATOR_COLOR)
                            .append(Component.text("Maximum lootboxes: ", LootboxCommand.DESCRIPTION_COLOR))
                            .append(Component.text(getMaxLootboxes() == -1 ? "Unlimited" : String.valueOf(getMaxLootboxes()),
                                    getMaxLootboxes() == -1 ? LootboxCommand.SUCCESS_COLOR : LootboxCommand.ERROR_COLOR)));
                    player.sendMessage(Component.text("  • ")
                            .color(LootboxCommand.SEPARATOR_COLOR)
                            .append(Component.text("Custom animations: ", LootboxCommand.DESCRIPTION_COLOR))
                            .append(Component.text("Disabled", LootboxCommand.ERROR_COLOR)));
                    player.sendMessage(Component.text("  • ")
                            .color(LootboxCommand.SEPARATOR_COLOR)
                            .append(Component.text("Oraxen items: ", LootboxCommand.DESCRIPTION_COLOR))
                            .append(Component.text("Disabled", LootboxCommand.ERROR_COLOR)));
                    player.sendMessage(Component.text("  • ")
                            .color(LootboxCommand.SEPARATOR_COLOR)
                            .append(Component.text("Command rewards: ", LootboxCommand.DESCRIPTION_COLOR))
                            .append(Component.text("Disabled", LootboxCommand.ERROR_COLOR)));
                } else if (newPlan.equals("PREMIUM")) {
                    // Show welcome message for premium users
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("Thank you for purchasing Lucky Rabbit!")
                            .color(LootboxCommand.SUCCESS_COLOR));
                    player.sendMessage(Component.text("All features are now unlocked!")
                            .color(LootboxCommand.SUCCESS_COLOR));
                } else if (newPlan.equals("TRIAL")) {
                    // Show trial welcome message
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("Welcome to Lucky Rabbit Trial!")
                            .color(LootboxCommand.INFO_COLOR));
                    player.sendMessage(Component.text("You have access to premium features for 3 days.")
                            .color(LootboxCommand.DESCRIPTION_COLOR));
                    player.sendMessage(Component.text("Maximum lootboxes during trial: ")
                            .color(LootboxCommand.DESCRIPTION_COLOR)
                            .append(Component.text("5", LootboxCommand.INFO_COLOR)));
                }

                player.sendMessage(Component.empty());
            }
        }
    }

    private static boolean isDowngrade(String oldPlan, String newPlan) {
        int oldValue = getPlanValue(oldPlan);
        int newValue = getPlanValue(newPlan);
        return newValue < oldValue;
    }

    private static int getPlanValue(String plan) {
        return switch (plan) {
            case "PREMIUM" ->
                3;
            case "TRIAL" ->
                2;
            default ->
                1;
        };
    }

    private static net.kyori.adventure.text.format.TextColor getStatusColor(String status) {
        return switch (status) {
            case "PREMIUM" ->
                LootboxCommand.SUCCESS_COLOR;
            case "TRIAL" ->
                LootboxCommand.INFO_COLOR;
            default ->
                LootboxCommand.ERROR_COLOR;
        };
    }

    public static int getMaxLootboxes() {
        if (LicenseManager.isPremium()) {
            return -1; // Unlimited
        }
        if (LicenseManager.isTrialActive()) {
            return 5; // Trial users can have 5 custom lootboxes
        }
        return 2; // Free version limit
    }

    public boolean canUseCustomAnimations() {
        return LicenseManager.isPremium() || LicenseManager.isTrialActive();
    }

    public boolean canUseAdvancedFeatures() {
        return LicenseManager.isPremium() || LicenseManager.isTrialActive();
    }

    public static boolean canUseAnimation(String animationType) {
        if (LicenseManager.isPremium() || LicenseManager.isTrialActive()) {
            return false;
        }
        // Free version only allows HORIZONTAL animation
        return !"HORIZONTAL".equalsIgnoreCase(animationType);
    }

    public static boolean canUseOraxenItems() {
        return !LicenseManager.isPremium() && !LicenseManager.isTrialActive();
    }

    public static boolean canExecuteCommands() {
        return !LicenseManager.isPremium() && !LicenseManager.isTrialActive();
    }
}
