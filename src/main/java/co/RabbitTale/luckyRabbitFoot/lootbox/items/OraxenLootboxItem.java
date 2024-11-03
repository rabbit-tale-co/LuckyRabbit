package co.RabbitTale.luckyRabbitFoot.lootbox.items;

import org.bukkit.inventory.ItemStack;

import lombok.Getter;

public class OraxenLootboxItem extends LootboxItem {
    @Getter private final String oraxenId;

    public OraxenLootboxItem(ItemStack item, String oraxenId) {
        super(item);
        this.oraxenId = oraxenId;
    }

    public OraxenLootboxItem(ItemStack item, String oraxenId, String id, double chance, String rarity) {
        super(item, id, chance, rarity);
        this.oraxenId = oraxenId;
    }

    @Override
    public void save(org.bukkit.configuration.ConfigurationSection config) {
        config.set("id", getId());
        config.set("chance", getChance());
        config.set("rarity", getRarity());
        config.set("oraxen_item", oraxenId);
    }
}
