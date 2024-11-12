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

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.api.FeatureManager;
import co.RabbitTale.luckyRabbit.api.LicenseManager;
import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbit.lootbox.entity.LootboxEntity;
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbit.lootbox.items.MinecraftLootboxItem;
import co.RabbitTale.luckyRabbit.lootbox.items.OraxenLootboxItem;
import co.RabbitTale.luckyRabbit.utils.Logger;
import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LootboxManager {

    private final LuckyRabbit plugin;
    private final Map<String, Lootbox> lootboxes;
    private final Map<UUID, LootboxEntity> entities;
    private int respawnTaskId = -1;

    public LootboxManager(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.lootboxes = new HashMap<>();
        this.entities = new HashMap<>();
    }

    public void loadLootboxes() {
        File lootboxFolder = new File(plugin.getDataFolder(), "lootboxes");

        if (!lootboxFolder.exists()) {
            Logger.debug("Lootbox folder doesn't exist, creating...");
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
                    Logger.debug("Created " + fileName + " from resources");
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

                // Enforce restrictions for non-example lootboxes
                if (!isExampleLootbox(lootbox.getId())) {
                    lootbox.enforceAnimationRestrictions();
                    lootbox.enforceItemRestrictions();

                    // Save if modifications were made
                    if (lootbox.hasBeenModified()) {
                        saveLootbox(lootbox);
                    }
                }

                lootboxes.put(id, lootbox);
            } catch (Exception e) {
                Logger.error("Failed to load lootbox from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        Logger.debug("Total lootboxes loaded: " + lootboxes.size());
        Logger.debug("Lootbox IDs: " + String.join(", ", lootboxes.keySet()));
    }

    public void createLootbox(String name, AnimationType animationType) {
        // If free version, force HORIZONTAL animation
        if (FeatureManager.canUseAnimation(animationType.name())) {
            animationType = AnimationType.HORIZONTAL;
            // Notify the admin
            Logger.warning("Free version only supports HORIZONTAL animation. Animation type has been changed.");
        }

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

        // Set default lore
        List<String> defaultLore = new ArrayList<>();
        defaultLore.add("<gray>A mysterious lootbox");
        defaultLore.add("<gray>Contains various rewards");
        defaultLore.add("");
        defaultLore.add("<yellow>Right-click to preview");
        defaultLore.add("<yellow>Use a key to open");
        lootbox.setLore(defaultLore);

        // Add to loaded lootboxes
        lootboxes.put(id, lootbox);

        // Save the lootbox (this will create the file with all sections)
        saveLootbox(lootbox);

        Logger.success("Created new lootbox: " + id + " with display name: " + name);
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

    public void deleteLootbox(String id) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + id + " does not exist!");
        }

        // Store the display name before deletion
        Component displayName = MiniMessage.miniMessage().deserialize(lootbox.getDisplayName());

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

        Component message = Component.text("Lootbox ")
                .color(LootboxCommand.SUCCESS_COLOR)
                .append(displayName)
                .append(Component.text(" has been deleted")
                        .color(LootboxCommand.SUCCESS_COLOR));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("luckyrabbit.admin")) {
                player.sendMessage(message);
            }
        }
    }

    // TODO: add also parametr for ammount can be random using min-max or just max (maybe before rarity?)
    public void addItem(Player player, @NotNull String lootboxId, @NotNull ItemStack item, String rarity, Double chance) {
        // Walidacja parametrów
        if (item.getType() == Material.AIR) {
            throw new IllegalArgumentException("Item cannot be null or AIR");
        }

        Lootbox lootbox = getLootbox(lootboxId);
        if (lootbox == null) {
            if (player != null) {
                player.sendMessage(Component.text("Lootbox not found!")
                        .color(LootboxCommand.ERROR_COLOR));
            }
            return;
        }

        // Sprawdź, czy to przedmiot Oraxen
        String oraxenId = OraxenItems.getIdByItem(item);
        if (oraxenId != null && !FeatureManager.canUseOraxenItems()) {
            if (player != null) {
                player.sendMessage(Component.text("Oraxen items are only available in premium version!")
                        .color(LootboxCommand.ERROR_COLOR));
            }
            return;
        }

        Map<String, LootboxItem> existingItems = lootbox.getItems();
        String finalRarity = rarity != null ? rarity.toUpperCase() : "COMMON";

        double finalChance;
        if (chance == null) {
            // Calculate equal distribution for all items (including new one)
            int totalItems = existingItems.size() + 1;
            finalChance = 100.0 / totalItems;

            // Create a new map for updated items
            Map<String, LootboxItem> updatedItems = new HashMap<>();

            // First, create the new item
            String newItemId = "item-" + existingItems.size();
            LootboxItem newItem = createLootboxItem(item, oraxenId, newItemId, finalChance, finalRarity);
            updatedItems.put(newItemId, newItem);

            // Then update all existing items with equal chance
            for (LootboxItem existingItem : existingItems.values()) {
                LootboxItem updatedItem = createUpdatedItem(existingItem, finalChance);
                updatedItems.put(existingItem.getId(), updatedItem);
            }

            // Clear and update the lootbox items
            existingItems.clear();
            existingItems.putAll(updatedItems);
        } else {
            finalChance = chance;
            double remainingChance = 100.0 - chance;
            double totalExistingChance = existingItems.values().stream()
                    .mapToDouble(LootboxItem::getChance)
                    .sum();

            if (totalExistingChance > 0) {
                // Create a new map for updated items
                Map<String, LootboxItem> updatedItems = new HashMap<>();

                // Adjust existing items proportionally
                for (Map.Entry<String, LootboxItem> entry : existingItems.entrySet()) {
                    double newChance = (entry.getValue().getChance() / totalExistingChance) * remainingChance;
                    LootboxItem updatedItem = createUpdatedItem(entry.getValue(), newChance);
                    updatedItems.put(entry.getKey(), updatedItem);
                }

                // Create new item with specified chance
                String newItemId = "item-" + existingItems.size();
                LootboxItem newItem = createLootboxItem(item, oraxenId, newItemId, finalChance, finalRarity);
                updatedItems.put(newItemId, newItem);

                // Clear and update the lootbox items
                existingItems.clear();
                existingItems.putAll(updatedItems);
            } else {
                // If no existing items, just add the new one with specified chance
                String newItemId = "item-" + existingItems.size();
                LootboxItem newItem = createLootboxItem(item, oraxenId, newItemId, finalChance, finalRarity);
                existingItems.put(newItemId, newItem);
            }
        }

        // Save lootbox
        saveLootbox(lootbox);

        // Show success message
        if (player != null) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Successfully added item to lootbox!")
                    .color(LootboxCommand.SUCCESS_COLOR));
            player.sendMessage(Component.text("Chance distribution updated:")
                    .color(LootboxCommand.INFO_COLOR));

            // Show all items with their chances
            existingItems.values().forEach(existingItem ->
                player.sendMessage(Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                    .append(Component.text(existingItem.getId() + ": ", LootboxCommand.DESCRIPTION_COLOR))
                    .append(Component.text(String.format("%.1f%%", existingItem.getChance()))
                        .color(LootboxCommand.TARGET_COLOR))));
            player.sendMessage(Component.empty());
        }
    }

    private LootboxItem createLootboxItem(ItemStack item, String oraxenId, String itemId, double chance, String rarity) {
        if (oraxenId != null) {
            return new OraxenLootboxItem(item, oraxenId, itemId, chance, rarity, null);
        } else {
            return new MinecraftLootboxItem(item, itemId, chance, rarity, null, null);
        }
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

        Component message = Component.text("Removed item from ")
                .color(LootboxCommand.SUCCESS_COLOR)
                .append(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))
                .append(Component.text(" (")
                        .color(LootboxCommand.DESCRIPTION_COLOR))
                .append(Component.text(item.getType().toString())
                        .color(LootboxCommand.ITEM_COLOR))
                .append(Component.text(")")
                        .color(LootboxCommand.DESCRIPTION_COLOR));
        player.sendMessage(message);
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

        // Special message for example lootboxes
        if (isExampleLootbox(lootbox.getId())) {
            player.sendMessage(Component.text("Note: This is an example lootbox - only admins can open it!")
                    .color(LootboxCommand.INFO_COLOR));
        }

        Component message = Component.text("Successfully placed ")
                .color(LootboxCommand.SUCCESS_COLOR)
                .append(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))
                .append(Component.text(" at your location")
                        .color(LootboxCommand.SUCCESS_COLOR));
        player.sendMessage(message);
    }

    public void saveLootbox(Lootbox lootbox) {
        // Don't save example lootboxes unless they've been modified
        if ((lootbox.getId().equals("example") || lootbox.getId().equals("example2"))
                && !lootbox.hasBeenModified()) {
            return;
        }

        File file = new File(plugin.getDataFolder(), "lootboxes/" + lootbox.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Basic information
        config.set("id", lootbox.getId());
        config.set("displayName", lootbox.getDisplayName());
        config.set("animationType", lootbox.getAnimationType().name());

        // Default empty lore if not set
        List<String> defaultLore = new ArrayList<>();
        defaultLore.add("<gray>A mysterious lootbox");
        defaultLore.add("<gray>Contains various rewards");
        defaultLore.add("");
        defaultLore.add("<yellow>Right-click to preview");
        defaultLore.add("<yellow>Use a key to open");
        config.set("lore", lootbox.getLore().isEmpty() ? defaultLore : lootbox.getLore());

        // Create empty sections if they don't exist
        config.createSection("items");
        config.createSection("locations");

        // Save items if any exist
        if (!lootbox.getItems().isEmpty()) {
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            for (LootboxItem item : lootbox.getItems().values()) {
                assert itemsSection != null;
                item.save(itemsSection.createSection(item.getId()));
            }
        }

        // Save locations if any exist
        if (!lootbox.getLocations().isEmpty()) {
            ConfigurationSection locationsSection = config.getConfigurationSection("locations");
            int locIndex = 0;
            for (Location location : lootbox.getLocations()) {
                assert locationsSection != null;
                ConfigurationSection locationSection = locationsSection.createSection(String.valueOf(locIndex++));
                locationSection.set("world", location.getWorld().getName());
                locationSection.set("x", location.getX());
                locationSection.set("y", location.getY());
                locationSection.set("z", location.getZ());
            }
        }

        // Statistics
        config.set("openedCount", lootbox.getOpenCount());

        try {
            config.save(file);
            Logger.debug("Saved lootbox: " + lootbox.getId());
        } catch (IOException e) {
            Logger.error("Failed to save lootbox: " + lootbox.getId(), e);
        }
    }

    public void saveAll() {
        // Save all lootboxes
        for (Lootbox lootbox : lootboxes.values()) {
            if (!isExampleLootbox(lootbox.getId()) || lootbox.hasBeenModified()) {
                saveLootbox(lootbox);
            }
        }
    }

    public boolean isExampleLootbox(String id) {
        return id.equals("example") || id.equals("example2");
    }

    public Lootbox getLootbox(String id) {
        return lootboxes.get(id);
    }

    public List<String> getLootboxNames() {
        return new ArrayList<>(lootboxes.keySet().stream()
                .filter(id -> !isExampleLootbox(id))
                .toList());
    }

    public List<String> getLootboxNamesAdmin() {
        return new ArrayList<>(lootboxes.keySet());
    }

    public Collection<Lootbox> getAllLootboxes() {
        // For non-admins, filter out example lootboxes
        return lootboxes.values().stream()
                .filter(lootbox -> !isExampleLootbox(lootbox.getId()))
                .toList();
    }

    public Collection<Lootbox> getAllLootboxesAdmin() {
        // For admins, show all lootboxes
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
            Logger.debug("Created example.yml from resources");
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
                    config.set("id", id);
                    config.save(file);
                }

                Lootbox lootbox = Lootbox.fromConfig(config);
                lootboxes.put(id, lootbox);
                Logger.debug("Loaded lootbox: " + id);
            } catch (Exception e) {
                Logger.error("Failed to load lootbox from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        int totalLoaded = lootboxes.size();
        int exampleCount = (int) lootboxes.values().stream()
                .filter(lb -> isExampleLootbox(lb.getId()))
                .count();
        int customCount = totalLoaded - exampleCount;

        Logger.info(String.format("Loaded %d lootboxes (%d custom, %d example) in %s mode",
                totalLoaded, customCount, exampleCount, isTrial ? "trial" : "free"));

        if (customCount >= maxLootboxes && maxLootboxes != -1) {
            Logger.warning(String.format("Reached %s mode limit of %d custom lootboxes",
                    isTrial ? "trial" : "free", maxLootboxes));
        }
    }

    public void respawnEntities() {
        // Add a respawning flag to prevent multiple concurrent respawns
        if (plugin.getServer().getScheduler().isCurrentlyRunning(respawnTaskId)) {
            Logger.debug("Respawn task already running, skipping...");
            return;
        }

        int totalLocations = 0;
        Map<Chunk, Boolean> chunksToLoad = new HashMap<>();

        // First count valid locations and identify chunks to load
        for (Lootbox lootbox : lootboxes.values()) {
            List<Location> locations = lootbox.getLocations();
            if (locations != null && !locations.isEmpty()) {
                for (Location location : locations) {
                    if (location != null && location.getWorld() != null) {
                        totalLocations++;
                        Chunk chunk = location.getChunk();
                        chunksToLoad.put(chunk, !chunk.isLoaded());
                    }
                }
            }
        }

        if (totalLocations == 0) {
            Logger.debug("No lootbox locations to respawn");
            return;
        }

        // Load necessary chunks
        chunksToLoad.forEach((chunk, needsLoading) -> {
            if (needsLoading) {
                chunk.load();
            }
            chunk.setForceLoaded(true);
        });

        // Wait a bit to ensure chunks are loaded
        int finalTotalLocations = totalLocations;
        respawnTaskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Remove any existing lootbox entities first
            for (World world : plugin.getServer().getWorlds()) {
                world.getEntities().stream()
                        .filter(entity -> entity instanceof ArmorStand
                        && entity.hasMetadata("LootboxEntity"))
                        .forEach(Entity::remove);
            }

            // Clear existing entities map
            entities.clear();

            // Spawn new entities
            int respawnedCount = 0;
            for (Lootbox lootbox : lootboxes.values()) {
                for (Location location : lootbox.getLocations()) {
                    if (location != null && location.getWorld() != null) {
                        LootboxEntity entity = new LootboxEntity(plugin, location, lootbox);
                        entities.put(entity.getUniqueId(), entity);
                        respawnedCount++;
                    }
                }
            }

            Logger.debug("Respawned " + respawnedCount + " lootbox entities (from " + finalTotalLocations + " valid locations)");
            respawnTaskId = -1;
        }, 20L).getTaskId(); // Store the task ID
    }

    public void cleanup() {
        // Cancel any pending respawn task
        if (respawnTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(respawnTaskId);
            respawnTaskId = -1;
        }

        // Remove all entities and unforce chunks
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

    public LootboxEntity getEntityById(UUID entityId) {
        return entities.get(entityId);
    }

    public Collection<LootboxEntity> getAllEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    public LootboxEntity getLootboxEntityAtTarget(Player player) {
        // Get the target location the player is looking at
        Location targetLoc = player.getTargetBlock(null, 5).getLocation().add(0.5, 0, 0.5);

        // Check for entities near the target location
        for (LootboxEntity entity : getAllEntities()) {
            Location entityLoc = entity.getLocation();

            // Check if locations are close enough (within 1 block)
            if (entityLoc.getWorld().equals(targetLoc.getWorld()) &&
                entityLoc.distance(targetLoc) <= 1.5) {
                return entity;
            }
        }
        return null;
    }

    public RemoveResult removeLootboxEntity(LootboxEntity entity) {
        // Remove the entity
        entity.remove();
        entities.remove(entity.getUniqueId());

        // Get the lootbox and remove the location
        Lootbox lootbox = getLootbox(entity.getLootboxId());
        if (lootbox != null) {
            Location loc = entity.getLocation();

            // Remove location from lootbox data
            lootbox.removeLocation(loc);

            // Save the updated lootbox file
            File file = new File(plugin.getDataFolder(), "lootboxes/" + lootbox.getId() + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Clear existing locations section and create new one if there are remaining locations
            config.set("locations", null); // This removes the entire locations section

            if (!lootbox.getLocations().isEmpty()) {
                getEntityPos(lootbox, config);
            }

            try {
                config.save(file);
                Logger.debug("Removed lootbox location from " + lootbox.getId() + " at " +
                    loc.getWorld().getName() + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ());
            } catch (IOException e) {
                Logger.error("Failed to save lootbox after removing location: " + lootbox.getId(), e);
            }

            // Create components for success message
            Component displayName = MiniMessage.miniMessage().deserialize(lootbox.getDisplayName());
            Component locationText = Component.text("at ")
                .color(LootboxCommand.DESCRIPTION_COLOR)
                .append(Component.text(String.format("%.1f, %.1f, %.1f",
                    loc.getX(), loc.getY(), loc.getZ()))
                    .color(LootboxCommand.TARGET_COLOR));

            return new RemoveResult(displayName, locationText);
        }
        return null;
    }

    // Add this record to store the removal result
    public record RemoveResult(Component displayName, Component locationText) {}

    private void getLootboxPosition(Lootbox lootbox, YamlConfiguration config) {
        getLootboxPostion(lootbox, config);
    }

    private void getLootboxPostion(Lootbox lootbox, YamlConfiguration config) {
        getEntityPos(lootbox, config);
    }

    private void getEntityPos(Lootbox lootbox, YamlConfiguration config) {
        // First, completely remove the old locations section
        config.set("locations", null);

        // Create empty or filled locations section
        ConfigurationSection locationsSection = config.createSection("locations");

        // Add remaining locations with fresh indices if any exist
        if (!lootbox.getLocations().isEmpty()) {
            int locIndex = 0;
            for (Location location : lootbox.getLocations()) {
                ConfigurationSection locationSection = locationsSection.createSection(String.valueOf(locIndex++));
                locationSection.set("world", location.getWorld().getName());
                locationSection.set("x", location.getX());
                locationSection.set("y", location.getY());
                locationSection.set("z", location.getZ());
            }
        }
        // If no locations, the section will remain empty but exist
    }

    private LootboxItem createUpdatedItem(LootboxItem existingItem, double newChance) {
        return existingItem instanceof OraxenLootboxItem oraxenItem ?
            new OraxenLootboxItem(
                existingItem.getItem(),
                oraxenItem.getOraxenId(),
                existingItem.getId(),
                newChance,
                existingItem.getRarity(),
                existingItem.getOriginalConfig()
            ) :
            new MinecraftLootboxItem(
                existingItem.getItem(),
                existingItem.getId(),
                newChance,
                existingItem.getRarity(),
                existingItem.getAction(),
                existingItem.getOriginalConfig()
            );
    }
}
