package co.RabbitTale.luckyRabbit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import co.RabbitTale.luckyRabbit.commands.CommandManager;
import co.RabbitTale.luckyRabbit.config.ConfigManager;
import co.RabbitTale.luckyRabbit.effects.CreatorEffects;
import co.RabbitTale.luckyRabbit.listeners.EntityListener;
import co.RabbitTale.luckyRabbit.listeners.ListenerManager;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;
import co.RabbitTale.luckyRabbit.user.UserManager;
import co.RabbitTale.luckyRabbit.utils.Logger;
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
 * TODO:
 *  - add multiple entity title column (max 5) (for example title and description)
 *  - add gui to choose how many creates user would like to open (1..n)
 *  - add minecraft:kill @e[type=armor_stand,distance=..3] force to /lb entity despawn command
 *  - remove old license code
 *  - manager to access *premium* animations via yml file from patreon
 *
 * FIXME:
 *  -
 *
 */
public class LuckyRabbit extends JavaPlugin {

    private static LuckyRabbit instance;

    private ConfigManager configManager;
    private LootboxManager lootboxManager;
    private UserManager userManager;
    private CreatorEffects creatorEffects;

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
        this.lootboxManager = new LootboxManager(this);
        CommandManager commandManager = new CommandManager(this);
        ListenerManager listenerManager = new ListenerManager(this);
        this.userManager = new UserManager(this);
        this.creatorEffects = new CreatorEffects(this);

        // Load configurations
        configManager.loadConfigs();
        lootboxManager.loadLootboxes();

        // Clean up any orphaned entities from previous server crashes
        lootboxManager.cleanupAllOrphanedEntities();

        // Clean up any orphaned creator parrots from previous server crashes
        creatorEffects.cleanupAllOrphanedParrots();

        // Register commands and listeners
        commandManager.registerCommands();
        listenerManager.registerListeners();

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

            boolean debugMode = getConfig().getBoolean("settings.debug", false);

            // Display startup banner
            Logger.info("==========================================");
            Logger.info("        LuckyRabbit v" + getDescription().getVersion() + (debugMode ? " - DEBUG" : ""));
            if (!hookedPlugins.isEmpty()) {
                Logger.info("        Hooked plugins: " + String.join(", ", hookedPlugins));
            }
            Logger.info("==========================================");

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

        // Clean up creator parrots
        if (creatorEffects != null) {
            creatorEffects.cleanupAllParrots();
        }

        // Save all user data
        if (userManager != null) {
            userManager.saveAllUsers();
        }

        Logger.info("Plugin disabled successfully!");
        instance = null;
    }

    /**
     * Reloads the plugin configuration and verifies license. This includes: -
     * Reloading config files - Verifying license status - Reloading lootboxes -
     * Respawning entities
     */
    public void reload() {
        reloadConfig();

        // First reload configs
        configManager.loadConfigs();

        // Cleanup existing entities and any orphaned ones
        lootboxManager.cleanup();
        lootboxManager.cleanupAllOrphanedEntities();

        // Clean up creator parrots before respawning
        creatorEffects.cleanupAllParrots();

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

        Economy economy = economyProvider.getProvider();
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
            return true;
        } catch (ClassNotFoundException e) {
            Logger.warning("Failed to hook into Oraxen: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the lootbox manager instance.
     *
     * @return The lootbox manager
     */
    public LootboxManager getLootboxManager() {
        return lootboxManager;
    }

    /**
     * Gets the user manager instance.
     *
     * @return The user manager
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Gets the config manager instance.
     *
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the creator effects instance.
     *
     * @return The creator effects
     */
    public CreatorEffects getCreatorEffects() {
        return creatorEffects;
    }

    /**
     * Gets the singleton instance of the plugin.
     *
     * @return The plugin instance
     */
    public static LuckyRabbit getInstance() {
        return instance;
    }
}
