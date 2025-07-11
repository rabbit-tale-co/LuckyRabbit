package co.RabbitTale.luckyRabbit.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.DESCRIPTION_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.ERROR_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.INFO_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.ITEM_COLOR;
import co.RabbitTale.luckyRabbit.gui.utils.GUIUtils;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

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
    private static final int FILTER_BUTTON_SLOT = 44; // Right bottom corner (5*9 - 1)

    private final LuckyRabbit plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Lootbox> lootboxes;
    private int currentPage = 0;
    private final Map<Integer, Lootbox> slotToLootboxMap = new HashMap<>();

    // Filter settings
    public enum SortType {
        NAME_ASC("Name A-Z"),
        NAME_DESC("Name Z-A"),
        CREATION_TIME("Creation Time"),
        POPULARITY("Popularity"),
        ITEM_COUNT("Item Count");

        private final String displayName;

        SortType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public SortType currentSort = SortType.NAME_ASC;
    public boolean showExamplesOnly = false;
    public boolean showNormalOnly = false;

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
            Logger.debug("Admin player " + player.getName() + " sees " + lootboxCollection.size() + " lootboxes (including examples)");
            // Debug: list all lootboxes for admin
            for (Lootbox lootbox : lootboxCollection) {
                Logger.debug("  - " + lootbox.getId() + " (" + lootbox.getTitle() + ") - Example: " + lootbox.isExample());
            }
        } else {
            lootboxCollection = plugin.getLootboxManager().getAllLootboxes();
            Logger.debug("Regular player " + player.getName() + " sees " + lootboxCollection.size() + " lootboxes (excluding examples)");
            // Debug: list all lootboxes for regular player
            for (Lootbox lootbox : lootboxCollection) {
                Logger.debug("  - " + lootbox.getId() + " (" + lootbox.getTitle() + ") - Example: " + lootbox.isExample());
            }
        }

        // Apply filters and create sorted list
        this.lootboxes = new ArrayList<>(lootboxCollection);
        applyFiltersAndSort();

        // Calculate total pages
        int totalPages = Math.max(1, (int) Math.ceil(lootboxes.size() / (double) PAGE_SIZE));

        this.inventory = Bukkit.createInventory(this, ROWS * 9,
                Component.text("Lootboxes (Page " + (currentPage + 1) + "/" + totalPages + ")"));

        updateInventory();
    }

    /**
     * Applies current filters and sorting to the lootbox list.
     */
    public void applyFiltersAndSort() {
        // Apply type filters
        List<Lootbox> filteredList = new ArrayList<>();
        for (Lootbox lootbox : lootboxes) {
            if (showExamplesOnly && !lootbox.isExample()) {
                continue;
            }
            if (showNormalOnly && lootbox.isExample()) {
                continue;
            }
            filteredList.add(lootbox);
        }
        this.lootboxes.clear();
        this.lootboxes.addAll(filteredList);

        // Apply sorting
        switch (currentSort) {
            case NAME_ASC:
                if (player.hasPermission("luckyrabbit.admin")) {
                    // Examples first, then normal, both alphabetically
                    this.lootboxes.sort((a, b) -> {
                        boolean aIsExample = a.isExample();
                        boolean bIsExample = b.isExample();
                        if (aIsExample && !bIsExample) {
                            return -1;
                        }
                        if (!aIsExample && bIsExample) {
                            return 1;
                        }
                        return a.getTitle().compareToIgnoreCase(b.getTitle());
                    });
                } else {
                    this.lootboxes.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                }
                break;
            case NAME_DESC:
                this.lootboxes.sort((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;
            case CREATION_TIME:
                // Sort by file creation time (newest first)
                this.lootboxes.sort((a, b) -> {
                    // For now, use ID as proxy for creation time
                    return a.getId().compareToIgnoreCase(b.getId());
                });
                break;
            case POPULARITY:
                // Sort by open count (most popular first)
                this.lootboxes.sort((a, b) -> Integer.compare(b.getOpenCount(), a.getOpenCount()));
                break;
            case ITEM_COUNT:
                // Sort by number of items (most items first)
                this.lootboxes.sort((a, b) -> Integer.compare(b.getItems().size(), a.getItems().size()));
                break;
        }
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
    public void updateInventory() {
        GUIUtils.setupBorder(inventory, ROWS);

        // Clear slot mapping
        slotToLootboxMap.clear();

        // Add lootbox items with separator for admins
        int startIndex = currentPage * PAGE_SIZE;
        boolean isAdmin = player.hasPermission("luckyrabbit.admin");
        boolean separatorAdded = false;
        int displayedItems = 0;

        for (int i = 0; i < PAGE_SIZE && startIndex + i < lootboxes.size(); i++) {
            Lootbox lootbox = lootboxes.get(startIndex + i);

            // Add separator between examples and normal lootboxes for admins
            if (isAdmin && !separatorAdded && !lootbox.isExample()) {
                // Check if we have any examples before this point
                boolean hasExamples = false;
                for (int j = startIndex; j < startIndex + i; j++) {
                    if (j < lootboxes.size() && lootboxes.get(j).isExample()) {
                        hasExamples = true;
                        break;
                    }
                }

                if (hasExamples && displayedItems < PAGE_SIZE - 1) {
                    // Add separator glass pane
                    int separatorRow = (displayedItems / 7) + 1;
                    int separatorCol = (displayedItems % 7) + 1;
                    int separatorSlot = separatorRow * 9 + separatorCol;

                    inventory.setItem(separatorSlot, createSeparatorItem());
                    separatorAdded = true;
                    displayedItems++;

                    // Skip this iteration if we've reached the page limit
                    if (displayedItems >= PAGE_SIZE) {
                        break;
                    }
                }
            }

            // Calculate position in the 7x3 grid (left to right, top to bottom)
            int row = (displayedItems / 7) + 1; // Start from row 1
            int col = (displayedItems % 7) + 1; // Start from col 1
            int slot = row * 9 + col;

            inventory.setItem(slot, createLootboxItem(lootbox));
            slotToLootboxMap.put(slot, lootbox); // Map slot to lootbox
            displayedItems++;
        }

        // Add navigation buttons - always show them, just like in LootboxContentGUI
        inventory.setItem(PREV_BUTTON_SLOT, GUIUtils.createNavigationButton("Previous Page",
                Material.ARROW, currentPage > 0));
        inventory.setItem(NEXT_BUTTON_SLOT, GUIUtils.createNavigationButton("Next Page",
                Material.ARROW, (currentPage + 1) * PAGE_SIZE < lootboxes.size()));

        // Add close button
        inventory.setItem(CLOSE_BUTTON_SLOT, GUIUtils.createNavigationButton("Close", Material.BARRIER, true));

        // Add filter button (right bottom corner)
        inventory.setItem(FILTER_BUTTON_SLOT, createFilterButton());

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
                .deserialize(lootbox.getTitle())
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

        // Show animation type for admins
        if (player.hasPermission("luckyrabbit.admin")) {
            lore.add(Component.empty());
            lore.add(Component.text("Animation: " + lootbox.getAnimationType().name())
                    .color(INFO_COLOR)
                    .decoration(TextDecoration.ITALIC, false));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        String createdDate = fmt.format(Instant.ofEpochMilli(lootbox.getCreated()));

        // Add statistics
        // FIXME: make example lootbox show on left side (as first in list) next in list show normal lootboxes (also add option to filter (next gui) - name, open count, items count, type (example, normal) ect.)
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
        lore.add(Component.text("  • Created At: " + createdDate)
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
                .color(LootboxCommand.TARGET_COLOR)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a separator item between examples and normal lootboxes.
     *
     * @return ItemStack configured as separator
     */
    private ItemStack createSeparatorItem() {
        ItemStack item = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        // Set display name with fancy formatting
        meta.displayName(Component.text("✦ ← Examples | Normal → ✦")
                .color(LootboxCommand.INFO_COLOR)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("← Examples: Templates & Inspiration")
                .color(LootboxCommand.TARGET_COLOR)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("→ Normal: Your Custom Lootboxes")
                .color(LootboxCommand.SUCCESS_COLOR)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("⚠ Examples cannot be placed in world")
                .color(LootboxCommand.ERROR_COLOR)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a filter button for the GUI.
     *
     * @return ItemStack configured as filter button
     */
    private ItemStack createFilterButton() {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        meta.displayName(Component.text("Filter & Sort")
                .color(LootboxCommand.INFO_COLOR)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Current Sort: " + currentSort.getDisplayName())
                .color(LootboxCommand.DESCRIPTION_COLOR)
                .decoration(TextDecoration.ITALIC, false));

        // if (showExamplesOnly) {
        //     lore.add(Component.text("Filter: Examples Only")
        //             .color(LootboxCommand.ERROR_COLOR)
        //             .decoration(TextDecoration.ITALIC, false));
        // } else if (showNormalOnly) {
        //     lore.add(Component.text("Filter: Normal Only")
        //             .color(NamedTextColor.GREEN)
        //             .decoration(TextDecoration.ITALIC, false));
        // } else {
        //     lore.add(Component.text("Filter: All Types")
        //             .color(NamedTextColor.WHITE)
        //             .decoration(TextDecoration.ITALIC, false));
        // }
        lore.add(Component.empty());
        lore.add(Component.text("Click to open filter menu")
                .color(LootboxCommand.DESCRIPTION_COLOR)
                .decoration(TextDecoration.ITALIC, true));

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
        } else if (slot == FILTER_BUTTON_SLOT) {
            // Open filter GUI
            new LootboxFilterGUI(player, this).show();
        } else {
            // Check if clicked on separator (only for admins)
            if (player.hasPermission("luckyrabbit.admin")
                    && event.getCurrentItem().getType() == Material.ORANGE_STAINED_GLASS_PANE) {
                // Ignore clicks on separator
                return;
            }

            // Check if this slot contains a lootbox using the mapping
            Lootbox lootbox = slotToLootboxMap.get(slot);
            if (lootbox != null) {
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
