package co.RabbitTale.luckyRabbit.gui.utils;

import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.*;

/*
 * GUIUtils.java
 *
 * Utility class for common GUI operations.
 * Provides reusable methods for inventory setup and button creation.
 *
 * Features:
 * - Border creation for inventories
 * - Navigation button generation
 * - Consistent styling across GUIs
 * - Confirmation buttons creation
 *
 * Layout Helpers:
 * - Border: Gray stained glass panes
 * - Navigation: Arrows with enabled/disabled states
 * - Buttons: Customizable text and colors
 * - Confirmation: Standard confirm/cancel buttons
 */
public class GUIUtils {

    /**
     * Sets up a border around an inventory. Creates a frame using glass panes.
     *
     * @param inventory The inventory to add border to
     * @param rows Number of rows in the inventory
     */
    public static void setupBorder(Inventory inventory, int rows) {
        inventory.clear();

        // Add glass pane border
        for (int i = 0; i < rows * 9; i++) {
            // First and last row
            if (i < 9 || i >= (rows - 1) * 9) {
                inventory.setItem(i, createBorderItem());
            } // Side borders
            else if (i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, createBorderItem());
            }
        }
    }

    /**
     * Creates a border item (gray glass pane). Used for inventory borders.
     *
     * @return ItemStack configured for border
     */
    public static ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a navigation button with state. Used for prev/next page buttons
     * and similar.
     *
     * @param name Button display name
     * @param material Button material
     * @param enabled Whether button is enabled
     * @return ItemStack configured as button
     */
    public static ItemStack createNavigationButton(String name, Material material, boolean enabled) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (enabled) {
            meta.displayName(Component.text(name)
                    .color(ITEM_COLOR)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(Component.text(name)
                    .color(DESCRIPTION_COLOR)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Not available")
                    .color(ERROR_COLOR)
                    .decoration(TextDecoration.ITALIC, false)));
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a confirmation button set (confirm and cancel). Used in
     * confirmation GUIs.
     *
     * @param inventory The inventory to add buttons to
     */
    public static void setupConfirmationButtons(Inventory inventory) {
        // Add confirm button (green wool)
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm Delete")
                .color(LootboxCommand.ITEM_COLOR)
                .decoration(TextDecoration.ITALIC, false));
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(11, confirm);

        // Add cancel button (red wool)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel")
                .color(LootboxCommand.ERROR_COLOR)
                .decoration(TextDecoration.ITALIC, false));
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(15, cancel);
    }

    /**
     * Checks if a slot is the confirm button.
     *
     * @param slot The slot to check
     * @return true if confirm button
     */
    public static boolean isConfirmButton(int slot) {
        return slot == 11;
    }

    /**
     * Checks if a slot is the cancel button.
     *
     * @param slot The slot to check
     * @return true if cancel button
     */
    public static boolean isCancelButton(int slot) {
        return slot == 15;
    }
}
