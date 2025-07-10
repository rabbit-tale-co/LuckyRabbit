package co.RabbitTale.luckyRabbit.lootbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Set;
import java.util.HashSet;

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

    /*
     * LootboxManager.java
     *
     * Core manager class for handling all lootbox-related functionality.
     * Manages lootbox creation, loading, saving, and entity handling.
     *
     * Features:
     * - YAML-based lootbox configuration
     * - Dynamic lootbox entity spawning and management
     * - Support for Oraxen custom items
     * - Automatic chance calculation system
     * - Example lootbox templates
     * - License-based feature restrictions
     *
     * File Structure:
     * lootboxes/example
     *   ├── example.yml  - Example lootbox template
     *   ├── example2.yml - Additional example template
     *   └── custom/      - User-created lootboxes
     *
     * Entity Management:
     * - Automatic entity respawning on server restart
     * - Chunk loading management for entities
     * - Entity cleanup on plugin disable
     * - Location persistence in config
     *
     * Restrictions:
     * - Free version: Limited number of custom lootboxes
     * - Trial version: Increased limits with time restriction
     * - Premium: Unlimited lootboxes and features
     */
    private final LuckyRabbit plugin;
    private final Map<String, Lootbox> lootboxes;
    private final Map<UUID, LootboxEntity> entities;
    private int respawnTaskId = -1;

    // Add reference to examples directory
    private final File examplesFolder;
    private final File lootboxFolder;

    /**
     * Initializes the LootboxManager.
     *
     * @param plugin The LuckyRabbit plugin instance
     */
    public LootboxManager(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.lootboxes = new HashMap<>();
        this.entities = new HashMap<>();

        // Initialize folder references
        this.lootboxFolder = new File(plugin.getDataFolder(), "lootboxes");
        this.examplesFolder = new File(lootboxFolder, "examples");
    }

    /**
     * Loads all lootboxes from the lootboxes directory. Creates example
     * lootboxes if they don't exist. Enforces license restrictions on
     * animations and items.
     */
    public void loadLootboxes() {
        // Create main lootbox directory if it doesn't exist
        if (!lootboxFolder.exists()) {
            Logger.debug("Lootbox folder doesn't exist, creating...");
            if (!lootboxFolder.mkdirs()) {
                Logger.error("Failed to create lootboxes directory!");
                return;
            }
        }

        // Create examples directory if it doesn't exist
        if (!examplesFolder.exists()) {
            Logger.debug("Examples folder doesn't exist, creating...");
            if (!examplesFolder.mkdirs()) {
                Logger.error("Failed to create examples directory!");
                return;
            }
        }

        // Get resource YML files using a dynamic approach
        List<String> resourceYmlFiles = findYmlResourceFiles();

        // If no files found, fall back to default list
        if (resourceYmlFiles.isEmpty()) {
            Logger.warning("Could not find any example YML files in resources");
        }

        Logger.debug("Found " + resourceYmlFiles.size() + " example YML files in resources");

        // Process each found YML file
        for (String fileName : resourceYmlFiles) {
            File file = new File(examplesFolder, fileName);
            if (!file.exists()) {
                try {
                    // Check if the resource exists
                    InputStream resource = plugin.getResource("lootboxes/" + fileName);
                    if (resource != null) {
                        // Save to examples directory, not main lootboxes directory
                        File targetFile = new File(examplesFolder, fileName);
                        if (!targetFile.exists()) {
                            java.nio.file.Files.copy(resource, targetFile.toPath());
                            Logger.debug("Created example file: " + targetFile.getPath());
                        }
                        resource.close();
                    } else {
                        Logger.debug("Resource not found: " + fileName);
                    }
                } catch (Exception e) {
                    Logger.error("Failed to create example file " + fileName + ": " + e.getMessage());
                }
            }
        }

        // Clear existing lootboxes
        lootboxes.clear();

        // First load example lootboxes from examples directory
        Logger.debug("Loading examples from: " + examplesFolder.getAbsolutePath());
        loadLootboxesFromDirectory(examplesFolder, true);

        // Then load user lootboxes from main directory (excluding examples subdirectory)
        Logger.debug("Loading user lootboxes from: " + lootboxFolder.getAbsolutePath());
        loadLootboxesFromDirectory(lootboxFolder, false);

        Logger.debug("Total lootboxes loaded: " + lootboxes.size());
        Logger.debug("Lootbox IDs: " + String.join(", ", lootboxes.keySet()));

        // Debug: show which are examples
        List<String> exampleIds = lootboxes.values().stream()
                .filter(Lootbox::isExample)
                .map(Lootbox::getId)
                .toList();
        Logger.debug("Example lootboxes: " + String.join(", ", exampleIds));

        List<String> normalIds = lootboxes.values().stream()
                .filter(lootbox -> !lootbox.isExample())
                .map(Lootbox::getId)
                .toList();
        Logger.debug("Normal lootboxes: " + String.join(", ", normalIds));
    }

    /**
     * Loads lootboxes from the specified directory.
     *
     * @param directory Directory to load from
     * @param isExampleDir Whether this is the examples directory
     */
    private void loadLootboxesFromDirectory(File directory, boolean isExampleDir) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        loadRecursive(directory, isExampleDir);
    }

    private void loadRecursive(File dir, boolean isExample) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (!isExample && file.getName().equals("examples")) {
                    continue; // Skip examples subdir when loading main
                }
                loadRecursive(file, isExample);
            } else if (file.getName().endsWith(".yml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Lootbox lootbox = Lootbox.fromConfig(file, config);
                if (lootbox != null) {
                    lootbox.setExample(isExample);
                    lootboxes.put(lootbox.getId(), lootbox);
                    Logger.debug("Loaded " + (isExample ? "example " : "") + "lootbox: " + lootbox.getId() + " from " + file.getPath());
                }
            }
        }
    }

    /**
     * Creates a new lootbox with the specified name and animation. Enforces
     * license restrictions on the number of lootboxes.
     *
     * @param name Display name for the lootbox (supports MiniMessage format)
     * @param animationType Animation type to use
     * @throws IllegalStateException if lootbox limit is reached
     * @throws IllegalArgumentException if lootbox already exists
     */
    public void createLootbox(String name, AnimationType animationType) {

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

    /**
     * Generates a safe ID from a lootbox name. Removes formatting tags and
     * special characters.
     *
     * @param name The display name to convert
     * @return Safe ID string
     * @throws IllegalArgumentException if ID already exists
     */
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

    /**
     * Deletes a lootbox and its entities. Removes all spawned entities and the
     * config file.
     *
     * @param id Lootbox ID to delete
     * @throws IllegalArgumentException if lootbox doesn't exist
     */
    public void deleteLootbox(String id) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) {
            throw new IllegalArgumentException("Lootbox with ID " + id + " does not exist!");
        }

        // Store the display name before deletion
        Component displayName = MiniMessage.miniMessage().deserialize(lootbox.getTitle());

        // Remove all entities
        for (Location location : lootbox.getLocations().values()) {
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

    /**
     * Adds an item to a lootbox with specified rarity and chance. Automatically
     * recalculates chances for other items.
     *
     * @param player Player adding the item (for messages)
     * @param lootboxId Target lootbox ID
     * @param item Item to add
     * @param rarity Item rarity (defaults to COMMON)
     * @param chance Custom chance (optional)
     */
    public void addItem(Player player, @NotNull String lootboxId, @NotNull ItemStack item, String rarity, Double chance) {
        // Validate parameters
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

        // Check if this is an Oraxen item
        String oraxenId;
        try {
            oraxenId = OraxenItems.getIdByItem(item);
        } catch (NoClassDefFoundError e) {
            // Oraxen is not available, treat as regular item
            oraxenId = null;
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

            // Create new item with calculated chance
            String newItemId = "item-" + existingItems.size();
            LootboxItem newItem = createLootboxItem(item, oraxenId, newItemId, finalChance, finalRarity);
            updatedItems.put(newItemId, newItem);

            // Then update all existing items with equal chance
            for (LootboxItem existingItem : existingItems.values()) {
                LootboxItem updatedItem;
                if (!existingItem.isChanceManuallySet()) {
                    updatedItem = createUpdatedItem(existingItem, finalChance);
                } else {
                    updatedItem = existingItem;
                }
                updatedItems.put(existingItem.getId(), updatedItem);
            }

            // Clear and update the lootbox items
            existingItems.clear();
            existingItems.putAll(updatedItems);
        } else {
            // Calculate remaining chance after manually set items
            double remainingChance = 100.0 - chance;
            double totalExistingChance = existingItems.values().stream()
                    .filter(LootboxItem::isChanceManuallySet)
                    .mapToDouble(LootboxItem::getChance)
                    .sum();

            if (totalExistingChance + chance > 100.0) {
                if (player != null) {
                    player.sendMessage(Component.text("Cannot add item! Total chance would exceed 100%")
                            .color(LootboxCommand.ERROR_COLOR));
                }
                return;
            }

            // Create a new map for updated items
            Map<String, LootboxItem> updatedItems = new HashMap<>();

            // Add new item with specified chance
            String newItemId = "item-" + existingItems.size();
            LootboxItem newItem = createLootboxItem(item, oraxenId, newItemId, chance, finalRarity);
            newItem.setChanceManuallySet(true);
            updatedItems.put(newItemId, newItem);

            // Update existing items
            for (Map.Entry<String, LootboxItem> entry : existingItems.entrySet()) {
                double newChance;
                if (entry.getValue().isChanceManuallySet()) {
                    newChance = entry.getValue().getChance();
                } else {
                    newChance = remainingChance / existingItems.values().stream()
                            .filter(lootItem -> !lootItem.isChanceManuallySet())
                            .count();
                }
                LootboxItem updatedItem = createUpdatedItem(entry.getValue(), newChance);
                updatedItems.put(entry.getKey(), updatedItem);
                Logger.debug("Updated item: " + entry.getKey() + " to " + newChance);
            }

            // Clear and update the lootbox items
            existingItems.clear();
            existingItems.putAll(updatedItems);
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
            existingItems.values().forEach(existingItem
                    -> player.sendMessage(Component.text("» ", LootboxCommand.SEPARATOR_COLOR)
                            .append(Component.text(existingItem.getId() + ": ", LootboxCommand.DESCRIPTION_COLOR))
                            .append(Component.text(String.format("%.1f%%", existingItem.getChance()))
                                    .color(LootboxCommand.TARGET_COLOR))));
            player.sendMessage(Component.empty());
        }
    }

    /**
     * Creates a new LootboxItem instance. Handles both Minecraft and Oraxen
     * items.
     *
     * @param item The ItemStack to create from
     * @param oraxenId Oraxen ID if applicable
     * @param itemId Internal item ID
     * @param chance Drop chance
     * @param rarity Item rarity
     * @return New LootboxItem instance
     */
    private LootboxItem createLootboxItem(ItemStack item, String oraxenId, String itemId, double chance, String rarity) {
        if (oraxenId != null) {
            return new OraxenLootboxItem(item, oraxenId, itemId, chance, rarity, null);
        } else {
            return new MinecraftLootboxItem(item, itemId, chance, rarity, null, null);
        }
    }

    /**
     * Removes an item from a lootbox.
     *
     * @param player Player removing the item
     * @param id Lootbox ID
     * @throws IllegalArgumentException if lootbox doesn't exist or player isn't
     * holding an item
     */
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
                .append(MiniMessage.miniMessage().deserialize(lootbox.getTitle()))
                .append(Component.text(" (")
                        .color(LootboxCommand.DESCRIPTION_COLOR))
                .append(Component.text(item.getType().toString())
                        .color(LootboxCommand.ITEM_COLOR))
                .append(Component.text(")")
                        .color(LootboxCommand.DESCRIPTION_COLOR));
        player.sendMessage(message);
    }

    /**
     * Places a lootbox at the player's location.
     *
     * @param player The player placing the lootbox
     * @param id The ID of the lootbox to place
     */
    public void placeLootbox(Player player, String id) {
        Lootbox lootbox = getLootbox(id);
        if (lootbox == null) {
            player.sendMessage(Component.text("Lootbox not found: " + id).color(LootboxCommand.ERROR_COLOR));
            return;
        }

        if (lootbox.isExample()) {
            player.sendMessage(Component.text("Cannot place example lootboxes!").color(LootboxCommand.ERROR_COLOR));
            return;
        }

        Location location = player.getLocation();
        if (placeLootbox(lootbox, location)) {
            lootbox.setModified(true);
            saveLootbox(lootbox);
            player.sendMessage(Component.text("Successfully placed ").color(LootboxCommand.SUCCESS_COLOR)
                    .append(MiniMessage.miniMessage().deserialize(lootbox.getTitle()))
                    .append(Component.text(" at your location!").color(LootboxCommand.SUCCESS_COLOR)));
        } else {
            player.sendMessage(Component.text("Failed to place lootbox - location already occupied or invalid!")
                    .color(LootboxCommand.ERROR_COLOR));
        }
    }

    /**
     * Places a lootbox in the world. Creates a new LootboxEntity at the
     * player's location.
     *
     * @param player Player placing the lootbox
     * @param id Lootbox ID to place
     * @throws IllegalArgumentException if lootbox doesn't exist
     */
    public boolean placeLootbox(Lootbox lootbox, Location location) {
        // Don't allow placing example lootboxes
        if (lootbox.isExample()) {
            Logger.debug("Attempted to place example lootbox: " + lootbox.getId());
            return false;
        }

        // Check if location is already occupied
        if (hasEntityAtLocation(location)) {
            Logger.debug("Location already occupied: " + location);
            return false;
        }

        // Generate a new UUID for this placement
        UUID uuid = UUID.randomUUID();

        // Add location to lootbox config
        lootbox.addLocation(uuid, location);

        // Spawn entity
        LootboxEntity entity = spawnEntity(lootbox, location, uuid);
        if (entity == null) {
            Logger.error("Failed to spawn lootbox entity at " + location);
            return false;
        }

        Logger.debug("Placed lootbox: " + lootbox.getId() + " at " + location + " with UUID: " + uuid);
        return true;
    }

    /**
     * Saves a lootbox configuration to file.
     *
     * @param lootbox The lootbox to save
     */
    public void saveLootbox(Lootbox lootbox) {
        File file = new File(lootboxFolder, lootbox.getId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Basic info
        config.set("id", lootbox.getId());
        config.set("title", lootbox.getTitle());
        config.set("animationType", lootbox.getAnimationType().name());
        config.set("openedCount", lootbox.getOpenCount());
        config.set("created", lootbox.getCreated());

        // Save descriptions
        config.set("description", lootbox.getDescriptions());

        // Save lore
        config.set("lore", lootbox.getLore());

        // Save items
        ConfigurationSection itemsSection = config.createSection("items");
        for (Map.Entry<String, LootboxItem> entry : lootbox.getItems().entrySet()) {
            ConfigurationSection itemSection = itemsSection.createSection(entry.getKey());
            entry.getValue().saveToConfig(itemSection);
        }

        // Save locations
        ConfigurationSection locationsSection = config.createSection("locations");
        for (Map.Entry<UUID, Location> entry : lootbox.getLocations().entrySet()) {
            ConfigurationSection locationSection = locationsSection.createSection(entry.getKey().toString());
            Map<String, Object> serialized = entry.getValue().serialize();
            for (Map.Entry<String, Object> serEntry : serialized.entrySet()) {
                locationSection.set(serEntry.getKey(), serEntry.getValue());
            }
        }

        try {
            config.save(file);
            lootbox.setModified(false);
        } catch (IOException e) {
            Logger.error("Failed to save lootbox " + lootbox.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Saves all lootboxes to disk. Only saves modified example lootboxes.
     */
    public void saveAll() {
        // Save all lootboxes
        for (Lootbox lootbox : lootboxes.values()) {
            if (!isExampleLootbox(lootbox.getId()) || lootbox.hasBeenModified()) {
                saveLootbox(lootbox);
            }
        }
    }

    /**
     * Checks if a lootbox is an example lootbox.
     *
     * @param id Lootbox ID to check
     * @return true if example lootbox
     */
    public boolean isExampleLootbox(String id) {
        // Only use the isExample flag - no hardcoded names
        Lootbox lootbox = lootboxes.get(id);
        return lootbox != null && lootbox.isExample();
    }

    /**
     * Gets a lootbox by its ID.
     *
     * @param id Lootbox ID
     * @return Lootbox instance or null if not found
     */
    public Lootbox getLootbox(String id) {
        return lootboxes.get(id);
    }

    /**
     * Gets a list of all non-example lootbox names. Used for regular players.
     *
     * @return List of lootbox IDs
     */
    public List<String> getLootboxNames() {
        return new ArrayList<>(lootboxes.keySet().stream()
                .filter(id -> !isExampleLootbox(id))
                .toList());
    }

    /**
     * Gets a list of all lootbox names including examples. Used for admins.
     *
     * @return List of all lootbox IDs
     */
    public List<String> getLootboxNamesAdmin() {
        return new ArrayList<>(lootboxes.keySet());
    }

    /**
     * Gets all non-example lootboxes. Used for regular players.
     *
     * @return Collection of lootboxes
     */
    public Collection<Lootbox> getAllLootboxes() {
        // For non-admins, filter out example lootboxes
        return lootboxes.values().stream()
                .filter(lootbox -> !isExampleLootbox(lootbox.getId()))
                .toList();
    }

    /**
     * Gets all lootboxes including examples. Used for admins.
     *
     * @return Collection of all lootboxes
     */
    public Collection<Lootbox> getAllLootboxesAdmin() {
        // For admins, show all lootboxes
        return Collections.unmodifiableCollection(lootboxes.values());
    }

    /**
     * Public method to clean up orphaned entities. Called during plugin startup
     * to ensure clean state.
     */
    public void cleanupAllOrphanedEntities() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (stand.hasMetadata("LootboxEntity")) {
                    String id = stand.getMetadata("LootboxEntity").get(0).asString();
                    Lootbox lootbox = getLootbox(id);
                    if (lootbox == null || !lootbox.getLocations().containsValue(stand.getLocation())) {
                        stand.remove();
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " orphaned lootbox entities");
        }
    }

    public LootboxEntity spawnEntity(Lootbox lootbox, Location location, UUID uuid) {
        // First check if we already have an entity at this location
        if (hasEntityAtLocation(location)) {
            Logger.debug("Entity already exists at location " + location);
            return null;
        }

        // Check if we already have an entity with this UUID
        if (entities.containsKey(uuid)) {
            Logger.debug("Entity already exists with UUID " + uuid);
            return null;
        }

        // Remove any existing entities at this exact location (cleanup)
        location.getWorld().getEntities().stream()
                .filter(entity -> entity instanceof ArmorStand
                && entity.hasMetadata("LootboxEntity")
                && entity.getLocation().distance(location) < 0.1)
                .forEach(Entity::remove);

        // Create new entity
        LootboxEntity entity = new LootboxEntity(plugin, location, lootbox);
        entities.put(entity.getUniqueId(), entity);
        return entity;
    }

    public void respawnEntities() {
        Logger.debug("Respawning all lootbox entities...");

        // First, remove all existing entities
        removeAllEntities();

        // Clear the entities map since we removed all entities
        entities.clear();

        // Now respawn entities for each lootbox
        for (Lootbox lootbox : lootboxes.values()) {
            for (Map.Entry<UUID, Location> entry : lootbox.getLocations().entrySet()) {
                UUID uuid = entry.getKey();
                Location location = entry.getValue();

                // Only spawn if no entity exists at this location
                if (!hasEntityAtLocation(location)) {
                    LootboxEntity entity = spawnEntity(lootbox, location, uuid);
                    if (entity != null) {
                        Logger.debug("Respawned entity for lootbox: " + lootbox.getId()
                                + " at " + location.getX() + ", " + location.getY() + ", " + location.getZ());
                    }
                }
            }
        }
    }

    public void removeAllEntities() {
        Logger.debug("Removing all lootbox entities...");

        // Remove all entities and their name tags
        for (World world : plugin.getServer().getWorlds()) {
            world.getEntities().stream()
                    .filter(entity -> entity instanceof ArmorStand
                    && (entity.hasMetadata("LootboxEntity")
                    || entity.hasMetadata("LootboxEntityUUID")))
                    .forEach(Entity::remove);
        }

        // Clear the entities map
        entities.clear();
    }

    /**
     * Checks if a lootbox entity already exists at the given location.
     *
     * @param location Location to check
     * @return true if entity exists, false otherwise
     */
    private boolean hasEntityAtLocation(Location location) {
        double radius = 1.0; // Check within 1 block radius

        return location.getWorld().getEntities().stream()
                .filter(entity -> entity instanceof ArmorStand && entity.hasMetadata("LootboxEntity"))
                .anyMatch(entity -> {
                    Location entityLoc = entity.getLocation();
                    return entityLoc.getWorld().equals(location.getWorld())
                            && Math.abs(entityLoc.getX() - location.getX()) < 0.1
                            && Math.abs(entityLoc.getY() - location.getY()) < 0.1
                            && Math.abs(entityLoc.getZ() - location.getZ()) < 0.1;
                });
    }

    /**
     * Cleans up all lootbox entities. Called during plugin disable and reload.
     */
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
            for (Location location : lootbox.getLocations().values()) {
                Chunk chunk = location.getChunk();
                if (chunk.isForceLoaded()) {
                    chunk.setForceLoaded(false);
                }
            }
        }
    }

    /**
     * Gets a lootbox entity by its UUID.
     *
     * @param entityId Entity UUID
     * @return LootboxEntity instance or null if not found
     */
    public LootboxEntity getEntityById(UUID entityId) {
        return entities.get(entityId);
    }

    /**
     * Gets all active lootbox entities.
     *
     * @return Unmodifiable collection of entities
     */
    public Collection<LootboxEntity> getAllEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    /**
     * Gets the lootbox entity a player is looking at.
     *
     * @param player Player to check
     * @return LootboxEntity if found within 5 blocks, null otherwise
     */
    public LootboxEntity getLootboxEntityAtTarget(Player player) {
        // Get the target location the player is looking at
        Location targetLoc = player.getTargetBlock(null, 5).getLocation().add(0.5, 0, 0.5);

        // Check for entities near the target location
        for (LootboxEntity entity : getAllEntities()) {
            Location entityLoc = entity.getLocation();

            // Check if locations are close enough (within 1 block)
            if (entityLoc.getWorld().equals(targetLoc.getWorld())
                    && entityLoc.distance(targetLoc) <= 1.5) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Removes a lootbox entity from the world. Updates configuration and sends
     * feedback.
     *
     * @param entity Entity to remove
     * @return RemoveResult containing display name and location, or null if
     * failed
     */
    public RemoveResult removeLootboxEntity(LootboxEntity entity) {
        // Remove the entity
        entity.remove();
        entities.remove(entity.getUniqueId());

        // Get the lootbox and remove the location
        Lootbox lootbox = getLootbox(entity.getLootboxId());
        if (lootbox != null) {
            Location loc = entity.getLocation();

            // Remove location from lootbox data
            lootbox.removeLocation(entity.getUniqueId());

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
                Logger.debug("Removed lootbox location from " + lootbox.getId() + " at "
                        + loc.getWorld().getName() + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ());
            } catch (IOException e) {
                Logger.error("Failed to save lootbox after removing location: " + lootbox.getId(), e);
            }

            // Create components for success message
            Component displayName = MiniMessage.miniMessage().deserialize(lootbox.getTitle());
            Component locationText = Component.text("at ")
                    .color(LootboxCommand.DESCRIPTION_COLOR)
                    .append(Component.text(String.format("%.1f, %.1f, %.1f",
                            loc.getX(), loc.getY(), loc.getZ()))
                            .color(LootboxCommand.TARGET_COLOR));

            return new RemoveResult(displayName, locationText);
        }
        return null;
    }

    /**
     * Result record for entity removal operations. Contains formatted
     * components for feedback messages.
     */
    public record RemoveResult(Component displayName, Component locationText) {

    }

    /**
     * Updates entity positions in configuration.
     *
     * @param lootbox Lootbox to update
     * @param config Configuration to save to
     */
    private void getEntityPos(Lootbox lootbox, YamlConfiguration config) {
        // First, completely remove the old locations section
        config.set("locations", null);

        // Create empty or filled locations section
        ConfigurationSection locationsSection = config.createSection("locations");

        // Add remaining locations with fresh indices if any exist
        if (!lootbox.getLocations().isEmpty()) {
            int locIndex = 0;
            for (Location location : lootbox.getLocations().values()) {
                ConfigurationSection locationSection = locationsSection.createSection(String.valueOf(locIndex++));
                locationSection.set("world", location.getWorld().getName());
                locationSection.set("x", location.getX());
                locationSection.set("y", location.getY());
                locationSection.set("z", location.getZ());
            }
        }
        // If no locations, the section will remain empty but exist
    }

    /**
     * Creates an updated item with new chance value. Preserves other item
     * properties.
     *
     * @param existingItem Item to update
     * @param newChance New chance value
     * @return Updated LootboxItem instance
     */
    private LootboxItem createUpdatedItem(LootboxItem existingItem, double newChance) {
        return existingItem instanceof OraxenLootboxItem oraxenItem
                ? new OraxenLootboxItem(
                        existingItem.getItem(),
                        oraxenItem.getOraxenId(),
                        existingItem.getId(),
                        newChance,
                        existingItem.getRarity(),
                        existingItem.getOriginalConfig()
                )
                : new MinecraftLootboxItem(
                        existingItem.getItem(),
                        existingItem.getId(),
                        newChance,
                        existingItem.getRarity(),
                        existingItem.getAction(),
                        existingItem.getOriginalConfig()
                );
    }

    /**
     * Finds all YML files in the resources/lootboxes directory. This uses
     * multiple approaches to handle different environments.
     *
     * @return List of YML filenames found in resources
     */
    private List<String> findYmlResourceFiles() {
        List<String> files = new ArrayList<>();
        String resourcePath = "lootboxes";

        try {
            // Method 1: Try to get the URL of the resource directory
            URL dirURL = getClass().getClassLoader().getResource(resourcePath);
            if (dirURL != null) {
                // Handle JAR files
                if (dirURL.getProtocol().equals("jar")) {
                    String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
                    JarFile jar = new JarFile(jarPath);
                    Enumeration<JarEntry> entries = jar.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.startsWith(resourcePath + "/") && name.endsWith(".yml")) {
                            String fileName = name.substring(name.lastIndexOf('/') + 1);
                            files.add(fileName);
                        }
                    }

                    jar.close();
                    return files;
                }
            }
        } catch (Exception e) {
            Logger.warning("Error finding resources: " + e.getMessage());
        }

        // Method 2: In development environments, scan the resource folder directly
        try {
            File resourceDir = new File("src/main/resources/lootboxes");
            if (resourceDir.exists() && resourceDir.isDirectory()) {
                File[] resourceFiles = resourceDir.listFiles((dir, name) -> name.endsWith(".yml"));
                if (resourceFiles != null) {
                    for (File file : resourceFiles) {
                        files.add(file.getName());
                    }
                    Logger.debug("Found " + files.size() + " example files in development resource directory");
                }
            }
        } catch (Exception e) {
            Logger.warning("Failed to scan development resource directory: " + e.getMessage());
        }

        return files;
    }

    public void removeAllEntities(String id) {
        Lootbox lootbox = getLootbox(id);
        if (lootbox == null) {
            return;
        }

        // Remove all entities and their name tags
        for (Location location : lootbox.getLocations().values()) {
            for (Entity entity : location.getWorld().getEntities()) {
                if (entity instanceof ArmorStand && entity.hasMetadata("LootboxEntity")) {
                    String lootboxId = entity.getMetadata("LootboxEntity").get(0).asString();
                    if (lootboxId.equals(id)) {
                        String entityUUID = entity.getMetadata("LootboxEntityUUID").get(0).asString();

                        // Remove all related name tags
                        location.getWorld().getEntities().stream()
                                .filter(e -> e instanceof ArmorStand
                                && e.hasMetadata("LootboxEntityUUID")
                                && e.getMetadata("LootboxEntityUUID").get(0).asString().equals(entityUUID))
                                .forEach(Entity::remove);

                        entity.remove();
                        entities.remove(UUID.fromString(entityUUID));
                    }
                }
            }
        }

        // Clear locations
        lootbox.getLocations().clear();

        // Mark as modified and save
        lootbox.setModified(true);
        saveLootbox(lootbox);
    }

    /**
     * Removes a lootbox entity by UUID.
     *
     * @param uuid The UUID of the lootbox entity to remove
     * @return true if removed, false if not found
     */
    public boolean removeLootbox(UUID uuid) {
        // Find the entity
        LootboxEntity entity = entities.get(uuid);
        if (entity == null) {
            Logger.debug("No entity found with UUID: " + uuid);
            return false;
        }

        // Get the lootbox config
        Lootbox lootbox = getLootbox(entity.getLootboxId());
        if (lootbox == null) {
            Logger.error("Lootbox config not found for entity: " + entity.getLootboxId());
            return false;
        }

        // Remove the entity
        entity.remove();
        entities.remove(uuid);

        // Remove the location from config
        lootbox.removeLocation(uuid);
        lootbox.setModified(true);
        saveLootbox(lootbox);

        Logger.debug("Removed lootbox entity: " + uuid);
        return true;
    }

    /**
     * Removes a lootbox at the specified location.
     *
     * @param location The location to remove from
     * @return true if removed, false if not found
     */
    public boolean removeLootbox(Location location) {
        // Find entity at location
        for (LootboxEntity entity : entities.values()) {
            if (entity.getLocation().distance(location) < 0.1) {
                return removeLootbox(entity.getUniqueId());
            }
        }
        return false;
    }

    /**
     * Removes a lootbox at the player's location.
     *
     * @param player The player removing the lootbox
     */
    public void removeLootbox(Player player) {
        Location location = player.getLocation();
        for (LootboxEntity entity : entities.values()) {
            if (entity.getLocation().distance(location) < 0.1) {
                removeLootboxByUUID(entity.getUniqueId());
                player.sendMessage(Component.text("Successfully removed lootbox!").color(LootboxCommand.SUCCESS_COLOR));
                return;
            }
        }
        player.sendMessage(Component.text("No lootbox found at your location!").color(LootboxCommand.ERROR_COLOR));
    }

    public void removeLootboxByUUID(UUID uuid) {
        LootboxEntity entity = entities.get(uuid);
        if (entity != null) {
            entity.remove();
            entities.remove(uuid);
        }
    }

    public void removeLootboxByLocation(Location location) {
        for (LootboxEntity entity : entities.values()) {
            if (entity.getLocation().distance(location) < 0.1) {
                removeLootboxByUUID(entity.getUniqueId());
                break;
            }
        }
    }
}
