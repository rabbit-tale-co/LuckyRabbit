package co.RabbitTale.luckyRabbit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

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
    private Economy economy = null;

    @Getter
    private boolean oraxenHooked = false;

    // TODO: make more lootbox animations (PARTICLES) -> lootbox/entity/lootboxEntity
    // TODO: add more lootbox animations (SPIN)
    // TODO: add option to change lang (even by API calls from other plugins)
    // TODO: make more advanced chance % system (auto recalculate on adding new items to lootbox)
    // TODO: web editor (manage lootboxes and items via web dashboard) will auto calculate % of items, will show list of lootboxes with items inside ect.
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

        // Check license and load lootboxes after all plugins are loaded
        getServer().getScheduler().runTaskLater(this, () -> {
            // Setup Vault
            if (setupEconomy()) {
                hookedPlugins.add("Vault Economy");
            }

            // Setup Oraxen with longer delay
            if (setupOraxen()) {
                hookedPlugins.add("Oraxen");
            }

            // Then check license
            String licenseKey = getConfig().getString("license-key", "");
            if (!licenseKey.isEmpty()) {
                licenseManager.verifyLicense(licenseKey);
            } else {
                LicenseManager.checkTrialStatus();
            }

            // Get plan type
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

        }, 100L); // Increased delay to 5 seconds (100 ticks)
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

    /**
     * Sets up the economy integration with Vault
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            Logger.warning("Vault plugin not found - economy features will be disabled");
            return false;
        }

        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                Logger.warning("No economy plugin (like Essentials) found - economy features will be disabled");
                return false;
            }
            this.economy = rsp.getProvider();
            Logger.success("Successfully hooked into Vault economy!");
            return true;
        } catch (NoClassDefFoundError e) {
            Logger.warning("Vault API not found - economy features will be disabled");
            return false;
        }
    }

    /**
     * Sets up Oraxen integration
     */
    private boolean setupOraxen() {
        if (getServer().getPluginManager().getPlugin("Oraxen") == null) {
            Logger.warning("Oraxen plugin not found - custom items will use fallback items");
            return false;
        }

        try {
            Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            this.oraxenHooked = true;
            Logger.success("Successfully hooked into Oraxen!");
            return true;
        } catch (ClassNotFoundException e) {
            Logger.error("Failed to hook into Oraxen - custom items will use fallback items");
            return false;
        }
    }
}
