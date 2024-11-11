package co.RabbitTale.luckyRabbit.lootbox.rewards;

import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;

public record Reward(LootboxItem item, double chance, RewardRarity rarity, RewardAction action) {

    public ItemStack displayItem() {
        return item.getDisplayItem();
    }
}
