package co.RabbitTale.luckyRabbit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import co.RabbitTale.luckyRabbit.api.AnimationManager;
import co.RabbitTale.luckyRabbit.api.FeatureManager;
import co.RabbitTale.luckyRabbit.api.LicenseManager;
import co.RabbitTale.luckyRabbit.api.LuckyRabbitAPI;
import co.RabbitTale.luckyRabbit.api.LuckyRabbitAPIImpl;
import co.RabbitTale.luckyRabbit.api.LuckyRabbitAPIProvider;
import co.RabbitTale.luckyRabbit.commands.CommandManager;
import co.RabbitTale.luckyRabbit.config.ConfigManager;
import co.RabbitTale.luckyRabbit.effects.CreatorEffects;
import co.RabbitTale.luckyRabbit.listeners.EntityListener;
import co.RabbitTale.luckyRabbit.listeners.ListenerManager;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;
import co.RabbitTale.luckyRabbit.user.UserManager;
import co.RabbitTale.luckyRabbit.utils.Logger;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;

/*
 * LuckyRabbit.java
 *
 * Main plugin class for the LuckyRabbit Minecraft plugin.
 * This class handles plugin initialization, configuration, and core functionality.
 *
 * Features:
 * - Lootbox system with customizable animations and rewards
 * - License management (Premium/Trial/Free modes)
 * - Integration with Vault Economy and Oraxen
 * - API for external plugin integration
 * - User data management and persistence
 *
 * Dependencies:
 * - Vault (optional) - For economy features
 * - Oraxen (optional) - For custom item support
 *
 * Configuration:
 * - Reads main config from config.yml
 * - Supports debug mode for detailed logging
 * - License key management through commands
 *
 */
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
    private LuckyRabbitAPI api;
    @Getter
    private UserManager userManager;
    @Getter
    private LicenseManager licenseManager;
    @Getter
    private FeatureManager featureManager;
    @Getter
    private CreatorEffects creatorEffects;
    @Getter
    private AnimationManager animationManager;

    @Getter
    private Economy economy = null;

    @Getter
    private boolean oraxenHooked = false;

    /**
     * Gets the singleton instance of the plugin.
     *
     * @return The plugin instance
     */
    public static LuckyRabbit getInstance() {
        return instance;
    }

    /**
     * Gets the config manager for this plugin.
     *
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the lootbox manager for this plugin.
     *
     * @return The lootbox manager
     */
    public LootboxManager getLootboxManager() {
        return lootboxManager;
    }

    /**
     * Gets the command manager for this plugin.
     *
     * @return The command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Gets the listener manager for this plugin.
     *
     * @return The listener manager
     */
    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    /**
     * Gets the API for this plugin.
     *
     * @return The plugin API
     */
    public LuckyRabbitAPI getApi() {
        return api;
    }

    /**
     * Gets the user manager for this plugin.
     *
     * @return The user manager
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Gets the license manager for this plugin.
     *
     * @return The license manager
     */
    public LicenseManager getLicenseManager() {
        return licenseManager;
    }

    /**
     * Gets the feature manager for this plugin.
     *
     * @return The feature manager
     */
    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    /**
     * Gets the creator effects manager for this plugin.
     *
     * @return The creator effects manager
     */
    public CreatorEffects getCreatorEffects() {
        return creatorEffects;
    }

    /**
     * Gets the animation manager for this plugin.
     *
     * @return The animation manager
     */
    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    /**
     * Gets the economy instance for this plugin.
     *
     * @return The economy instance
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Called when the plugin is enabled. Initializes all managers, loads
     * configurations, and sets up integrations.
     */
    @Override
    public void onEnable() {
        instance = this;

        // Load config first
        saveDefaultConfig();
        reloadConfig();

        // Initialize logger with debug setting
        Logger.init(this);

        // Setup integrations and collect status
        List<String> hookedPlugins = new ArrayList<>();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.licenseManager = new LicenseManager(this);
        this.featureManager = new FeatureManager(licenseManager, this);
        this.animationManager = new AnimationManager(this);
        this.lootboxManager = new LootboxManager(this);
        this.commandManager = new CommandManager(this);
        this.listenerManager = new ListenerManager(this);
        this.api = new LuckyRabbitAPIImpl(this);
        LuckyRabbitAPIProvider.setAPI(this.api);
        this.userManager = new UserManager(this);
        this.creatorEffects = new CreatorEffects(this);

        // Load configurations
        configManager.loadConfigs();

        // Register commands and listeners
        commandManager.registerCommands();
        listenerManager.registerListeners();

        // Initialize API
        LuckyRabbitAPIProvider.setAPI(this.api);

        // Register entity listener
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);

        // Setup integrations and display startup banner after all plugins are loaded
        getServer().getScheduler().runTaskLater(this, () -> {
            // Setup Vault
            if (setupEconomy()) {
                hookedPlugins.add("Vault Economy");
            }

            // Setup Oraxen with longer delay
            if (setupOraxen()) {
                hookedPlugins.add("Oraxen");
            }

            // Get current plan type (bez ponownego sprawdzania)
            String planType;
            if (LicenseManager.isPremium()) {
                planType = "PREMIUM";
            } else if (LicenseManager.isTrialActive()) {
                planType = "TRIAL";
            } else {
                planType = "FREE";
            }

            boolean debugMode = getConfig().getBoolean("settings.debug", false);

            // Display startup banner
            Logger.info("==========================================");
            Logger.info("        LuckyRabbit v" + getDescription().getVersion() + (debugMode ? " - DEBUG" : ""));
            Logger.info("        Running in " + planType + " mode");
            if (!hookedPlugins.isEmpty()) {
                Logger.info("        Hooked plugins: " + String.join(", ", hookedPlugins));
            }
            Logger.info("==========================================");

            // Load lootboxes based on plan
            if (LicenseManager.isPremium()) {
                lootboxManager.loadLootboxes();
            } else {
                Logger.warning("Running in limited mode. Some features are disabled.");
                lootboxManager.loadLimitedLootboxes();
            }

            // Respawn entities
            lootboxManager.respawnEntities();

        }, 100L);
    }

    /**
     * Called when the plugin is disabled. Saves all data and cleans up
     * resources.
     */
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
     * Reloads the plugin configuration and verifies license. This includes: -
     * Reloading config files - Verifying license status - Reloading lootboxes -
     * Respawning entities
     */
    public void reload() {
        reloadConfig();
        String licenseKey = getConfig().getString("license-key", "");

        // First reload configs
        configManager.loadConfigs();

        // Sprawdź licencję tylko jeśli nie jest aktualnie weryfikowana
        if (!LicenseManager.isVerifying()) {
            if (!licenseKey.isEmpty()) {
                licenseManager.verifyLicense(licenseKey, false);
            } else {
                LicenseManager.checkTrialStatus();
            }

            // Wait for license check to complete
            try {
                Thread.sleep(1000); // Give time for async license check
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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

    /**
     * Sets up the economy integration with Vault. This is optional - plugin
     * will work without economy features if Vault is not present.
     *
     * @return true if economy was successfully set up, false otherwise
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            Logger.warning("Vault plugin not found - economy features will be disabled");
            return false;
        }

        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (economyProvider == null) {
            Logger.warning("Vault economy provider not found - economy features will be disabled");
            return false;
        }

        economy = economyProvider.getProvider();
        Logger.info("Found Vault economy provider: " + economy.getName());
        return true;
    }

    /**
     * Sets up the Oraxen integration. This is optional - plugin will work
     * without custom item features if Oraxen is not present.
     *
     * @return true if Oraxen was successfully set up, false otherwise
     */
    private boolean setupOraxen() {
        if (getServer().getPluginManager().getPlugin("Oraxen") == null) {
            Logger.warning("Oraxen plugin not found - custom item features will be limited");
            return false;
        }

        try {
            Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            Logger.info("Found Oraxen - custom item features are available");
            oraxenHooked = true;
            return true;
        } catch (ClassNotFoundException e) {
            Logger.warning("Failed to hook into Oraxen: " + e.getMessage());
            return false;
        }
    }
}
