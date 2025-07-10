package co.RabbitTale.luckyRabbit.gui;

import java.util.ArrayList;
import java.util.List;

import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import co.RabbitTale.luckyRabbit.gui.utils.GUIUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * LootboxFilterGUI.java
 *
 * GUI for filtering and sorting lootboxes. Allows players to change display
 * order and filter criteria.
 */
public class LootboxFilterGUI implements GUI {

    private static final int ROWS = 4;

    // Sort buttons (top row)
    private static final int SORT_NAME_ASC_SLOT = 10; // A-Z
    private static final int SORT_NAME_DESC_SLOT = 11; // Z-A
    private static final int SORT_CREATION_SLOT = 12; // Creation time
    private static final int SORT_POPULARITY_SLOT = 13; // Popularity
    private static final int SORT_ITEMS_SLOT = 14; // Item count

    // Filter buttons (middle row) - only for admins
    // private static final int FILTER_ALL_SLOT = 19; // All types
    // private static final int FILTER_EXAMPLES_SLOT = 20; // Examples only
    // private static final int FILTER_NORMAL_SLOT = 21; // Normal only
    // Control buttons (bottom row)
    private static final int BACK_BUTTON_SLOT = 27; // Left
    private static final int RESET_BUTTON_SLOT = 31; // Center
    private static final int CLOSE_BUTTON_SLOT = 35; // Right

    private final Player player;
    private final LootboxListGUI parentGUI;
    private final Inventory inventory;

    /**
     * Creates a new filter GUI.
     *
     * @param player Player viewing the GUI
     * @param parentGUI Parent LootboxListGUI to return to
     */
    public LootboxFilterGUI(Player player, LootboxListGUI parentGUI) {
        this.player = player;
        this.parentGUI = parentGUI;
        this.inventory = Bukkit.createInventory(this, ROWS * 9,
                Component.text("Filter & Sort Lootboxes"));

        updateInventory();
    }

    /**
     * Updates the inventory contents.
     */
    private void updateInventory() {
        GUIUtils.setupBorder(inventory, ROWS);

        // Sort buttons (top row)
        inventory.setItem(SORT_NAME_ASC_SLOT, createSortButton(LootboxListGUI.SortType.NAME_ASC, Material.GREEN_STAINED_GLASS));
        inventory.setItem(SORT_NAME_DESC_SLOT, createSortButton(LootboxListGUI.SortType.NAME_DESC, Material.RED_STAINED_GLASS));
        inventory.setItem(SORT_CREATION_SLOT, createSortButton(LootboxListGUI.SortType.CREATION_TIME, Material.CLOCK));
        inventory.setItem(SORT_POPULARITY_SLOT, createSortButton(LootboxListGUI.SortType.POPULARITY, Material.GOLD_INGOT));
        inventory.setItem(SORT_ITEMS_SLOT, createSortButton(LootboxListGUI.SortType.ITEM_COUNT, Material.CHEST));

        // Filter buttons (middle row) - only for admins
        // if (player.hasPermission("luckyrabbit.admin")) {
        //     inventory.setItem(FILTER_ALL_SLOT, createFilterButton("All Types", Material.WHITE_STAINED_GLASS,
        //             !parentGUI.showExamplesOnly && !parentGUI.showNormalOnly));
        //     inventory.setItem(FILTER_EXAMPLES_SLOT, createFilterButton("Examples Only", Material.YELLOW_STAINED_GLASS,
        //             parentGUI.showExamplesOnly));
        //     inventory.setItem(FILTER_NORMAL_SLOT, createFilterButton("Normal Only", Material.BLUE_STAINED_GLASS,
        //             parentGUI.showNormalOnly));
        // }
        // Control buttons (bottom row)
        inventory.setItem(BACK_BUTTON_SLOT, GUIUtils.createNavigationButton("Back to List", Material.ARROW, true));
        inventory.setItem(RESET_BUTTON_SLOT, createResetButton());
        inventory.setItem(CLOSE_BUTTON_SLOT, GUIUtils.createNavigationButton("Close", Material.BARRIER, true));
    }

