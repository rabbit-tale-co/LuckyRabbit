package co.RabbitTale.luckyRabbit.config;

import java.io.File;

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

        // Create lootboxes directory structure
        createLootboxDirectories();

        // Create animations directory
        createAnimationsDirectory();

        // Load main config
        plugin.reloadConfig();

        // Load lootbox config
        new LootboxConfig(plugin);

        Logger.debug("All configurations loaded successfully");
    }

    /**
     * Creates the directory structure for lootboxes and example lootboxes. Main
     * directory: plugins/LuckyRabbit/lootboxes/ Examples directory:
     * plugins/LuckyRabbit/lootboxes/examples/
     */
    private void createLootboxDirectories() {
        // Create main lootboxes directory
        File lootboxDir = new File(plugin.getDataFolder(), "lootboxes");
        if (!lootboxDir.exists()) {
            if (lootboxDir.mkdirs()) {
                Logger.debug("Created lootboxes directory");
            } else {
                Logger.error("Failed to create lootboxes directory!");
            }
        }

        // Create examples subdirectory
        File examplesDir = new File(lootboxDir, "examples");
        if (!examplesDir.exists()) {
            if (examplesDir.mkdirs()) {
                Logger.debug("Created examples directory");
            } else {
                Logger.error("Failed to create examples directory!");
            }
        }
    }

    /**
     * Creates the animations directory and saves default animation files.
     * Directory: plugins/LuckyRabbit/animations/
     */
    private void createAnimationsDirectory() {
        // Create animations directory
        File animationsDir = new File(plugin.getDataFolder(), "animations");
        if (!animationsDir.exists()) {
            if (animationsDir.mkdirs()) {
                Logger.debug("Created animations directory");

                // Save default horizontal animation file if it doesn't exist
                File horizontalFile = new File(animationsDir, "horizontal.yml");
                if (!horizontalFile.exists()) {
                    try {
                        plugin.saveResource("animations/horizontal.yml", false);
                        Logger.debug("Created default horizontal.yml animation file");
                    } catch (Exception e) {
                        Logger.error("Failed to create default horizontal.yml animation file", e);
                    }
                }

                // We don't auto-create other animation files (circle.yml, three_in_row.yml, etc.)
                // They can be downloaded or purchased separately
                Logger.info("Only default animation (HORIZONTAL) is available by default");
                Logger.info("Additional animations can be added manually to the animations folder");
            } else {
                Logger.error("Failed to create animations directory!");
            }
        }
    }
}
