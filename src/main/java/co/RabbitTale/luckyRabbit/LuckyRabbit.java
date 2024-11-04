package co.RabbitTale.luckyRabbit;

import co.RabbitTale.luckyRabbit.api.LootboxAPI;
import co.RabbitTale.luckyRabbit.api.LicenseManager;
import co.RabbitTale.luckyRabbit.api.FeatureManager;
import co.RabbitTale.luckyRabbit.commands.CommandManager;
import co.RabbitTale.luckyRabbit.config.ConfigManager;
import co.RabbitTale.luckyRabbit.listeners.ListenerManager;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;
import co.RabbitTale.luckyRabbit.utils.Logger;
import co.RabbitTale.luckyRabbit.user.UserManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class LuckyRabbit extends JavaPlugin {

    @Getter
    private static LuckyRabbit instance;
    @Getter
    private ConfigManager configManager;
    @Getter
    private LootboxManager lootboxManager;
    @Getter
    private CommandManager commandManager;
    @Getter
    private ListenerManager listenerManager;
    @Getter
    private LootboxAPI api;
    @Getter
    private UserManager userManager;
    @Getter
    private LicenseManager licenseManager;
    @Getter
    private FeatureManager featureManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers first
        this.configManager = new ConfigManager(this);
        this.licenseManager = new LicenseManager(this);
        this.featureManager = new FeatureManager(licenseManager);
        this.lootboxManager = new LootboxManager(this);
        this.commandManager = new CommandManager(this);
        this.listenerManager = new ListenerManager(this);
        this.api = new LootboxAPI(this);
        this.userManager = new UserManager(this);

        // Load configurations
        configManager.loadConfigs();
        saveDefaultConfig();

        // Check license status first and display plan info
        String licenseKey = getConfig().getString("license-key", "");
        String planType;

        // Wait for license check to complete
        if (!licenseKey.isEmpty()) {
            licenseManager.verifyLicense(licenseKey);
            try {
                Thread.sleep(1000); // Give time for async license check
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            Logger.warning("No license key found! Checking trial status...");
            LicenseManager.checkTrialStatus();
        }

        // Get plan type from FeatureManager
        if (LicenseManager.isPremium()) {
            planType = "PREMIUM";
        } else if (LicenseManager.isTrialActive()) {
            planType = "TRIAL";
        } else {
            planType = "FREE";
        }

        // Display startup banner with plan info
        Logger.info("==========================================");
        Logger.info("        LuckyRabbitFoot v" + getDescription().getVersion());
        Logger.info("        Running in " + planType + " mode");
        Logger.info("==========================================");

        // Register commands and listeners
        commandManager.registerCommands();
        listenerManager.registerListeners();

        // Load lootboxes based on plan
        if (LicenseManager.isPremium()) {
            lootboxManager.loadLootboxes();
        } else {
            Logger.warning("Running in limited mode. Some features are disabled.");
            lootboxManager.loadLimitedLootboxes();
        }

        Logger.info("Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save all data and remove entities
        if (lootboxManager != null) {
            lootboxManager.saveAll();
            lootboxManager.removeAllEntities();
        }

        // Save all user data when the plugin disables
        userManager.saveAllUsers();

        Logger.info("Plugin disabled successfully!");
    }

    /**
     * Reloads the plugin configuration and verifies license
     */
    public void reload() {
        reloadConfig();
        String licenseKey = getConfig().getString("license-key", "");

        // First reload configs
        configManager.loadConfigs();

        // Then check license status
        if (!licenseKey.isEmpty()) {
            licenseManager.verifyLicense(licenseKey);
        } else {
            LicenseManager.checkTrialStatus();
        }

        // Wait for license check to complete
        try {
            Thread.sleep(1000); // Give time for async license check
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reload lootboxes based on plan
        if (LicenseManager.isPremium()) {
            Logger.info("Reloading with PREMIUM access");
            lootboxManager.loadLootboxes();
        } else {
            String planType = LicenseManager.isTrialActive() ? "TRIAL" : "FREE";
            int maxLootboxes = FeatureManager.getMaxLootboxes();

            Logger.info("Reloading in " + planType + " mode");
            Logger.info("Maximum lootboxes allowed: " + maxLootboxes);

            // Force reload with limited mode
            lootboxManager.loadLimitedLootboxes();
        }

        Logger.info("Plugin reloaded successfully!");
    }
}
