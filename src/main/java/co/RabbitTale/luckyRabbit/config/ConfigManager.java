package co.RabbitTale.luckyRabbit.config;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

public class ConfigManager {

    private final LuckyRabbit plugin;

    public ConfigManager(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Create plugin folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            if (!plugin.getDataFolder().mkdirs()) {
                Logger.error("Failed to create plugin directory!");
                return;
            }
        }

        // Load main config
        plugin.reloadConfig();

        // Load lootbox config
        new LootboxConfig(plugin);

        Logger.debug("All configurations loaded successfully");
    }
}
