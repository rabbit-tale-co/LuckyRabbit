package co.RabbitTale.luckyRabbitFoot.lootbox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbitFoot.lootbox.entity.LootboxEntity;
import co.RabbitTale.luckyRabbitFoot.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbitFoot.lootbox.items.OraxenLootboxItem;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class LootboxManager {
    private final LuckyRabbitFoot plugin;
    private final Map<String, Lootbox> lootboxes;
    private final Map<UUID, LootboxEntity> entities;

    public LootboxManager(LuckyRabbitFoot plugin) {
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
                Logger.info("Loading lootbox from: " + file.getAbsolutePath());
                if (!file.exists()) {
                    Logger.error("File doesn't exist: " + file.getAbsolutePath());
                    continue;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                // Debug config contents
                Logger.info("Config contents for " + file.getName() + ":");
                Logger.info("ID: " + config.getString("id"));
                Logger.info("DisplayName: " + config.getString("displayName"));
                Logger.info("AnimationType: " + config.getString("animationType"));
                Logger.info("Items count: " + (config.getConfigurationSection("items") != null ?
                    Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false).size() : 0));

                String id = file.getName().replace(".yml", "");
                Lootbox lootbox = Lootbox.fromConfig(config);
                lootboxes.put(id, lootbox);

                // Spawn entities at saved locations
                for (Location loc : lootbox.getLocations()) {
                    spawnEntity(id, loc);
                }

                Logger.info("Successfully loaded lootbox: " + id + " from " + file.getName());
                Logger.info("Lootbox items count: " + lootbox.getItems().size());
            } catch (Exception e) {
                Logger.error("Failed to load lootbox from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        Logger.info("Total lootboxes loaded: " + lootboxes.size());
        Logger.info("Lootbox IDs: " + String.join(", ", lootboxes.keySet()));
    }

    public void createLootbox(String name, AnimationType animationType) {
        String id = name.toLowerCase().replace(" ", "_");
        if (lootboxes.containsKey(id)) {
            throw new IllegalArgumentException("Lootbox with ID " + id + " already exists!");
        }

        Lootbox lootbox = new Lootbox(id, name, animationType);
        lootboxes.put(id, lootbox);
        saveLootbox(lootbox);
    }

    private void spawnEntity(String id, Location location) {
        Lootbox lootbox = lootboxes.get(id);
        if (lootbox == null) return;

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
                    String lootboxId = entity.getMetadata("LootboxEntity").getFirst().asString();
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
        config.set("displayName", lootbox.getDisplayName());
        config.set("lore", lootbox.getLore());
        config.set("animationType", lootbox.getAnimationType().name());
        config.set("openedCount", lootbox.getOpenCount());

        // Save items with full metadata
        ConfigurationSection itemsSection = config.createSection("items");
        int index = 0;
        for (LootboxItem item : lootbox.getItems().values()) {
            ConfigurationSection itemSection = itemsSection.createSection(String.valueOf(index));
            itemSection.set("id", item.getId());
            itemSection.set("chance", item.getChance());
            itemSection.set("rarity", item.getRarity());

            if (item instanceof OraxenLootboxItem oraxenItem) {
                itemSection.set("oraxen_item", oraxenItem.getOraxenId());
            } else {
                ConfigurationSection itemDataSection = itemSection.createSection("item");
                ItemStack itemStack = item.getItem();

                // Save basic item data
                itemDataSection.set("type", itemStack.getType().name());
                itemDataSection.set("amount", itemStack.getAmount());

                // Save metadata if exists
                if (itemStack.hasItemMeta()) {
                    ConfigurationSection metaSection = itemDataSection.createSection("meta");
                    ItemMeta meta = itemStack.getItemMeta();

                    // Save display name
                    if (meta.hasDisplayName()) {
                        metaSection.set("display-name", PlainTextComponentSerializer.plainText()
                            .serialize(Objects.requireNonNull(meta.displayName())));
                    }

                    // Save lore
                    if (meta.hasLore()) {
                        metaSection.set("lore", Objects.requireNonNull(meta.lore()).stream()
                            .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                            .collect(Collectors.toList()));
                    }

                    // Save enchantments
                    if (!meta.getEnchants().isEmpty()) {
                        ConfigurationSection enchantSection = metaSection.createSection("enchants");
                        meta.getEnchants().forEach((enchant, level) ->
                            enchantSection.set(enchant.getKey().getKey(), level));
                    }

                    // Save glow effect
                    if (meta.hasEnchants() && meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
                        metaSection.set("glow", true);
                    }
                }
            }

            // Save action if exists
            if (item.getAction() != null) {
                ConfigurationSection actionSection = itemSection.createSection("action");
                actionSection.set("type", item.getAction().type());
                item.getAction().getData().forEach(actionSection::set);
            }

            index++;
        }

        // Save locations
        ConfigurationSection locationsSection = config.createSection("locations");
        index = 0;
        for (Location location : lootbox.getLocations()) {
            index = getIndex(index, locationsSection, location);
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

    public void reloadOraxenItems() {
        for (Lootbox lootbox : lootboxes.values()) {
            for (LootboxItem item : lootbox.getItems().values()) {
                if (item instanceof OraxenLootboxItem oraxenItem) {
                    try {
                        var builder = OraxenItems.getItemById(oraxenItem.getOraxenId());
                        if (builder != null) {
                            // Update the item with the actual Oraxen item
                            ItemStack newItem = builder.build();
                            // You'll need to implement a method to update the item
                            // in your OraxenLootboxItem class
                        }
                    } catch (Exception e) {
                        Logger.warning("Failed to reload Oraxen item " + oraxenItem.getOraxenId());
                    }
                }
            }
        }
    }

    public Collection<Lootbox> getAllLootboxes() {
        return Collections.unmodifiableCollection(lootboxes.values());
    }

    // Additional methods will be implemented here...
}
