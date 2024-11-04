package co.RabbitTale.luckyRabbit.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

public class ConfigManager {
    private final LuckyRabbit plugin;
    private final String[] EXAMPLE_FILES = {
        "example.yml", "example2.yml"
    };

    public ConfigManager(LuckyRabbit plugin) {
        this.plugin = plugin;
        new LootboxConfig(plugin);
        loadMainConfig();
    }

    public void loadConfigs() {
        loadMainConfig();
        setupLootboxFolder();
    }

    private void loadMainConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.getConfig();
    }

    private void setupLootboxFolder() {
        File lootboxFolder = new File(plugin.getDataFolder(), "lootboxes");
        if (!lootboxFolder.exists()) {
            if (!lootboxFolder.mkdirs()) {
                Logger.error("Failed to create lootboxes directory!");
                return;
            }
            Logger.info("Created lootboxes directory");
        }

        // Try to save all example files
        for (String fileName : EXAMPLE_FILES) {
            File exampleFile = new File(lootboxFolder, fileName);
            if (!exampleFile.exists()) {
                try {
                    // Try to get the resource from the jar
                    InputStream in = plugin.getResource("lootboxes/" + fileName);
                    if (in == null) {
                        Logger.error("Could not find " + fileName + " in plugin resources!");
                        continue;
                    }

                    // Copy the file
                    Files.copy(in, exampleFile.toPath());
                    in.close();
                    Logger.info("Successfully created " + fileName);
                } catch (IOException e) {
                    Logger.error("Failed to create " + fileName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
