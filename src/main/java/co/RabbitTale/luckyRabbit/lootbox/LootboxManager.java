package co.RabbitTale.luckyRabbit.lootbox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.RabbitTale.luckyRabbit.api.FeatureManager;
import co.RabbitTale.luckyRabbit.api.LicenseManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbit.lootbox.entity.LootboxEntity;
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbit.lootbox.items.OraxenLootboxItem;
import co.RabbitTale.luckyRabbit.utils.Logger;
import io.th0rgal.oraxen.api.OraxenItems;
import org.jetbrains.annotations.NotNull;

public class LootboxManager {

    private final LuckyRabbit plugin;
    private final Map<String, Lootbox> lootboxes;
    private final Map<UUID, LootboxEntity> entities;

    public LootboxManager(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.lootboxes = new HashMap<>();
        this.entities = new HashMap<>();
    }

    public void loadLootboxes() {
        File lootboxFolder = new File(plugin.getDataFolder(), "lootboxes");
        Logger.info("Checking lootbox folder: " + lootboxFolder.getAbsolutePath());

        if (!lootboxFolder.exists()) {
            Logger.info("Lootbox folder doesn't exist, creating...");
            if (!lootboxFolder.mkdirs()) {
                Logger.error("Failed to create lootboxes directory!");
                return;
            }
        }

        // Save example files from resources if they don't exist
        String[] exampleFiles = {"example.yml", "example2.yml"};
        for (String fileName : exampleFiles) {
            File file = new File(lootboxFolder, fileName);
            if (!file.exists()) {
                try {
                    plugin.saveResource("lootboxes/" + fileName, false);
                    Logger.info("Created " + fileName + " from resources");
                } catch (IllegalArgumentException e) {
                    Logger.error("Resource not found: " + fileName);
                } catch (Exception e) {
                    Logger.error("Failed to create " + fileName + ": " + e.getMessage());
                }
            }
        }

        // Load all lootbox files
        File[] files = lootboxFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            Logger.error("Failed to list lootbox files!");
            return;
        }

        // Clear existing lootboxes
        lootboxes.clear();

        // Load each file
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id");

