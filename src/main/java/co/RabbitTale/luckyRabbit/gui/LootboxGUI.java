package co.RabbitTale.luckyRabbit.gui;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/*
 * LootboxGUI.java
 *
 * Abstract base class for lootbox-related GUIs.
 * Provides common functionality for lootbox interfaces.
 *
 * Features:
 * - Plugin instance access
 * - Inventory management
 * - State tracking for animations
 */
public abstract class LootboxGUI implements InventoryHolder {
    protected final LuckyRabbit plugin;
    protected Inventory inventory;
    protected boolean isFinished = false;
    protected boolean isProcessingReward = false;

    /**
     * Creates a new lootbox GUI.
     *
     * @param plugin Plugin instance
     * @param inventory Initial inventory
     */
    public LootboxGUI(LuckyRabbit plugin, Inventory inventory) {
        this.plugin = plugin;
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
