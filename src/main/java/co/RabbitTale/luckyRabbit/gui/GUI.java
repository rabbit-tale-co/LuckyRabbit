package co.RabbitTale.luckyRabbit.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/*
 * GUI.java
 *
 * Base interface for all GUI menus in the plugin.
 * Defines core functionality required for inventory handling.
 *
 * Features:
 * - Click event handling
 * - Close event handling
 * - Inventory management
 */
public interface GUI extends InventoryHolder {
    /**
     * Handles inventory click events.
     * Called when a player clicks in the GUI.
     *
     * @param event The click event
     */
    void handleClick(InventoryClickEvent event);

    /**
     * Handles inventory close events.
     * Called when a player closes the GUI.
     *
     * @param event The close event
     */
    void handleClose(InventoryCloseEvent event);

    /**
     * Gets the inventory associated with this GUI.
     * Required by InventoryHolder interface.
     *
     * @return The GUI's inventory
     */
    @NotNull Inventory getInventory();
}
