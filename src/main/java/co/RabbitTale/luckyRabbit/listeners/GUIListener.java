package co.RabbitTale.luckyRabbit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import co.RabbitTale.luckyRabbit.gui.GUI;
import co.RabbitTale.luckyRabbit.gui.animations.BaseAnimationGUI;

public class GUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();

        // Check both clicked and top inventory for animation GUI
        if (clickedInv != null && clickedInv.getHolder() instanceof BaseAnimationGUI || topInv.getHolder() instanceof BaseAnimationGUI) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            player.updateInventory();
            // Block all inventory actions
            event.getView().setCursor(null);
            return;
        }

        if (event.getInventory().getHolder() instanceof GUI gui) {
            gui.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();

        if (topInv.getHolder() instanceof BaseAnimationGUI) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.updateInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof GUI) {
            ((GUI) holder).handleClose(event);
        }
    }
}
