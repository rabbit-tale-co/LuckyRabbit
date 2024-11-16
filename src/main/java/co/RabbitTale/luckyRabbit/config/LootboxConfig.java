package co.RabbitTale.luckyRabbit.config;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LootboxConfig {

    private final LuckyRabbit plugin;
    private FileConfiguration config;

    /**
     * Creates a new lootbox configuration.
     *
     * @param plugin Plugin instance
     */
    public LootboxConfig(LuckyRabbit plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Loads the configuration from disk.
     * Creates default if not exists.
     */
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        validateConfig();
    }

    /**
     * Validates configuration values.
     * Sets defaults if missing.
     */
    private void validateConfig() {
        // Validate settings section
        if (!config.contains("settings")) {
            config.createSection("settings");
        }

        // Set default values if not present
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            if (!settings.contains("debug")) {
                settings.set("debug", false);
            }
            if (!settings.contains("max-lootboxes")) {
                settings.set("max-lootboxes", 5);
            }
            if (!settings.contains("default-animation")) {
                settings.set("default-animation", "HORIZONTAL");
            }
        }

        // Validate animations section
        if (!config.contains("animations")) {
            config.createSection("animations");
            ConfigurationSection animations = config.getConfigurationSection("animations");
            if (animations != null) {
                animations.set("enabled", Arrays.asList("HORIZONTAL", "CIRCLE"));
                animations.set("premium", Arrays.asList("CIRCLE", "PIN_POINT", "CASCADE", "THREE_IN_ROW"));
            }
        }

        saveConfig();
    }

    /**
     * Saves the configuration to disk.
     */
    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            Logger.error("Failed to save config.yml", e);
        }
    }

}
