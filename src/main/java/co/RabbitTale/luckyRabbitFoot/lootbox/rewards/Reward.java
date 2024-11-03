package co.RabbitTale.luckyRabbitFoot.lootbox.rewards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record Reward(
    ItemStack item,
    double chance,
    RewardRarity rarity,
    RewardAction action
) {
    public Reward(ItemStack item, double chance, RewardRarity rarity) {
        this(item, chance, rarity, null);
    }

    public void give(Player player) {
        if (action != null) {
            action.execute(player);
        } else {
            player.getInventory().addItem(item.clone());
        }
    }

    public ItemStack displayItem() {
        return item.clone();
    }
}
