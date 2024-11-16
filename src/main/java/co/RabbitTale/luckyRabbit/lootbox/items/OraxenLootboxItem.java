package co.RabbitTale.luckyRabbit.lootbox.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;

@Getter
public class OraxenLootboxItem extends LootboxItem {

    private final String oraxenId;

    public OraxenLootboxItem(ItemStack item, String oraxenId) {
        super(item, generateId(oraxenId), 100.0, "COMMON", null, null);
        this.oraxenId = oraxenId;
    }

    public OraxenLootboxItem(ItemStack item, String oraxenId, String id, double chance, String rarity, ConfigurationSection originalConfig) {
        super(item, id, chance, rarity, null, originalConfig);
        this.oraxenId = oraxenId;
    }

    private static String generateId(String oraxenId) {
        return "oraxen-" + oraxenId + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    protected void saveSpecific(ConfigurationSection config) {
        // Save only Oraxen-specific properties
        config.set("oraxen_item", oraxenId);
        config.set("item.amount", getItem().getAmount());
    }
}
