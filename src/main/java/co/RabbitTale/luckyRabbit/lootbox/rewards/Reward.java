package co.RabbitTale.luckyRabbit.lootbox.rewards;

import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;

/*
 * Reward.java
 *
 * Record class representing a lootbox reward.
 * Combines item data with chance, rarity, and action information.
 *
 * Structure:
 * - LootboxItem: The item to be given as a reward
 * - chance: Drop chance percentage
 * - rarity: Rarity level (affects display and animations)
 * - action: Optional action to execute when reward is given
 */
public record Reward(LootboxItem item, double chance, RewardRarity rarity, RewardAction action) {

    /**
     * Gets the display item for this reward.
     * Used in GUIs and preview menus.
     *
     * @return ItemStack configured for display
     */
    public ItemStack displayItem() {
        return item.getDisplayItem();
    }
}
