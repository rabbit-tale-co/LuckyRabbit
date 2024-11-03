package co.RabbitTale.luckyRabbitFoot.lootbox.rewards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbitFoot.lootbox.items.LootboxItem;

public record Reward(ItemStack displayItem, double chance, RewardRarity rarity, LootboxItem lootboxItem) {

    public Reward(ItemStack displayItem, double chance, RewardRarity rarity) {
        this(displayItem, chance, rarity, null);
    }

    /**
     * Get a copy of the display item
     * @return Cloned ItemStack
     */
    public ItemStack getDisplayItem() {
        return displayItem.clone();
    }

    /**
     * Get the chance of this reward
     * @return Chance value (0-100)
     */
    public double chance() {
        return chance;
    }

    /**
     * Get the rarity of this reward
     * @return RewardRarity enum value
     */
    public RewardRarity rarity() {
        return rarity;
    }

    /**
     * Give this reward to a player
     * @param player Player to receive the reward
     */
    public void give(Player player) {
        if (lootboxItem != null) {
            lootboxItem.give(player);
        } else {
            player.getInventory().addItem(displayItem.clone());
        }
    }

    /**
     * Get a copy of the display item
     * This is an alias for getDisplayItem() to maintain compatibility
     * @return Cloned ItemStack
     */
    public ItemStack displayItem() {
        return getDisplayItem();
    }
}
