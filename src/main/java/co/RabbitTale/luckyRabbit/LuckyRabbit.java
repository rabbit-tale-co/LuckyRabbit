package co.RabbitTale.luckyRabbit;

import org.bukkit.plugin.java.JavaPlugin;

import co.RabbitTale.luckyRabbit.api.FeatureManager;
import co.RabbitTale.luckyRabbit.api.LicenseManager;
import co.RabbitTale.luckyRabbit.api.LootboxAPI;
import co.RabbitTale.luckyRabbit.commands.CommandManager;
import co.RabbitTale.luckyRabbit.config.ConfigManager;
import co.RabbitTale.luckyRabbit.listeners.ListenerManager;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;
import co.RabbitTale.luckyRabbit.user.UserManager;
import co.RabbitTale.luckyRabbit.utils.Logger;
import lombok.Getter;

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

        // Load config first
        saveDefaultConfig();
        reloadConfig();

        // Initialize logger with debug setting
        Logger.init(this);

        // Initialize managers
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

        // Check license status first and display plan info
        String licenseKey = getConfig().getString("license-key", "");
        String planType;

        // Wait for license check to complete
        if (!licenseKey.isEmpty()) {
            Logger.debug("Found license key, verifying...");
            licenseManager.verifyLicense(licenseKey);
            try {
                Thread.sleep(1000); // Give time for async license check
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            Logger.debug("No license key found, checking trial status...");
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

        // Respawn all lootbox entities
        lootboxManager.respawnEntities();

        // Log debug mode status
        boolean debugMode = getConfig().getBoolean("settings.debug", false);
        if (debugMode) {
            Logger.success("Plugin enabled successfully! - DEBUG");
        } else {
            Logger.success("Plugin enabled successfully!");
        }
    }

    @Override
    public void onDisable() {
        // Save all data and cleanup entities
        if (lootboxManager != null) {
            lootboxManager.saveAll();
            lootboxManager.cleanup();
        }

        // Save all user data
        if (userManager != null) {
            userManager.saveAllUsers();
        }

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

        // Cleanup existing entities
        lootboxManager.cleanup();

        // Reload lootboxes based on plan
        if (LicenseManager.isPremium()) {
            Logger.success("Reloading with PREMIUM access");
            lootboxManager.loadLootboxes();
        } else {
            String planType = LicenseManager.isTrialActive() ? "TRIAL" : "FREE";
            int maxLootboxes = FeatureManager.getMaxLootboxes();

            Logger.info("Reloading in " + planType + " mode");
            Logger.info("Maximum lootboxes allowed: " + maxLootboxes);

            lootboxManager.loadLimitedLootboxes();
        }

        // Respawn all entities
        lootboxManager.respawnEntities();

        Logger.success("Plugin reloaded successfully!");
    }
}
