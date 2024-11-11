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
    private final List<String> lore;
    private final Map<String, LootboxItem> items;
    private final List<Location> locations;
    private AnimationType animationType;
    private int openCount;
    private boolean modified = false;

    public Lootbox(String id, String displayName, AnimationType animationType) {
        this.id = id;
        this.displayName = displayName;
        this.lore = new ArrayList<>();
        this.items = new HashMap<>();
        this.locations = new ArrayList<>();
        this.animationType = animationType;
        this.openCount = 0;
    }

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

    public void addItem(LootboxItem item) {
        items.put(item.getId(), item);
        modified = true;
    }

    public void removeItem(ItemStack item) {
        items.values().removeIf(lootboxItem -> lootboxItem.getItem().isSimilar(item));
        modified = true;
    }

    public void addLocation(Location location) {
        locations.add(location);
        modified = true;
    }

    public void incrementOpenCount() {
        this.openCount++;
        setModified();
    }

    public boolean hasBeenModified() {
        return modified;
    }

    public void setModified() {
        this.modified = true;
    }

    /**
     * Sets the animation type for this lootbox
     * @param animationType The new animation type
     */
    public void setAnimationType(AnimationType animationType) {
        this.animationType = animationType;
        this.modified = true;
    }

    public void enforceAnimationRestrictions() {
        if (id.startsWith("example")) {
            return;
        }

        if (FeatureManager.canUseAnimation(animationType.name())) {
            this.animationType = AnimationType.HORIZONTAL;
        }
    }

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

    public void removeLocation(Location location) {
        if (locations == null) return;

        // Remove the exact location if it exists
        locations.remove(location);

        // If not found, try to find a location that matches coordinates
        locations.removeIf(loc ->
            loc.getWorld().equals(location.getWorld()) &&
            Math.abs(loc.getX() - location.getX()) < 0.1 && // Small delta for X and Z
            Math.abs(loc.getY() - location.getY()) < 0.5 && // Medium delta for Y to account for small animations
            Math.abs(loc.getZ() - location.getZ()) < 0.1
        );

        // Mark as modified so it gets saved
        modified = true;
    }

}
