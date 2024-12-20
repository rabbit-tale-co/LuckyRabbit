package co.RabbitTale.luckyRabbit.user;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.utils.Logger;

/*
 * UserManager.java
 *
 * Manages user data storage and manipulation for the LuckyRabbit plugin.
 * This class handles all user-related operations including key management
 * and data persistence.
 *
 * Features:
 * - Per-user configuration files in YAML format
 * - Automatic data loading and saving
 * - Lootbox key management system
 * - Memory-efficient data handling with unloading
 *
 * Data Structure:
 * - Each user has their own YAML file in playerdata/
 * - Files are named using player UUID
 * - Data is loaded on demand and cached in memory
 * - Automatic cleanup of unused data
 *
 * File Structure:
 * playerdata/
 *   ├── <uuid1>.yml
 *   ├── <uuid2>.yml
 *   └── ...
 *
 */
public class UserManager {

    private final LuckyRabbit plugin;
    private final Map<UUID, FileConfiguration> userConfigs;
    private final File userDirectory;

    /**
     * Initializes the UserManager and creates necessary directories.
     *
     * @param plugin The LuckyRabbit plugin instance
     */
    public UserManager(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.userConfigs = new HashMap<>();
        this.userDirectory = new File(plugin.getDataFolder(), "playerdata");

        if (!userDirectory.exists()) {
            if (!userDirectory.mkdirs()) {
                Logger.error("Failed to create playerdata directory!");
            } else {
                Logger.debug("Created playerdata directory successfully");
            }
        }

        loadAllPlayerData();
    }

