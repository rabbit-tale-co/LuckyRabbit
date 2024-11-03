package co.RabbitTale.luckyRabbitFoot.user;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {
    private final LuckyRabbitFoot plugin;
    private final Map<UUID, FileConfiguration> userConfigs;
    private final File userDirectory;

    public UserManager(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
        this.userConfigs = new HashMap<>();
        this.userDirectory = new File(plugin.getDataFolder(), "users");

        if (!userDirectory.exists()) {
            userDirectory.mkdirs();
        }
    }

    public int getKeys(UUID uuid, String lootboxId) {
        FileConfiguration config = getUserConfig(uuid);
        return config.getInt("keys." + lootboxId, 0);
    }

    public void setKeys(UUID uuid, String lootboxId, int amount) {
        FileConfiguration config = getUserConfig(uuid);
        config.set("keys." + lootboxId, amount);
        saveUserConfig(uuid);
    }

    public void addKeys(UUID uuid, String lootboxId, int amount) {
        int current = getKeys(uuid, lootboxId);
        setKeys(uuid, lootboxId, current + amount);
    }

    private FileConfiguration getUserConfig(UUID uuid) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config == null) {
            File file = new File(userDirectory, uuid + ".yml");
            config = YamlConfiguration.loadConfiguration(file);
            userConfigs.put(uuid, config);
        }
        return config;
    }

    private void saveUserConfig(UUID uuid) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config == null) return;

        try {
            config.save(new File(userDirectory, uuid + ".yml"));
        } catch (IOException e) {
            Logger.error("Failed to save user config: " + uuid, e);
        }
    }
}