                // Create lootbox with the formatted display name
                Lootbox lootbox = Lootbox.fromConfig(config);
                lootboxes.put(id, lootbox);
            } catch (Exception e) {
                Logger.error("Failed to load lootbox from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        Logger.info("Total lootboxes loaded: " + lootboxes.size());
        Logger.info("Lootbox IDs: " + String.join(", ", lootboxes.keySet()));
    }

    public void createLootbox(String name, AnimationType animationType) {
        // Count existing custom lootboxes (excluding examples)
        long existingCustomLootboxes = lootboxes.values().stream()
                .filter(lb -> !isExampleLootbox(lb.getId()))
                .count();

        // Get the maximum allowed lootboxes
        int maxLootboxes = FeatureManager.getMaxLootboxes();

        // Check if we've reached the limit
        if (maxLootboxes != -1 && existingCustomLootboxes >= maxLootboxes) {
            String planType = LicenseManager.isPremium() ? "Premium"
                    : LicenseManager.isTrialActive() ? "Trial" : "Free";

            throw new IllegalStateException(
                    String.format("""
                                  Cannot create more lootboxes! You have reached the limit (%d/%d) for your %s plan.
                                  Upgrade your plan to create more lootboxes!""",
                            existingCustomLootboxes, maxLootboxes, planType)
            );
        }

        // Create safe file name by removing all formatting tags and special characters
        String id = getId(name);

        // Create new lootbox with the original formatted name
        Lootbox lootbox = new Lootbox(id, name, animationType);
        lootboxes.put(id, lootbox);

        // Save the new lootbox
        saveLootbox(lootbox);
        Logger.info("Created new lootbox: " + id + " with display name: " + name);
    }

    private @NotNull
    String getId(String name) {
        String cleanName = name.replaceAll("<[^>]*>", "") // Remove all tags like <gradient:blue:purple>
                .replaceAll("\\s+", "_") // Replace spaces with underscores
                .replaceAll("[^a-zA-Z0-9_-]", "") // Remove any other special characters
                .toLowerCase();  // Convert to lowercase

        // Check if lootbox already exists
        if (lootboxes.containsKey(cleanName)) {
            throw new IllegalArgumentException("Lootbox with ID " + cleanName + " already exists!");
        }
        return cleanName;
    }

    private void spawnEntity(String id, Location location) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) {
            return;
        }

        LootboxEntity entity = new LootboxEntity(plugin, location, lootbox);
        entities.put(entity.getUniqueId(), entity);
    }

    public void deleteLootbox(String id) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + id + " does not exist!");
        }

        // Remove all entities
        for (Location location : lootbox.getLocations()) {
            for (Entity entity : location.getWorld().getEntities()) {
                if (entity instanceof ArmorStand && entity.hasMetadata("LootboxEntity")) {
                    String lootboxId = entity.getMetadata("LootboxEntity").get(0).asString();
                    if (lootboxId.equals(id)) {
                        entity.remove();
                    }
                }
            }
        }

        // Remove from maps
        lootboxes.remove(id);

        // Delete file
        File file = new File(plugin.getDataFolder(), "lootboxes/" + id + ".yml");
        if (file.exists() && !file.delete()) {
            Logger.error("Failed to delete lootbox file: " + id);
        }
    }

    public void addItem(Player player, String id) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + id + " does not exist!");
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            throw new IllegalArgumentException("You must hold an item to add!");
        }

        // Check if it's an Oraxen item
        String oraxenId = OraxenItems.getIdByItem(item);
        LootboxItem lootboxItem;

        if (oraxenId != null) {
            lootboxItem = new OraxenLootboxItem(item, oraxenId);
        } else {
            lootboxItem = new LootboxItem(item);
        }

        lootbox.addItem(lootboxItem);
        saveLootbox(lootbox);
    }

    public void removeItem(Player player, String id) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + id + " does not exist!");
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            throw new IllegalArgumentException("You must hold an item to remove!");
        }

        lootbox.removeItem(item);
        saveLootbox(lootbox);
    }

    public void placeLootbox(Player player, String id) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + id + " does not exist!");
        }

        Location location = player.getLocation().clone();
        location.setY(location.getY() - 1); // Place at feet level

        // Center the coordinates
        location.setX(Math.floor(location.getX()) + 0.5);
        location.setZ(Math.floor(location.getZ()) + 0.5);

        // Ensure chunk is loaded
        Chunk chunk = location.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }
        chunk.setForceLoaded(true);

        // Create entity
        LootboxEntity entity = new LootboxEntity(plugin, location, lootbox);
        entities.put(entity.getUniqueId(), entity);

        // Save location
        lootbox.addLocation(location);
        saveLootbox(lootbox);
    }

    public void saveLootbox(Lootbox lootbox) {
        // Don't save example lootboxes unless they've been modified
        if ((lootbox.getId().equals("example") || lootbox.getId().equals("example2"))
                && !lootbox.hasBeenModified()) {
            return;
        }

        File file = new File(plugin.getDataFolder(), "lootboxes/" + lootbox.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Save basic lootbox info
        config.set("id", lootbox.getId());
        config.set("displayName", lootbox.getDisplayName());  // Save original formatted name
        config.set("lore", lootbox.getLore());
        config.set("animationType", lootbox.getAnimationType().name());
        config.set("openedCount", lootbox.getOpenCount());

        // Save items
        ConfigurationSection itemsSection = config.createSection("items");
        int index = 0;
        for (LootboxItem item : lootbox.getItems().values()) {
            ConfigurationSection itemSection = itemsSection.createSection(String.valueOf(index++));
            item.save(itemSection);
        }

        // Save locations
        ConfigurationSection locationsSection = config.createSection("locations");
        int locIndex = 0;
        for (Location location : lootbox.getLocations()) {
            locIndex = getIndex(locIndex, locationsSection, location);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Logger.error("Failed to save lootbox: " + lootbox.getId(), e);
        }
    }

    static int getIndex(int index, ConfigurationSection locationsSection, Location location) {
        ConfigurationSection locationSection = locationsSection.createSection(String.valueOf(index++));
        locationSection.set("world", location.getWorld().getName());
        locationSection.set("x", location.getX());
        locationSection.set("y", location.getY());
        locationSection.set("z", location.getZ());
        locationSection.set("yaw", location.getYaw());
        locationSection.set("pitch", location.getPitch());
        return index;
    }

    public void saveAll() {
        // Save only modified lootboxes, skip examples if they haven't been modified
        for (Lootbox lootbox : lootboxes.values()) {
            if (!isExampleLootbox(lootbox.getId()) || lootbox.hasBeenModified()) {
                saveLootbox(lootbox);
            }
        }
    }

    private boolean isExampleLootbox(String id) {
        return id.equals("example") || id.equals("example2");
    }

    public void removeAllEntities() {
        // Remove all lootbox entities
        for (LootboxEntity entity : entities.values()) {
            entity.remove();
        }
        entities.clear();

        // Unforce-load chunks
        for (Lootbox lootbox : lootboxes.values()) {
            for (Location location : lootbox.getLocations()) {
                Chunk chunk = location.getChunk();
                if (chunk.isForceLoaded()) {
                    chunk.setForceLoaded(false);
                }
            }
        }
    }

    public Lootbox getLootbox(String id) {
        return lootboxes.get(id);
    }

    public List<String> getLootboxNames() {
        return new ArrayList<>(lootboxes.keySet());
    }

    public Collection<Lootbox> getAllLootboxes() {
        return Collections.unmodifiableCollection(lootboxes.values());
    }

    public void loadLimitedLootboxes() {
        // First check trial status
        boolean isTrial = LicenseManager.isTrialActive();
        int maxLootboxes = FeatureManager.getMaxLootboxes();

        // Ensure lootbox folder exists
        File lootboxFolder = new File(plugin.getDataFolder(), "lootboxes");
        if (!lootboxFolder.exists()) {
            if (!lootboxFolder.mkdirs()) {
                Logger.error("Failed to create lootboxes directory!");
                return;
            }
        }

        // Save example file if it doesn't exist
        File exampleFile = new File(lootboxFolder, "example.yml");
        if (!exampleFile.exists()) {
            plugin.saveResource("lootboxes/example.yml", false);
            Logger.info("Created example.yml from resources");
        }

        // Get all yml files
        File[] files = lootboxFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            Logger.error("Failed to list lootbox files!");
            return;
        }

        // Clear existing lootboxes
        lootboxes.clear();

        // First, load example lootboxes
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id", "");

                if (id.isEmpty()) {
                    // If no ID in config, use filename without extension
                    id = file.getName().replace(".yml", "");
                }

                if (isExampleLootbox(id)) {
                    Lootbox lootbox = Lootbox.fromConfig(config);
                    lootboxes.put(id, lootbox);
                    Logger.info("Loaded example lootbox: " + id);
                }
            } catch (Exception e) {
                Logger.error("Failed to load lootbox from " + file.getName() + ": " + e.getMessage());
            }
        }

        // Sort remaining files by last modified date (newest first)
        List<File> customFiles = Arrays.stream(files)
            .filter(file -> {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String id = config.getString("id");
                    assert id != null;
                    return !isExampleLootbox(id);
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
            .toList();

        // Load custom lootboxes up to the limit
        int loadedCustom = 0;
        for (File file : customFiles) {
            if (loadedCustom >= maxLootboxes) {
                Logger.warning("Skipping remaining lootboxes due to " +
                    (isTrial ? "trial" : "free version") + " limitations");
                break;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id");
                Lootbox lootbox = Lootbox.fromConfig(config);
                lootboxes.put(id, lootbox);
                loadedCustom++;
            } catch (Exception e) {
                Logger.error("Failed to load lootbox from " + file.getName() + ": " + e.getMessage());
            }
        }

        // Apply limitations to loaded custom lootboxes
        for (Lootbox lootbox : lootboxes.values()) {
            if (!isExampleLootbox(lootbox.getId()) && !plugin.getFeatureManager().canUseCustomAnimations()) {
                if (lootbox.getAnimationType() != AnimationType.HORIZONTAL) {
                    Logger.warning("Custom animations are not available in free version. Using default animation for " + lootbox.getId());
                    lootbox.setAnimationType(AnimationType.HORIZONTAL);
                }
            }
        }

        int totalLoaded = lootboxes.size();
        int exampleCount = (int) lootboxes.values().stream()
            .filter(lb -> isExampleLootbox(lb.getId()))
            .count();

        Logger.info(String.format("Loaded %d lootboxes (%d custom, %d example) in %s mode",
            totalLoaded, loadedCustom, exampleCount, isTrial ? "trial" : "free"));

        if (loadedCustom >= maxLootboxes) {
            Logger.warning(String.format("Reached %s mode limit of %d custom lootboxes",
                isTrial ? "trial" : "free", maxLootboxes));
        }
    }
}
