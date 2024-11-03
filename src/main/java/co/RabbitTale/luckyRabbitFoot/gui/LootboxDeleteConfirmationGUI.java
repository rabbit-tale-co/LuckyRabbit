package co.RabbitTale.luckyRabbitFoot.gui;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class LootboxDeleteConfirmationGUI implements GUI {
    private static final int GUI_SIZE = 27;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final LuckyRabbitFoot plugin;
    private final Player player;
    private final Lootbox lootbox;
    private final Inventory inventory;

    public LootboxDeleteConfirmationGUI(Player player, Lootbox lootbox) {
        this.plugin = LuckyRabbitFoot.getInstance();
        this.player = player;
        this.lootbox = lootbox;
        this.inventory = createInventory();
        fillInventory();
    }

    private Inventory createInventory() {
        return Bukkit.createInventory(null, GUI_SIZE,
            Component.text("Confirm Delete: " + lootbox.getDisplayName())
                .color(NamedTextColor.RED));
    }

    public void show() {
        player.openInventory(inventory);
    }

    private void fillInventory() {
        // Create confirm button
        ItemStack confirmItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm Delete")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(Arrays.asList(
            Component.text("Click to permanently delete")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("this lootbox and all its contents")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));
        confirmItem.setItemMeta(confirmMeta);

        // Create cancel button
        ItemStack cancelItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel")
            .color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        cancelMeta.lore(Arrays.asList(
            Component.text("Click to cancel deletion")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        ));
        cancelItem.setItemMeta(cancelMeta);

        // Place buttons
        inventory.setItem(CONFIRM_SLOT, confirmItem);
        inventory.setItem(CANCEL_SLOT, cancelItem);

        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < GUI_SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        if (event.getSlot() == CONFIRM_SLOT) {
            plugin.getLootboxManager().deleteLootbox(lootbox.getId());
            player.sendMessage(Component.text("Successfully deleted lootbox: " + lootbox.getDisplayName())
                .color(NamedTextColor.GREEN));
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        } else if (event.getSlot() == CANCEL_SLOT) {
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Cleanup if needed
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
