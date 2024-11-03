package co.RabbitTale.luckyRabbitFoot.config;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final LuckyRabbitFoot plugin;

    public ConfigManager(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
        new LootboxConfig(plugin);
        loadMainConfig();
    }

    public void loadConfigs() {
        loadMainConfig();
        // Load other configs here
    }

    private void loadMainConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.getConfig();
    }
}
