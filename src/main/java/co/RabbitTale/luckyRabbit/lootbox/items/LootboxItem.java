package co.RabbitTale.luckyRabbit.lootbox.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.RabbitTale.luckyRabbit.lootbox.rewards.RewardAction;
import co.RabbitTale.luckyRabbit.lootbox.rewards.RewardRarity;
import co.RabbitTale.luckyRabbit.utils.Logger;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

@Getter
public abstract class LootboxItem {

    private final ItemStack item;
    private final String id;
    private final double chance;
    private final String rarity;
    private final RewardAction action;
    private final ConfigurationSection originalConfig;
    /**
     * Checks if this item's chance was manually set. Used for automatic chance
     * recalculation.
     */
    private boolean isChanceManuallySet;

    /**
     * Creates a new lootbox item.
     *
     * @param item ItemStack to give
     * @param id Unique identifier
     * @param chance Drop chance percentage
     * @param rarity Item rarity level
     * @param action Action to execute on win
     * @param originalConfig Original config section for saving
     */
    public LootboxItem(ItemStack item, String id, double chance, String rarity, RewardAction action, ConfigurationSection originalConfig) {
        this.item = item;
        this.id = id;
        this.chance = chance;
        this.rarity = rarity;
        this.action = action;
        this.originalConfig = originalConfig;
    }

    /**
     * Gets the ItemStack associated with this lootbox item.
     *
     * @return The item
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Gets the unique identifier for this item.
     *
     * @return The item ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the drop chance of this item.
     *
     * @return The chance as a percentage
     */
    public double getChance() {
        return chance;
    }

    /**
     * Gets the rarity level of this item.
     *
     * @return The rarity as a string
     */
    public String getRarity() {
        return rarity;
    }

    /**
     * Gets the action to execute when this item is won.
     *
     * @return The reward action
     */
    public RewardAction getAction() {
        return action;
    }

    /**
     * Gets the original configuration section this item was loaded from.
     *
     * @return The config section
     */
    public ConfigurationSection getOriginalConfig() {
        return originalConfig;
    }

    /**
     * Checks if this item's chance was manually set.
     *
     * @return true if the chance was manually set, false otherwise
     */
    public boolean isChanceManuallySet() {
        return isChanceManuallySet;
    }

    /**
     * Sets whether this item's chance was manually set.
     *
     * @param isChanceManuallySet true if the chance was manually set
     */
    public void setChanceManuallySet(boolean isChanceManuallySet) {
        this.isChanceManuallySet = isChanceManuallySet;
    }

