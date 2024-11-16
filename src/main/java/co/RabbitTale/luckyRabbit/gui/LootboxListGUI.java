package co.RabbitTale.luckyRabbit.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.gui.utils.GUIUtils;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.*;

/*
 * LootboxListGUI.java
 *
 * GUI for displaying all available lootboxes.
 * Provides paginated list with lootbox information and management options.
 *
 * Features:
 * - Paginated display (7x3 grid per page)
 * - Permission-based content (admin/user views)
 * - Interactive buttons for navigation
 * - Detailed lootbox information display
 * - Quick access to lootbox management
 *
 * Layout:
 * - Main content: 7x3 grid of lootboxes
 * - Navigation: Previous/Next page buttons
 * - Controls: Close button, additional admin options
 * - Statistics: Key count, open count, items available
 */
public class LootboxListGUI implements GUI {

    private static final int ROWS = 5;
    private static final int PAGE_SIZE = 21; // 7x3, leaving space for borders
    private static final int PREV_BUTTON_SLOT = 39; // Left side (adjusted for 5 rows)
    private static final int NEXT_BUTTON_SLOT = 41; // Right side (adjusted for 5 rows)
    private static final int CLOSE_BUTTON_SLOT = 40; // Center bottom (adjusted for 5 rows)

    private final LuckyRabbit plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Lootbox> lootboxes;
    private int currentPage = 0;

    /**
     * Creates a new lootbox list GUI. Loads appropriate lootboxes based on
     * player permissions.
     *
     * @param player Player viewing the GUI
     */
    public LootboxListGUI(Player player) {
        this.plugin = LuckyRabbit.getInstance();
        this.player = player;

        // Get appropriate lootbox collection based on permissions
        Collection<Lootbox> lootboxCollection;
        if (player.hasPermission("luckyrabbit.admin")) {
            lootboxCollection = plugin.getLootboxManager().getAllLootboxesAdmin();
        } else {
            lootboxCollection = plugin.getLootboxManager().getAllLootboxes();
        }

        // Create a sorted list of lootboxes
        this.lootboxes = new ArrayList<>(lootboxCollection);

        // Calculate total pages
        int totalPages = Math.max(1, (int) Math.ceil(lootboxes.size() / (double) PAGE_SIZE));

        this.inventory = Bukkit.createInventory(this, ROWS * 9,
                Component.text("Lootboxes (Page " + (currentPage + 1) + "/" + totalPages + ")"));

        updateInventory();
    }

    /**
     * Opens the GUI for a player. Shows first page by default.
     *
     * @param player Player to show GUI to
     */
    public static void openGUI(Player player) {
        openGUI(player, 1); // Default to first page
    }

    /**
     * Opens the GUI for a player at a specific page.
     *
     * @param player Player to show GUI to
     * @param page Page number to display
     */
    public static void openGUI(Player player, int page) {
        Collection<Lootbox> lootboxes;
        if (player.hasPermission("luckyrabbit.admin")) {
            lootboxes = LuckyRabbit.getInstance().getLootboxManager().getAllLootboxesAdmin();
        } else {
            lootboxes = LuckyRabbit.getInstance().getLootboxManager().getAllLootboxes();
        }

        LootboxListGUI gui = new LootboxListGUI(player);

        // Calculate total pages
        int totalPages = Math.max(1, (int) Math.ceil(lootboxes.size() / (double) PAGE_SIZE));

        // Validate page number
        if (page < 1 || page > totalPages) {
            player.sendMessage(Component.text("Invalid page number! Available pages: 1-" + totalPages)
                    .color(NamedTextColor.RED));
            return;
        }

        gui.currentPage = page - 1; // Convert to 0-based index
        gui.updateInventory();
        player.openInventory(gui.getInventory());
    }

    /**
     * Updates the inventory contents. Refreshes lootbox display and navigation
     * buttons.
     */
    private void updateInventory() {
        GUIUtils.setupBorder(inventory, ROWS);

        // Add lootbox items
        int startIndex = currentPage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && startIndex + i < lootboxes.size(); i++) {
            // Calculate position in the 7x3 grid (left to right, top to bottom)
            int row = (i / 7) + 1; // Start from row 1
            int col = (i % 7) + 1; // Start from col 1
            int slot = row * 9 + col;

            Lootbox lootbox = lootboxes.get(startIndex + i);
            inventory.setItem(slot, createLootboxItem(lootbox));
        }

