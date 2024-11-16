package co.RabbitTale.luckyRabbit.lootbox.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.lootbox.rewards.RewardAction;

/*
 * MinecraftLootboxItem.java
 *
 * Implementation of LootboxItem for vanilla Minecraft items.
 * Handles standard ItemStack storage and serialization.
 *
 * Configuration Structure:
 * item:
 *   type: DIAMOND_SWORD
 *   amount: 1
 *   meta:
 *     display-name: "<red>Special Sword"
 *     lore:
 *       - "<gray>A rare sword"
 */
public class MinecraftLootboxItem extends LootboxItem {

    /**
     * Creates a new Minecraft item with default values.
     *
     * @param item ItemStack to use
     */
    public MinecraftLootboxItem(ItemStack item) {
        super(item, generateId(item), 100.0, "COMMON", null, null);
    }

    /**
     * Creates a new Minecraft item with specified properties.
     *
     * @param item ItemStack to use
     * @param id Unique identifier
     * @param chance Drop chance percentage
     * @param rarity Item rarity level
     * @param action Action to execute on win
     * @param originalConfig Original config section for saving
     */
    public MinecraftLootboxItem(ItemStack item, String id, double chance, String rarity, RewardAction action, ConfigurationSection originalConfig) {
        super(item, id, chance, rarity, action, originalConfig);
    }

    /**
     * Generates a unique ID for this item. Format: material_name-random_uuid
     *
     * @param item ItemStack to generate ID for
     * @return Generated ID string
     */
    private static String generateId(ItemStack item) {
        return item.getType().name().toLowerCase() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Saves Minecraft-specific properties to configuration.
     *
     * @param config Configuration section to save to
     */
    @Override
    protected void saveSpecific(ConfigurationSection config) {
        config.set("item.type", getItem().getType().name());
        config.set("item.amount", getItem().getAmount());
    }
}
