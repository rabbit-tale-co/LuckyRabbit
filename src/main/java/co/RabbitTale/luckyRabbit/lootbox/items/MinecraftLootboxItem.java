package co.RabbitTale.luckyRabbit.lootbox.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.lootbox.rewards.RewardAction;

public class MinecraftLootboxItem extends LootboxItem {

    public MinecraftLootboxItem(ItemStack item) {
        super(item, generateId(item), 100.0, "COMMON", null, null);
    }

    public MinecraftLootboxItem(ItemStack item, String id, double chance, String rarity, RewardAction action, ConfigurationSection originalConfig) {
        super(item, id, chance, rarity, action, originalConfig);
    }

    private static String generateId(ItemStack item) {
        return item.getType().name().toLowerCase() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    protected void saveSpecific(ConfigurationSection config) {
        // Save only Minecraft-specific properties
        config.set("item.type", getItem().getType().name());
        config.set("item.amount", getItem().getAmount());
    }
}