        // Add navigation buttons - always show them, just like in LootboxContentGUI
        inventory.setItem(PREV_BUTTON_SLOT, GUIUtils.createNavigationButton("Previous Page",
                Material.ARROW, currentPage > 0));
        inventory.setItem(NEXT_BUTTON_SLOT, GUIUtils.createNavigationButton("Next Page",
                Material.ARROW, (currentPage + 1) * PAGE_SIZE < lootboxes.size()));

        // Add close button
        inventory.setItem(CLOSE_BUTTON_SLOT, GUIUtils.createNavigationButton("Close", Material.BARRIER, true));

        // Update title with current page
        int totalPages = Math.max(1, (int) Math.ceil(lootboxes.size() / (double) PAGE_SIZE));
        player.openInventory(Bukkit.createInventory(this, ROWS * 9,
                Component.text("Lootboxes (Page " + (currentPage + 1) + "/" + totalPages + ")")));
        player.getOpenInventory().getTopInventory().setContents(inventory.getContents());
    }

    /**
     * Creates a display item for a lootbox. Includes statistics and admin
     * options.
     *
     * @param lootbox Lootbox to create item for
     * @return ItemStack configured for display
     */
    private ItemStack createLootboxItem(Lootbox lootbox) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        // Set display name with MiniMessage format support
        meta.displayName(MiniMessage.miniMessage()
                .deserialize(lootbox.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        // Add example lootbox indicator for admins
        if (plugin.getLootboxManager().isExampleLootbox(lootbox.getId())) {
            lore.add(Component.empty());
            lore.add(Component.text("EXAMPLE LOOTBOX")
                    .color(INFO_COLOR)
                    .decoration(TextDecoration.BOLD, true));
            lore.add(Component.text("Cannot be placed in world")
                    .color(DESCRIPTION_COLOR));
            lore.add(Component.empty());
        }

        // Add existing lore lines with MiniMessage parsing
        for (String loreLine : lootbox.getLore()) {
            lore.add(MiniMessage.miniMessage()
                    .deserialize(loreLine)
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Add statistics
        // TODO: open count and items count make as separated color
        lore.add(Component.empty());
        lore.add(Component.text("Statistics:")
                .color(INFO_COLOR)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • Times opened: " + lootbox.getOpenCount())
                .color(DESCRIPTION_COLOR)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • Items available: " + lootbox.getItems().size())
                .color(DESCRIPTION_COLOR)
                .decoration(TextDecoration.ITALIC, false));

        // Add key count
        int keyCount = plugin.getUserManager().getKeyCount(player.getUniqueId(), lootbox.getId());
        lore.add(Component.empty());
        lore.add(Component.text("Your keys: " + keyCount)
                .color(keyCount > 0 ? ITEM_COLOR : ERROR_COLOR)
                .decoration(TextDecoration.ITALIC, false));

        // Add actions
        lore.add(Component.empty());
        lore.add(Component.text("Click to view contents")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handles inventory click events. Processes navigation and lootbox
     * interaction.
     *
     * @param event The click event
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);

        int slot = event.getRawSlot();

        if (slot == PREV_BUTTON_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
        } else if (slot == NEXT_BUTTON_SLOT && (currentPage + 1) * PAGE_SIZE < lootboxes.size()) {
            currentPage++;
            updateInventory();
        } else if (slot == CLOSE_BUTTON_SLOT) {
            player.closeInventory();
        } else {
            // Calculate which lootbox was clicked
            int row = slot / 9;
            int col = slot % 9;

            // Check if click was in the valid area (not on border)
            if (row > 0 && row < ROWS - 1 && col > 0 && col < 8) {
                // Calculate the index in the lootboxes list
                int index = currentPage * PAGE_SIZE + ((row - 1) * 7) + (col - 1);

                if (index < lootboxes.size()) {
                    Lootbox lootbox = lootboxes.get(index);

                    if (event.isShiftClick() && event.isLeftClick() && player.hasPermission("luckyrabbit.admin")) {
                        // Show delete confirmation
                        new LootboxDeleteConfirmationGUI(player, lootbox).show();
                    } else {
                        // Show contents
                        player.closeInventory(); // Close current inventory first
                        new LootboxContentGUI(player, lootbox).show();
                    }
                }
            }
        }
    }

    /**
     * Handles inventory close events. Cleans up any necessary resources.
     *
     * @param event The close event
     */
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
     * Shows the GUI to a player. Opens the inventory for viewing.
     */
    public void show() {
        player.openInventory(inventory);
    }
}