    /**
     * Creates a sort button.
     */
    private ItemStack createSortButton(LootboxListGUI.SortType sortType, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        boolean isSelected = parentGUI.currentSort == sortType;

        // Set display name
        meta.displayName(Component.text(sortType.getDisplayName())
                .color(isSelected ? LootboxCommand.SUCCESS_COLOR : NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, isSelected)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (isSelected) {
            lore.add(Component.text("✓ Currently selected")
                    .color(LootboxCommand.ITEM_COLOR)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Click to select")
                    .color(LootboxCommand.DESCRIPTION_COLOR)
                    .decoration(TextDecoration.ITALIC, true));
        }

        // Add description based on sort type
        lore.add(Component.empty());
        switch (sortType) {
            case NAME_ASC:
                lore.add(Component.text("Sort alphabetically A to Z")
                        .color(LootboxCommand.DESCRIPTION_COLOR)
                        .decoration(TextDecoration.ITALIC, true));
                break;
            case NAME_DESC:
                lore.add(Component.text("Sort alphabetically Z to A")
                        .color(LootboxCommand.DESCRIPTION_COLOR)
                        .decoration(TextDecoration.ITALIC, true));
                break;
            case CREATION_TIME:
                lore.add(Component.text("Sort by creation time")
                        .color(LootboxCommand.DESCRIPTION_COLOR)
                        .decoration(TextDecoration.ITALIC, true));
                break;
            case POPULARITY:
                lore.add(Component.text("Sort by open count")
                        .color(LootboxCommand.DESCRIPTION_COLOR)
                        .decoration(TextDecoration.ITALIC, true));
                break;
            case ITEM_COUNT:
                lore.add(Component.text("Sort by number of items")
                        .color(LootboxCommand.DESCRIPTION_COLOR)
                        .decoration(TextDecoration.ITALIC, true));
                break;
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a reset button.
     */
    private ItemStack createResetButton() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Reset to Default")
                .color(LootboxCommand.ERROR_COLOR)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Reset all filters and sorting")
                .color(LootboxCommand.DESCRIPTION_COLOR)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text("to default settings")
                .color(LootboxCommand.DESCRIPTION_COLOR)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);

        int slot = event.getRawSlot();

        // Sort buttons
        if (slot == SORT_NAME_ASC_SLOT) {
            parentGUI.currentSort = LootboxListGUI.SortType.NAME_ASC;
            updateParentAndRefresh();
        } else if (slot == SORT_NAME_DESC_SLOT) {
            parentGUI.currentSort = LootboxListGUI.SortType.NAME_DESC;
            updateParentAndRefresh();
        } else if (slot == SORT_CREATION_SLOT) {
            parentGUI.currentSort = LootboxListGUI.SortType.CREATION_TIME;
            updateParentAndRefresh();
        } else if (slot == SORT_POPULARITY_SLOT) {
            parentGUI.currentSort = LootboxListGUI.SortType.POPULARITY;
            updateParentAndRefresh();
        } else if (slot == SORT_ITEMS_SLOT) {
            parentGUI.currentSort = LootboxListGUI.SortType.ITEM_COUNT;
            updateParentAndRefresh();
        } // Filter buttons (only for admins)
        // else if (player.hasPermission("luckyrabbit.admin")) {
        //     if (slot == FILTER_ALL_SLOT) {
        //         parentGUI.showExamplesOnly = false;
        //         parentGUI.showNormalOnly = false;
        //         updateParentAndRefresh();
        //     } else if (slot == FILTER_EXAMPLES_SLOT) {
        //         parentGUI.showExamplesOnly = true;
        //         parentGUI.showNormalOnly = false;
        //         updateParentAndRefresh();
        //     } else if (slot == FILTER_NORMAL_SLOT) {
        //         parentGUI.showExamplesOnly = false;
        //         parentGUI.showNormalOnly = true;
        //         updateParentAndRefresh();
        //     }
        // }
        // Control buttons
        if (slot == BACK_BUTTON_SLOT) {
            player.closeInventory();
            parentGUI.show();
        } else if (slot == RESET_BUTTON_SLOT) {
            // Reset to defaults
            parentGUI.currentSort = LootboxListGUI.SortType.NAME_ASC;
            parentGUI.showExamplesOnly = false;
            parentGUI.showNormalOnly = false;
            updateParentAndRefresh();
        } else if (slot == CLOSE_BUTTON_SLOT) {
            player.closeInventory();
        }
    }

    /**
     * Updates parent GUI and refreshes this GUI.
     */
    private void updateParentAndRefresh() {
        parentGUI.applyFiltersAndSort();
        parentGUI.updateInventory();
        updateInventory();
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Cleanup if needed
    }

    @Override
    public @NotNull
    Inventory getInventory() {
        return inventory;
    }

    /**
     * Shows the GUI to a player.
     */
    public void show() {
        player.openInventory(inventory);
    }
}
