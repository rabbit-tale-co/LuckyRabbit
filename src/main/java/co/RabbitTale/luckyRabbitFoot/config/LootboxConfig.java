package co.RabbitTale.luckyRabbitFoot.config;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LootboxConfig {

    private final LuckyRabbitFoot plugin;
    private final Map<String, FileConfiguration> lootboxConfigs;
    private final File lootboxDirectory;

    public LootboxConfig(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
        this.lootboxConfigs = new HashMap<>();
        this.lootboxDirectory = new File(plugin.getDataFolder(), "lootboxes");

        if (!lootboxDirectory.exists()) {
            if (!lootboxDirectory.mkdirs()) {
                Logger.error("Failed to create lootboxes directory!");
            }
        }
    }

    public void loadLootbox(String id) {
        File file = new File(lootboxDirectory, id + ".yml");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    Logger.error("Failed to create lootbox config file: " + id);
                    return;
                }
            } catch (IOException e) {
                Logger.error("Failed to create lootbox config file: " + id, e);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        lootboxConfigs.put(id, config);
    }

    public void saveLootbox(String id) {
        FileConfiguration config = lootboxConfigs.get(id);
        if (config == null) return;

        try {
            File file = new File(lootboxDirectory, id + ".yml");
            config.save(file);
            Logger.info("Saved lootbox config: " + id);
        } catch (IOException e) {
            Logger.error("Failed to save lootbox config: " + id, e);
        }
    }

    public FileConfiguration getLootboxConfig(String id) {
        return lootboxConfigs.get(id);
    }

    public void ensureDirectories() {
        // Create main plugin directory
        File pluginDir = plugin.getDataFolder();
        if (!pluginDir.exists() && !pluginDir.mkdirs()) {
            Logger.error("Failed to create plugin directory!");
            return;
        }

        // Create lootboxes directory
        if (!lootboxDirectory.exists() && !lootboxDirectory.mkdirs()) {
            Logger.error("Failed to create lootboxes directory!");
            return;
        }

        // Create playerdata directory
        File playerDataDir = new File(pluginDir, "playerdata");
        if (!playerDataDir.exists() && !playerDataDir.mkdirs()) {
            Logger.error("Failed to create playerdata directory!");
        }
    }

    public void deleteLootbox(String id) {
        File file = new File(lootboxDirectory, id + ".yml");
        if (file.exists() && !file.delete()) {
            Logger.error("Failed to delete lootbox file: " + id);
        }
        lootboxConfigs.remove(id);
    }

    public boolean exists(String id) {
        return new File(lootboxDirectory, id + ".yml").exists();
    }

    public void reload() {
        lootboxConfigs.clear();
        File[] files = lootboxDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().replace(".yml", "");
                loadLootbox(id);
            }
        }
    }
}