    /**
     * Loads all player data files from disk. Called during initialization to
     * populate the cache.
     */
    private void loadAllPlayerData() {
        if (userDirectory.exists()) {
            File[] files = userDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String uuidStr = fileName.substring(0, fileName.length() - 4);
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        loadUserData(uuid);
                        Logger.debug("Loaded data for player: " + uuidStr);
                    } catch (IllegalArgumentException e) {
                        Logger.error("Invalid player data file name: " + fileName);
                    }
                }
            }
        }
    }

    /**
     * Gets the number of keys a player has for a specific lootbox.
     *
     * @param uuid Player UUID
     * @param lootboxId Lootbox identifier
     * @return Number of keys owned
     */
    public int getKeys(UUID uuid, String lootboxId) {
        FileConfiguration config = getUserConfig(uuid);

        if (!config.contains("keys")) {
            Logger.debug("No keys section found for user " + uuid);
            return 0;
        }

        int keys = config.getInt("keys." + lootboxId, 0);
        Logger.debug("Getting keys for " + uuid + " lootbox: " + lootboxId + " amount: " + keys);
        return keys;
    }

    /**
     * Sets the number of keys a player has for a specific lootbox.
     *
     * @param uuid Player UUID
     * @param lootboxId Lootbox identifier
     * @param amount New amount of keys
     */
    public void setKeys(UUID uuid, String lootboxId, int amount) {
        FileConfiguration config = getUserConfig(uuid);
        config.set("keys." + lootboxId, amount);
        saveUserConfig(uuid);
        Logger.debug("Set " + amount + " keys for " + uuid + " lootbox: " + lootboxId);
    }

    /**
     * Adds keys to a player's inventory. Validates lootbox existence before
     * adding.
     *
     * @param uuid Player UUID
     * @param lootboxId Lootbox identifier
     * @param amount Number of keys to add
     * @throws IllegalArgumentException if lootbox doesn't exist
     */
    public void addKeys(UUID uuid, String lootboxId, int amount) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + lootboxId + " does not exist!");
        }

        int current = getKeys(uuid, lootboxId);
        setKeys(uuid, lootboxId, current + amount);

        Logger.debug("Added " + amount + " keys for " + uuid + " lootbox: " + lootboxId + " new total: " + (current + amount));
    }

    /**
     * Removes keys from a player's inventory. Won't go below zero.
     *
     * @param uuid Player UUID
     * @param lootboxId Lootbox identifier
     * @param amount Number of keys to remove
     */
    public void removeKeys(UUID uuid, String lootboxId, int amount) {
        int current = getKeys(uuid, lootboxId);
        int newAmount = Math.max(0, current - amount);
        setKeys(uuid, lootboxId, newAmount);
        Logger.debug("Removed " + amount + " keys from " + uuid + " lootbox: " + lootboxId + " new total: " + newAmount);
    }

    /**
     * Checks if a player has at least one key for a lootbox.
     *
     * @param uuid Player UUID
     * @param lootboxId Lootbox identifier
     * @return true if player has keys, false otherwise
     */
    public boolean hasKey(UUID uuid, String lootboxId) {
        return getKeys(uuid, lootboxId) > 0;
    }

    /**
     * Uses one key from a player's inventory. Equivalent to removing one key.
     *
     * @param uuid Player UUID
     * @param lootboxId Lootbox identifier
     */
    public void useKey(UUID uuid, String lootboxId) {
        removeKeys(uuid, lootboxId, 1);
    }

    /**
     * Gets the current key count for a player. Alias for getKeys method.
     *
     * @param uuid Player UUID
     * @param lootboxId Lootbox identifier
     * @return Number of keys owned
     */
    public int getKeyCount(UUID uuid, String lootboxId) {
        return getKeys(uuid, lootboxId);
    }

    /**
     * Gets or loads a player's configuration. Creates new config if none
     * exists.
     *
     * @param uuid Player UUID
     * @return Player's configuration
     */
    private FileConfiguration getUserConfig(UUID uuid) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config == null) {
            loadUserData(uuid);
            config = userConfigs.get(uuid);
        }
        return config;
    }

    /**
     * Saves a player's configuration to disk.
     *
     * @param uuid Player UUID
     */
    private void saveUserConfig(UUID uuid) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config == null) {
            return;
        }

        try {
            File userFile = new File(userDirectory, uuid.toString() + ".yml");
            config.save(userFile);
            Logger.debug("Saved config for user: " + uuid + " to " + userFile.getAbsolutePath());
        } catch (IOException e) {
            Logger.error("Failed to save user config: " + uuid, e);
        }
    }

    /**
     * Saves all loaded user configurations to disk. Called during plugin
     * shutdown and periodic saves.
     */
    public void saveAllUsers() {
        Logger.debug("Saving all user configurations...");
        for (Map.Entry<UUID, FileConfiguration> entry : userConfigs.entrySet()) {
            try {
                UUID uuid = entry.getKey();
                FileConfiguration config = entry.getValue();
                File userFile = new File(userDirectory, uuid + ".yml");
                config.save(userFile);
                Logger.debug("Saved configuration for user: " + uuid);
            } catch (IOException e) {
                Logger.error("Failed to save user config", e);
            }
        }
        Logger.info("All user configurations saved successfully");
    }

    /**
     * Loads or creates user data for a player.
     *
     * @param uuid Player UUID
     */
    public void loadUserData(UUID uuid) {
        File userFile = new File(userDirectory, uuid + ".yml");
        if (userFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(userFile);
            userConfigs.put(uuid, config);
            Logger.debug("Loaded data for user: " + uuid);
        } else {
            FileConfiguration config = new YamlConfiguration();
            userConfigs.put(uuid, config);
            Logger.debug("Created new data for user: " + uuid);
            saveUserConfig(uuid);
        }
    }

    /**
     * Unloads user data from memory. Saves data before unloading.
     *
     * @param uuid Player UUID
     */
    public void unloadUserData(UUID uuid) {
        if (userConfigs.containsKey(uuid)) {
            saveUserConfig(uuid);
            userConfigs.remove(uuid);
            Logger.debug("Unloaded data for user: " + uuid);
        }
    }
}
