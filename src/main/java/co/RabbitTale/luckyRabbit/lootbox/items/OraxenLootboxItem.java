package co.RabbitTale.luckyRabbit.lootbox.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;

public class OraxenLootboxItem extends LootboxItem {

    @Getter
    private final String oraxenId;

    /**
     * Creates a new Oraxen lootbox item.
     *
     * @param item ItemStack from Oraxen
     * @param oraxenId Oraxen item ID
     * @param id Unique identifier for this reward
     * @param chance Drop chance percentage
     * @param rarity Item rarity level
     * @param originalConfig Original config section
     */
    public OraxenLootboxItem(ItemStack item, String oraxenId, String id, double chance, String rarity,
            ConfigurationSection originalConfig) {
        super(item, id, chance, rarity, null, originalConfig);
        this.oraxenId = oraxenId;
    }

    /**
     * Gets the Oraxen item ID.
     *
     * @return The Oraxen item ID
     */
    public String getOraxenId() {
        return oraxenId;
    }

    @Override
    protected void saveSpecific(ConfigurationSection config) {
        // Save only Oraxen-specific properties
        config.set("oraxen_item", oraxenId);
        config.set("item.amount", getItem().getAmount());
    }
}
