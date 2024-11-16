package co.RabbitTale.luckyRabbit.gui;

import co.RabbitTale.luckyRabbit.gui.utils.GUIUtils;
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

import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.ERROR_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.ITEM_COLOR;

/*
 * LootboxDeleteConfirmationGUI.java
 *
 * Confirmation GUI for deleting lootboxes.
 * Provides a safe way to confirm lootbox deletion.
 *
 * Features:
 * - Visual confirmation interface
 * - Lootbox preview
 * - Confirm/Cancel buttons
 * - Admin-only access
 *
 * Layout:
 * - Black glass pane border
 * - Center: Lootbox preview
 * - Left: Confirm button (green)
 * - Right: Cancel button (red)
 */
public class LootboxDeleteConfirmationGUI implements GUI {
    private final Inventory inventory;
    private final Player player;
    private final Lootbox lootbox;

    /**
     * Creates a new deletion confirmation GUI.
     *
     * @param player Player viewing the GUI
     * @param lootbox Lootbox to be deleted
     */
    public LootboxDeleteConfirmationGUI(Player player, Lootbox lootbox) {
        this.player = player;
        this.lootbox = lootbox;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm Delete Lootbox"));
        setupInventory();
    }

    /**
     * Sets up the inventory with buttons and preview.
     */
    private void setupInventory() {
        // Fill with black glass
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Add lootbox representation in the middle
        ItemStack lootboxItem = new ItemStack(Material.CHEST);
        ItemMeta meta = lootboxItem.getItemMeta();
        meta.displayName(Component.text("Delete: ")
            .color(ERROR_COLOR)
            .append(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))
            .decoration(TextDecoration.ITALIC, false));
        lootboxItem.setItemMeta(meta);
        inventory.setItem(13, lootboxItem);

        // Add confirmation buttons
        GUIUtils.setupConfirmationButtons(inventory);
    }

    /**
     * Handles inventory click events.
     * Processes confirmation or cancellation.
     *
     * @param event The click event
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        if (GUIUtils.isConfirmButton(event.getSlot())) {
            LuckyRabbit.getInstance().getLootboxManager().deleteLootbox(lootbox.getId());
            player.sendMessage(Component.text("Lootbox deleted successfully!")
                .color(ITEM_COLOR));
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        } else if (GUIUtils.isCancelButton(event.getSlot())) {
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        }
    }

    /**
     * Shows the GUI to a player.
     * Opens the inventory for viewing.
     */
    public void show() {
        player.openInventory(inventory);
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Nothing to clean up
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
