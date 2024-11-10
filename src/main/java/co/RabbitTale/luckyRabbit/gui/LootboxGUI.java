package co.RabbitTale.luckyRabbit.gui;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class LootboxGUI implements InventoryHolder {
    protected final LuckyRabbit plugin;
    protected Inventory inventory;
    protected boolean isFinished = false;
    protected boolean isProcessingReward = false;

    public LootboxGUI(LuckyRabbit plugin, Inventory inventory) {
        this.plugin = plugin;
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
