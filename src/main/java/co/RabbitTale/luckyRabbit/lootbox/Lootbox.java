package co.RabbitTale.luckyRabbit.lootbox;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.th0rgal.oraxen.utils.drops.Loot;
import java.io.File;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;

import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbit.lootbox.items.OraxenLootboxItem;
import lombok.Getter;
import co.RabbitTale.luckyRabbit.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import co.RabbitTale.luckyRabbit.LuckyRabbit;

@Getter
public class Lootbox {

    /**
     * -- GETTER -- Gets the unique identifier for this lootbox.
     *
     */
    @Getter
    private final String id;
    /**
     * -- GETTER -- Gets the display name of this lootbox.
     *
     */
    @Getter
    private final String title;
    /**
     * -- GETTER -- Gets the lore lines for this lootbox.
     *
     */
    @Getter
    private List<String> lore;
    /**
     * -- GETTER -- Gets all items in this lootbox.
     *
     */
    @Getter
    private final Map<String, LootboxItem> items;
    /**
     * -- GETTER -- Gets all locations where this lootbox can spawn.
     *
     */
    private Map<UUID, Location> locations = new HashMap<>();
    /**
     * -- GETTER -- Gets the animation type for this lootbox.
     *
     */
    @Getter
    private AnimationType animationType;
    /**
     * -- GETTER -- Gets the number of times this lootbox has been opened.
     *
     */
    @Getter
    @Setter
    private long created;

    private int openCount;
    private boolean modified = false;
    private boolean isExample = false;
    private List<String> descriptions = new ArrayList<>();
    private boolean keyRequired = false;
    private String keyName = null;

    /**
     * Creates a new lootbox instance.
     *
     * @param id Unique identifier
     * @param title Display name (supports MiniMessage format)
     * @param animationType Animation type to use
     */
    public Lootbox(String id, String title, AnimationType animationType) {
        this.id = id;
        this.title = title;
        this.lore = new ArrayList<>();
        this.items = new HashMap<>();
        this.locations = new HashMap<>();
        this.animationType = animationType;
        this.openCount = 0;
        this.created = System.currentTimeMillis(); // Set current timestamp for new lootboxes
    }

