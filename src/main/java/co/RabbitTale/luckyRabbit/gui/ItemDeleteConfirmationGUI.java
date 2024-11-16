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
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbit.gui.utils.GUIUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.ERROR_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.ITEM_COLOR;

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

        // Add confirmation buttons
        GUIUtils.setupConfirmationButtons(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        if (GUIUtils.isConfirmButton(event.getSlot())) {
            lootbox.removeItem(item.getItem());
            LuckyRabbit.getInstance().getLootboxManager().saveLootbox(lootbox);
            player.sendMessage(Component.text("Item removed successfully!")
                .color(ITEM_COLOR));
            player.closeInventory();
            new LootboxContentGUI(player, lootbox).show();
        } else if (GUIUtils.isCancelButton(event.getSlot())) {
            player.closeInventory();
            new LootboxContentGUI(player, lootbox).show();
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
