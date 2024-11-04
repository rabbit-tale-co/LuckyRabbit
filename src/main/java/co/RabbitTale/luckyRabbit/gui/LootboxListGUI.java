package co.RabbitTale.luckyRabbit.gui;

import java.util.ArrayList;
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
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import static co.RabbitTale.luckyRabbit.gui.LootboxContentGUI.adminLore;

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

    public LootboxListGUI(Player player) {
        this.plugin = LuckyRabbit.getInstance();
        this.player = player;

        // Create a sorted list of lootboxes (oldest first)
        // If you want newest first, use Collections.reverse(sortedLootboxes);
        this.lootboxes = new ArrayList<>(plugin.getLootboxManager().getAllLootboxes());

        // Calculate total pages
        int totalPages = Math.max(1, (int) Math.ceil(lootboxes.size() / (double) PAGE_SIZE));

        this.inventory = Bukkit.createInventory(this, ROWS * 9,
                Component.text("Lootboxes (Page " + (currentPage + 1) + "/" + totalPages + ")"));

        updateInventory();
    }

    public static void openGUI(Player player) {
        openGUI(player, 1); // Default to first page
    }

    public static void openGUI(Player player, int page) {
        LootboxListGUI gui = new LootboxListGUI(player);

        // Calculate total pages
        int totalPages = Math.max(1, (int) Math.ceil(gui.lootboxes.size() / (double) PAGE_SIZE));

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

    @Override
    public @NotNull
    Inventory getInventory() {
        return inventory;
    }

    private void updateInventory() {
        inventory.clear();

        // Add glass pane border
        for (int i = 0; i < ROWS * 9; i++) {
            // First and last row
            if (i < 9 || i >= (ROWS - 1) * 9) {
                inventory.setItem(i, createBorderItem());
            }
            // Side borders
            else if (i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, createBorderItem());
            }
        }

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

        // Always show navigation buttons
        // Previous button (disabled if on first page)
        inventory.setItem(PREV_BUTTON_SLOT, createNavigationButton("Previous Page",
            Material.ARROW, currentPage > 0));

        // Close button
        inventory.setItem(CLOSE_BUTTON_SLOT, createNavigationButton("Close",
            Material.BARRIER, true));

        // Next button (disabled if on last page)
        boolean hasNextPage = (currentPage + 1) * PAGE_SIZE < lootboxes.size();
        inventory.setItem(NEXT_BUTTON_SLOT, createNavigationButton("Next Page",
            Material.ARROW, hasNextPage));

        // Update title with current page
        int totalPages = Math.max(1, (int) Math.ceil(lootboxes.size() / (double) PAGE_SIZE));
        player.openInventory(Bukkit.createInventory(this, ROWS * 9,
                Component.text("Lootboxes (Page " + (currentPage + 1) + "/" + totalPages + ")")));
        player.getOpenInventory().getTopInventory().setContents(inventory.getContents());
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE); // Changed to dark gray
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationButton(String name, Material material, boolean enabled) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (enabled) {
            meta.displayName(Component.text(name)
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(Component.text(name)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
            // Add "disabled" lore
            meta.lore(List.of(Component.text("Not available")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLootboxItem(Lootbox lootbox) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        // Set display name with MiniMessage format support
        meta.displayName(MiniMessage.miniMessage()
                .deserialize(lootbox.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        // Add lore lines with MiniMessage parsing
        for (String loreLine : lootbox.getLore()) {
            lore.add(MiniMessage.miniMessage()
                    .deserialize(loreLine)
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Add statistics
        lore.add(Component.empty());
        lore.add(Component.text("Statistics:")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Times opened: " + lootbox.getOpenCount())
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Items available: " + lootbox.getItems().size())
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        // Add key count
        int keyCount = plugin.getUserManager().getKeyCount(player.getUniqueId(), lootbox.getId());
        lore.add(Component.empty());
        lore.add(Component.text("Your keys: " + keyCount)
                .color(keyCount > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        // Add actions
        lore.add(Component.empty());
        lore.add(Component.text("Click to view contents")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        return adminLore(item, meta, lore, player);
    }

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

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Cleanup if needed
    }
}
