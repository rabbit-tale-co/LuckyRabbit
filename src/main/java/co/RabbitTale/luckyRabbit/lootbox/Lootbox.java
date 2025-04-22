package co.RabbitTale.luckyRabbit.lootbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.api.FeatureManager;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbit.lootbox.items.OraxenLootboxItem;
import lombok.Getter;

@Getter
public class Lootbox {

    private final String id;
    private final String displayName;
    private List<String> lore;
    private final Map<String, LootboxItem> items;
    private final List<Location> locations;
    private AnimationType animationType;
    private int openCount;
    private boolean modified = false;
    private boolean isExample = false;

    /**
     * Creates a new lootbox instance.
     *
     * @param id Unique identifier
     * @param displayName Display name (supports MiniMessage format)
     * @param animationType Animation type to use
     */
    public Lootbox(String id, String displayName, AnimationType animationType) {
        this.id = id;
        this.displayName = displayName;
        this.lore = new ArrayList<>();
        this.items = new HashMap<>();
        this.locations = new ArrayList<>();
        this.animationType = animationType;
        this.openCount = 0;
    }

    /**
     * Creates a lootbox from a configuration section.
     *
     * @param config YAML configuration to load from
     * @return New Lootbox instance
     */
    public static Lootbox fromConfig(FileConfiguration config) {
        String id = config.getString("id");
        String displayName = config.getString("displayName", id);
        AnimationType animationType = AnimationType.valueOf(
                config.getString("animationType", "HORIZONTAL").toUpperCase()
        );

        Lootbox lootbox = new Lootbox(id, displayName, animationType);

        // Load lore
        lootbox.lore.addAll(config.getStringList("lore"));

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
                    Location location = Location.deserialize(locationSection.getValues(true));
                    lootbox.locations.add(location);
                }
            }
        }

        // Load statistics
        lootbox.openCount = config.getInt("openedCount", 0);

        return lootbox;
    }

    /**
     * Gets the unique identifier for this lootbox.
     *
     * @return The lootbox ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name of this lootbox.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the lore lines for this lootbox.
     *
     * @return The lore as a list of strings
     */
    public List<String> getLore() {
        return lore;
    }

    /**
     * Gets all items in this lootbox.
     *
     * @return Map of item ID to LootboxItem
     */
    public Map<String, LootboxItem> getItems() {
        return items;
    }

    /**
     * Gets all locations where this lootbox can spawn.
     *
     * @return List of locations
     */
    public List<Location> getLocations() {
        return locations;
    }

    /**
     * Gets the animation type for this lootbox.
     *
     * @return The animation type
     */
    public AnimationType getAnimationType() {
        return animationType;
    }

    /**
     * Gets the number of times this lootbox has been opened.
     *
     * @return The open count
     */
    public int getOpenCount() {
        return openCount;
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
     * @param location Location to add
     */
    public void addLocation(Location location) {
        locations.add(location);
        modified = true;
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
     * Sets the animation type for this lootbox. If the requested animation is
     * not available, falls back to HORIZONTAL.
     *
     * @param animationType The new animation type
     * @return Whether the requested animation was available and set
     * successfully
     */
    public boolean setAnimationType(AnimationType animationType) {
        // Always allow HORIZONTAL
        if (animationType == AnimationType.HORIZONTAL) {
            this.animationType = animationType;
            this.modified = true;
            return true;
        }

        // Check if the requested animation is available
        if (FeatureManager.canUseAnimation(animationType.name())) {
            this.animationType = animationType;
            this.modified = true;
            return true;
        } else {
            // If not available, keep the current animation and return false
            return false;
        }
    }

    /**
     * Enforces animation restrictions based on license. Forces HORIZONTAL
     * animation for non-premium users.
     */
    public void enforceAnimationRestrictions() {
        if (id.startsWith("example")) {
            return; // Don't enforce restrictions on example lootboxes
        }

        // Only fallback to HORIZONTAL if the requested animation is not available
        if (!FeatureManager.canUseAnimation(animationType.name()) && animationType != AnimationType.HORIZONTAL) {
            this.animationType = AnimationType.HORIZONTAL;
            this.modified = true;
        }
    }

    /**
     * Enforces item restrictions based on license. Removes Oraxen items and
     * command actions for non-premium users.
     */
    public void enforceItemRestrictions() {
        if (FeatureManager.canUseOraxenItems() || FeatureManager.canExecuteCommands()) {
            items.values().removeIf(item -> {
                boolean isOraxenItem = item instanceof OraxenLootboxItem;
                boolean hasCommandAction = item.getAction() != null;
                return isOraxenItem || hasCommandAction;
            });
            modified = true;
        }
    }

    /**
     * Removes a spawn location.
     *
     * @param location Location to remove
     */
    public void removeLocation(Location location) {
        // Remove the exact location if it exists
        locations.remove(location);

        // If not found, try to find a location that matches coordinates
        locations.removeIf(loc
                -> loc.getWorld().equals(location.getWorld())
                && Math.abs(loc.getX() - location.getX()) < 0.1
                && // Small delta for X and Z
                Math.abs(loc.getY() - location.getY()) < 0.5
                && // Medium delta for Y to account for small animations
                Math.abs(loc.getZ() - location.getZ()) < 0.1
        );

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
}
