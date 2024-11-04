package co.RabbitTale.luckyRabbit.user;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class UserManager {
    private final LuckyRabbit plugin;
    private final Map<UUID, FileConfiguration> userConfigs;
    private final File userDirectory;

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

    public void setKeys(UUID uuid, String lootboxId, int amount) {
        FileConfiguration config = getUserConfig(uuid);
        config.set("keys." + lootboxId, amount);
        saveUserConfig(uuid);
        Logger.debug("Set " + amount + " keys for " + uuid + " lootbox: " + lootboxId);
    }

    public void addKeys(UUID uuid, String lootboxId, int amount) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + lootboxId + " does not exist!");
        }

        int current = getKeys(uuid, lootboxId);
        setKeys(uuid, lootboxId, current + amount);

        Player target = Bukkit.getPlayer(uuid);
        if (target != null) {
            Component message = Component.text("You received ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(amount + " key(s)")
                    .color(NamedTextColor.GOLD))
                .append(Component.text(" for lootbox ")
                    .color(NamedTextColor.GRAY))
                .append(Component.text(lootbox.getDisplayName()));
            target.sendMessage(message);
        }

        Logger.debug("Added " + amount + " keys for " + uuid + " lootbox: " + lootboxId + " new total: " + (current + amount));
    }

    public void removeKeys(UUID uuid, String lootboxId, int amount) {
        int current = getKeys(uuid, lootboxId);
        int newAmount = Math.max(0, current - amount);
        setKeys(uuid, lootboxId, newAmount);
        Logger.debug("Removed " + amount + " keys from " + uuid + " lootbox: " + lootboxId + " new total: " + newAmount);
    }

    public boolean hasKey(UUID uuid, String lootboxId) {
        return getKeys(uuid, lootboxId) > 0;
    }

    public void useKey(UUID uuid, String lootboxId) { removeKeys(uuid, lootboxId, 1); }

    public int getKeyCount(UUID uuid, String lootboxId) {
        return getKeys(uuid, lootboxId);
    }

    private FileConfiguration getUserConfig(UUID uuid) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config == null) {
            loadUserData(uuid);
            config = userConfigs.get(uuid);
        }
        return config;
    }

    private void saveUserConfig(UUID uuid) {
        FileConfiguration config = userConfigs.get(uuid);
        if (config == null) return;

        try {
            File userFile = new File(userDirectory, uuid.toString() + ".yml");
            config.save(userFile);
            Logger.debug("Saved config for user: " + uuid + " to " + userFile.getAbsolutePath());
        } catch (IOException e) {
            Logger.error("Failed to save user config: " + uuid, e);
        }
    }

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

    public void unloadUserData(UUID uuid) {
        if (userConfigs.containsKey(uuid)) {
            saveUserConfig(uuid);
            userConfigs.remove(uuid);
            Logger.debug("Unloaded data for user: " + uuid);
        }
    }
}
