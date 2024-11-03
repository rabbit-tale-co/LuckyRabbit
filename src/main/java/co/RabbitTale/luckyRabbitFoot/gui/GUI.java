package co.RabbitTale.luckyRabbitFoot.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface GUI extends InventoryHolder {
    void handleClick(InventoryClickEvent event);
    void handleClose(InventoryCloseEvent event);
    Inventory getInventory();
}
