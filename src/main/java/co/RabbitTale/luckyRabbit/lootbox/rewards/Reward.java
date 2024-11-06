package co.RabbitTale.luckyRabbit.lootbox.rewards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;

public record Reward(LootboxItem item, double chance, RewardRarity rarity, RewardAction action) {

    public void give(Player player) {
        if (action != null) {
            action.execute(player);
        } else {
            // If no action is defined, give the physical item
            player.getInventory().addItem(item.getDisplayItem());
        }
    }

    public ItemStack displayItem() {
        return item.getDisplayItem();
    }
}
