package co.RabbitTale.luckyRabbit.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import net.kyori.adventure.text.Component;

public class FeatureManager {
    private static LicenseManager licenseManager = null;
    private static String currentPlanType = "FREE";

    public FeatureManager(LicenseManager licenseManager) {
        FeatureManager.licenseManager = licenseManager;
        updatePlanType();
    }

    public static void updatePlanType() {
        String newPlanType = LicenseManager.isPremium() ? "PREMIUM"
                         : LicenseManager.isTrialActive() ? "TRIAL"
                         : "FREE";

        if (!newPlanType.equals(currentPlanType)) {
            notifyPlanChange(currentPlanType, newPlanType);
            currentPlanType = newPlanType;
        }
    }

    private static void notifyPlanChange(String oldPlan, String newPlan) {
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
                    player.sendMessage(Component.text("Some features have been disabled!")
                        .color(LootboxCommand.ERROR_COLOR));
                    player.sendMessage(Component.text("Maximum lootboxes: " + getMaxLootboxes())
                        .color(LootboxCommand.DESCRIPTION_COLOR));
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
            case "PREMIUM" -> 3;
            case "TRIAL" -> 2;
            default -> 1;
        };
    }

    private static net.kyori.adventure.text.format.TextColor getStatusColor(String status) {
        return switch (status) {
            case "PREMIUM" -> LootboxCommand.SUCCESS_COLOR;
            case "TRIAL" -> LootboxCommand.INFO_COLOR;
            default -> LootboxCommand.ERROR_COLOR;
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
}
