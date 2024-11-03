package co.RabbitTale.luckyRabbitFoot.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.items.LootboxItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ItemDeleteConfirmationGUI implements GUI {
    private final Inventory inventory;
    private final Player player;
    private final Lootbox lootbox;
    private final LootboxItem item;

    public ItemDeleteConfirmationGUI(Player player, Lootbox lootbox, LootboxItem item) {
        this.player = player;
        this.lootbox = lootbox;
        this.item = item;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm Delete Item"));
        setupInventory();
    }

    private void setupInventory() {
        // Fill with black glass
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Add item to delete in the middle
        inventory.setItem(13, item.getItem());

        // Add confirm button (green wool)
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm Delete")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false));
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(11, confirm);

        // Add cancel button (red wool)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel")
            .color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(15, cancel);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        if (event.getSlot() == 11) { // Confirm
            lootbox.removeItem(item.getItem());
            LuckyRabbitFoot.getInstance().getLootboxManager().saveLootbox(lootbox);
            player.sendMessage(Component.text("Item removed successfully!")
                .color(NamedTextColor.GREEN));
            player.closeInventory();
            new LootboxContentGUI(player, lootbox).show();
        } else if (event.getSlot() == 15) { // Cancel
            player.closeInventory();
            new LootboxContentGUI(player, lootbox).show();
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Nothing to clean up
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void show() {
        player.openInventory(inventory);
    }
}
