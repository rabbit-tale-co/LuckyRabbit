package co.RabbitTale.luckyRabbitFoot.lootbox;

import co.RabbitTale.luckyRabbitFoot.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbitFoot.lootbox.items.LootboxItem;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Lootbox {
    @Getter private final String id;
    @Getter private final String displayName;
    @Getter private final List<String> lore;
    @Getter private final Map<String, LootboxItem> items;
    @Getter private final List<Location> locations;
    @Getter private final AnimationType animationType;
    @Getter private int openCount;
    @Getter private boolean modified = false;

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

    public void removeLocation(Location location) {
        locations.removeIf(loc -> loc.equals(location));
    }

    public void incrementOpenCount() {
        this.openCount++;
        setModified();
    }

    public void save(FileConfiguration config) {
        config.set("id", id);
        config.set("displayName", displayName);
        config.set("lore", lore);
        config.set("animationType", animationType.name());
        config.set("openedCount", openCount);

        // Save items
        ConfigurationSection itemsSection = config.createSection("items");
        int index = 0;
        for (LootboxItem item : items.values()) {
            ConfigurationSection itemSection = itemsSection.createSection(String.valueOf(index++));
            item.save(itemSection);
        }

        // Save locations
        ConfigurationSection locationsSection = config.createSection("locations");
        int locIndex = 0;
        for (Location location : locations) {
            locIndex = LootboxManager.getIndex(locIndex, locationsSection, location);
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("displayName", displayName);
        data.put("lore", lore);
        data.put("animationType", animationType.name());
        data.put("openedCount", openCount);

        // Serialize items
        Map<String, Object> itemsData = new HashMap<>();
        items.forEach((itemId, item) -> itemsData.put(itemId, item.serialize()));
        data.put("items", itemsData);

        // Serialize locations
        List<Map<String, Object>> locationsData = new ArrayList<>();
        locations.forEach(location -> locationsData.add(location.serialize()));
        data.put("locations", locationsData);

        return data;
    }

    public boolean hasBeenModified() {
        return modified;
    }

    public void setModified() {
        this.modified = true;
    }
}