    /**
     * Creates a lootbox from a configuration section.
     *
     * @param config YAML configuration to load from
     * @return New Lootbox instance
     */
    public static Lootbox fromConfig(File file, FileConfiguration config) {
        String id = config.getString("id");
        String title = config.getString("title", id);
        AnimationType animationType = AnimationType.valueOf(
                config.getString("animationType", "HORIZONTAL").toUpperCase()
        );

        Lootbox lootbox = new Lootbox(id, title, animationType);

        Object createdObj = config.get("created");
        long created;

        if (createdObj instanceof Number n) {
            created = n.longValue();
        } else if (createdObj instanceof String s) {
            created = parseDateString(s, System.currentTimeMillis()); // Use current time as fallback, not file time
        } else if (createdObj instanceof java.util.Date date) { // Handle Date objects from YAML
            created = date.getTime();
        } else {
            // If no created field in YAML, use current time and mark as modified so it gets saved
            created = System.currentTimeMillis();
            lootbox.setModified();
        }
        lootbox.setCreated(created);

        // Load lore
        lootbox.lore.addAll(config.getStringList("lore"));

        // Load descriptions (max 5)
        List<String> descs = config.getStringList("description");
        if (descs.size() > 5) {
            descs = descs.subList(0, 5);
            lootbox.setModified(true);
        }
        lootbox.descriptions = descs;

        // Load items
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    LootboxItem item = LootboxItem.fromConfig(itemSection);
                    lootbox.items.put(item.getId(), item);
                }
            }
        }

        // Load locations
        ConfigurationSection locationsSection = config.getConfigurationSection("locations");
        if (locationsSection != null) {
            for (String key : locationsSection.getKeys(false)) {
                ConfigurationSection locationSection = locationsSection.getConfigurationSection(key);
                if (locationSection != null) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        Location location = Location.deserialize(locationSection.getValues(true));
                        lootbox.locations.put(uuid, location);
                    } catch (IllegalArgumentException e) {
                        Logger.error("Invalid UUID key '" + key + "' in locations for " + file.getName() + " - skipping location");
                    }
                }
            }
        }

        // Load statistics
        lootbox.openCount = config.getInt("openedCount", 0);

        // Load key settings
        lootbox.keyRequired = config.getBoolean("key_required", false);
        ConfigurationSection keySection = config.getConfigurationSection("key");
        if (keySection != null && keySection.contains("name")) {
            lootbox.keyName = keySection.getString("name");
        }

        return lootbox;
    }

    private static long parseDateString(String s, long fallback) {
        if (s == null || s.trim().isEmpty()) {
            return fallback;
        }

        s = s.trim(); // Remove any whitespace

        try {
            // Support different date formats found in YAML files
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2024-11-16 19:57:00
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), // 2024-11-16 19:57
                DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 2024-11-16
                DateTimeFormatter.ISO_LOCAL_DATE_TIME, // ISO format
                DateTimeFormatter.ISO_INSTANT // Instant format
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(s, formatter);
                    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (Exception ignored) {
                    // Try next format
                }
            }

            // If no format worked, use fallback silently
        } catch (Exception ignored) {
            // Use fallback silently
        }
        return fallback;
    }

    /**
     * Adds an item to the lootbox.
     *
     * @param item Item to add
     */
    public void addItem(LootboxItem item) {
        items.put(item.getId(), item);
        modified = true;
    }

    /**
     * Removes an item from the lootbox.
     *
     * @param item Item to remove
     */
    public void removeItem(ItemStack item) {
        items.values().removeIf(lootboxItem -> lootboxItem.getItem().isSimilar(item));
        modified = true;
    }

    /**
     * Adds a spawn location for this lootbox.
     *
     * @param uuid Unique identifier for the location
     * @param location Location to add
     */
    public void addLocation(UUID uuid, Location location) {
        locations.put(uuid, location);
        modified = true;
    }

    /**
     * Adds a spawn location for this lootbox.
     *
     * @param location Location to add
     */
    public void addLocation(Location location) {
        locations.put(UUID.randomUUID(), location);
        modified = true;
    }

    /**
     * Gets the number of times this lootbox has been opened.
     *
     * @return Number of times opened
     */
    public int getOpenCount() {
        return openCount;
    }

    /**
     * Increments the open count statistic.
     */
    public void incrementOpenCount() {
        this.openCount++;
        setModified();
    }

    /**
     * Checks if the lootbox has been modified since loading.
     *
     * @return true if modified, false otherwise
     */
    public boolean hasBeenModified() {
        return modified;
    }

    /**
     * Sets the modified flag for this lootbox.
     */
    public void setModified() {
        this.modified = true;
    }

    /**
     * Sets the modified flag for this lootbox.
     *
     * @param modified The new modified state
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * Enforces animation restrictions based on license. Forces HORIZONTAL
     * animation for non-premium users. FIXME: use new animation manager (yml)
     */
    public void enforceAnimationRestrictions() {
//        if (id.startsWith("example")) {
//            return; // Don't enforce restrictions on example lootboxes
//        }
//
//        // Only fallback to HORIZONTAL if the requested animation is not available
//        if (!FeatureManager.canUseAnimation(animationType.name()) && animationType != AnimationType.HORIZONTAL) {
//            this.animationType = AnimationType.HORIZONTAL;
//            this.modified = true;
//        }
    }

    /**
     * Checks if a location with the given UUID exists.
     *
     * @param uuid The UUID of the location to check
     * @return true if the location exists, false otherwise
     */
    public boolean hasLocation(UUID uuid) {
        return locations.containsKey(uuid);
    }

    /**
     * Checks if a location with the given Location object exists.
     *
     * @param location The Location object to check
     * @return true if the location exists, false otherwise
     */
    public boolean hasLocation(Location location) {
        return locations.values().stream().anyMatch(loc -> loc.equals(location));
    }

    /**
     * Removes a spawn location by UUID.
     *
     * @param uuid The UUID of the location to remove
     */
    public void removeLocation(UUID uuid) {
        locations.remove(uuid);
        modified = true;
    }

    /**
     * Sets the lore for this lootbox.
     *
     * @param lore New lore lines
     */
    public void setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        this.modified = true;
    }

    /**
     * Checks if this is an example lootbox.
     *
     * @return true if this is an example lootbox, false otherwise
     */
    public boolean isExample() {
        return isExample;
    }

    /**
     * Sets whether this is an example lootbox.
     *
     * @param isExample true to mark as example, false otherwise
     */
    public void setExample(boolean isExample) {
        this.isExample = isExample;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    /**
     * Checks if this lootbox requires a key to open.
     *
     * @return true if key is required, false otherwise
     */
    public boolean isKeyRequired() {
        return keyRequired;
    }

    /**
     * Gets the key name required to open this lootbox.
     *
     * @return name of the key, or null if no key required
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Sets whether this lootbox requires a key.
     *
     * @param required true to require key, false otherwise
     */
    public void setKeyRequired(boolean required) {
        this.keyRequired = required;
        setModified();
    }

    /**
     * Sets the key name required to open this lootbox.
     *
     * @param name name of the key
     */
    public void setKeyName(String name) {
        this.keyName = name;
        setModified();
    }
}
