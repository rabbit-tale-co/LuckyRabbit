package co.RabbitTale.luckyRabbit.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public class LootboxDeleteConfirmationGUI implements GUI {
    private final Inventory inventory;
    private final Player player;
    private final Lootbox lootbox;

    public LootboxDeleteConfirmationGUI(Player player, Lootbox lootbox) {
        this.player = player;
        this.lootbox = lootbox;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm Delete Lootbox"));
        setupInventory();
    }

    private void setupInventory() {
        // Fill with black glass
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Add lootbox representation in the middle
        ItemStack lootboxItem = new ItemStack(Material.CHEST);
        ItemMeta meta = lootboxItem.getItemMeta();
        meta.displayName(Component.text("Delete: ")
            .color(NamedTextColor.RED)
            .append(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))
            .decoration(TextDecoration.ITALIC, false));
        lootboxItem.setItemMeta(meta);
        inventory.setItem(13, lootboxItem);

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
            LuckyRabbit.getInstance().getLootboxManager().deleteLootbox(lootbox.getId());
            player.sendMessage(Component.text("Lootbox deleted successfully!")
                .color(NamedTextColor.GREEN));
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        } else if (event.getSlot() == 15) { // Cancel
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Nothing to clean up
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void show() {
        player.openInventory(inventory);
    }
}
