package co.RabbitTale.luckyRabbitFoot;

import co.RabbitTale.luckyRabbitFoot.api.LootboxAPI;
import co.RabbitTale.luckyRabbitFoot.commands.CommandManager;
import co.RabbitTale.luckyRabbitFoot.config.ConfigManager;
import co.RabbitTale.luckyRabbitFoot.listeners.ListenerManager;
import co.RabbitTale.luckyRabbitFoot.lootbox.LootboxManager;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class LuckyRabbitFoot extends JavaPlugin {

    @Getter private static LuckyRabbitFoot instance;
    @Getter private ConfigManager configManager;
    @Getter private LootboxManager lootboxManager;
    @Getter private CommandManager commandManager;
    @Getter private ListenerManager listenerManager;
    @Getter private LootboxAPI api;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.lootboxManager = new LootboxManager(this);
        this.commandManager = new CommandManager(this);
        this.listenerManager = new ListenerManager(this);
        this.api = new LootboxAPI(this);

        // Load configurations
        configManager.loadConfigs();

        // Register commands and listeners
        commandManager.registerCommands();
        listenerManager.registerListeners();

        // Load lootboxes and spawn entities
        lootboxManager.loadLootboxes();

        // Verify license
        verifyLicense();

        Logger.info("Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save all data and remove entities
        if (lootboxManager != null) {
            lootboxManager.saveAll();
            lootboxManager.removeAllEntities();
        }

        Logger.info("Plugin disabled successfully!");
    }

    private void verifyLicense() {
        String authCode = getConfig().getString("license.auth-code");
        if (authCode == null || authCode.isEmpty()) {
            Logger.warning("No license key found! Running in trial mode...");
        }

        // Verify license with API
        // TODO: Implement license verification
    }
}