    /**
     * Creates a LootboxItem from a configuration section. Handles both
     * Minecraft and Oraxen items.
     *
     * @param section Configuration section to load from
     * @return New LootboxItem instance
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static LootboxItem fromConfig(ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        String id = section.getName();
        double chance = section.getDouble("chance", 100.0);
        String rarity = section.getString("rarity", "COMMON");
        RewardAction action = RewardAction.fromConfig(section.getConfigurationSection("action"));

        // Check for Oraxen item first
        String oraxenId = section.getString("oraxen_item");
        if (oraxenId != null) {
            try {
                // Try to load Oraxen item
                ItemBuilder itemBuilder = OraxenItems.getItemById(oraxenId);
                if (itemBuilder != null) {
                    ItemStack oraxenItem = itemBuilder.build();

                    // Apply any additional meta from config
                    ConfigurationSection itemSection = section.getConfigurationSection("item");
                    if (itemSection != null) {
                        applyItemMeta(oraxenItem, itemSection);
                    }

                    return new OraxenLootboxItem(oraxenItem, oraxenId, id, chance, rarity, section);
                } else {
                    Logger.error("Failed to load Oraxen item: " + oraxenId);
                }
            } catch (NoClassDefFoundError e) {
                Logger.error("Oraxen plugin is not installed or not properly loaded. Skipping Oraxen item: " + oraxenId);
            }
        }

        // If not Oraxen, create as Minecraft item
        ConfigurationSection itemSection = section.getConfigurationSection("item");
        if (itemSection == null) {
            Logger.warning("Missing item section in config for item: " + id);
            // Create a fallback item instead of throwing an error
            ItemStack fallbackItem = new ItemStack(org.bukkit.Material.STONE);
            ItemMeta meta = fallbackItem.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Invalid Item: " + id)
                        .color(NamedTextColor.RED));
                fallbackItem.setItemMeta(meta);
            }
            return new MinecraftLootboxItem(fallbackItem, id, chance, rarity, action, section);
        }

        // Get item properties
        String materialName = itemSection.getString("type", "STONE");
        int amount = itemSection.getInt("amount", 1);

        // Create the item
        ItemStack item = new ItemStack(org.bukkit.Material.valueOf(materialName.toUpperCase()), amount);

        // Apply metadata from config
        applyItemMeta(item, itemSection);

        return new MinecraftLootboxItem(item, id, chance, rarity, action, section);
    }

    /**
     * Applies metadata to an ItemStack from configuration. Handles display name
     * and lore with MiniMessage formatting.
     *
     * @param item ItemStack to modify
     * @param itemSection Configuration section containing metadata
     */
    protected static void applyItemMeta(ItemStack item, ConfigurationSection itemSection) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // Apply metadata from config
        if (itemSection.contains("meta")) {
            ConfigurationSection metaSection = itemSection.getConfigurationSection("meta");
            if (metaSection != null) {
                // Set display name with MiniMessage formatting
                if (metaSection.contains("displayName") || metaSection.contains("display-name")) {
                    String displayName = metaSection.getString("displayName");
                    if (displayName == null) {
                        displayName = metaSection.getString("display-name");
                    }
                    if (displayName != null) {
                        Component nameComponent = MiniMessage.miniMessage().deserialize(displayName)
                                .decoration(TextDecoration.ITALIC, false);
                        meta.displayName(nameComponent);
                    }
                }

                // Set lore with MiniMessage formatting
                if (metaSection.contains("lore")) {
                    List<String> configLore = metaSection.getStringList("lore");
                    List<Component> lore = configLore.stream()
                            .map(line -> MiniMessage.miniMessage().deserialize(line)
                            .decoration(TextDecoration.ITALIC, false))
                            .collect(Collectors.toList());
                    meta.lore(lore);
                }
            }
        }

        item.setItemMeta(meta);
    }

    /**
     * Gets a display version of this item. Includes rarity and chance
     * information in lore.
     *
     * @return ItemStack configured for display
     */
    public ItemStack getDisplayItem() {
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();

            // Preserve original lore if it exists (especially for Oraxen items)
            if (meta.hasLore()) {
                lore.addAll(Objects.requireNonNull(meta.lore()));
            }

            // Add rarity and chance information
            lore.add(Component.empty());
            lore.add(Component.text("Rarity: ")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(rarity)
                            .color(RewardRarity.valueOf(rarity.toUpperCase()).getColor())
                            .decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text(String.format("Chance: %.1f%%", chance))
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    /**
     * Saves this item to a configuration section.
     *
     * @param section Configuration section to save to
     */
    public void saveToConfig(ConfigurationSection section) {
        // Save basic properties
        section.set("id", id);
        section.set("chance", chance);
        section.set("rarity", rarity);

        // Save item data
        ConfigurationSection itemSection = section.createSection("item");
        itemSection.set("type", item.getType().name());
        itemSection.set("amount", item.getAmount());

        // Save item meta
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                itemSection.set("displayName", MiniMessage.miniMessage().serialize(meta.displayName()));
            }
            if (meta.hasLore()) {
                List<String> serializedLore = meta.lore().stream()
                        .map(component -> MiniMessage.miniMessage().serialize(component))
                        .collect(Collectors.toList());
                itemSection.set("lore", serializedLore);
            }
        }

        // Save action
        if (action != null) {
            ConfigurationSection actionSection = section.createSection("action");
            action.save(actionSection);
        }

        // Save specific implementation details
        saveSpecific(section);
    }

    /**
     * Saves item-specific data to configuration. Implemented by subclasses for
     * their unique properties.
     *
     * @param config Configuration section to save to
     */
    protected abstract void saveSpecific(ConfigurationSection config);
}
