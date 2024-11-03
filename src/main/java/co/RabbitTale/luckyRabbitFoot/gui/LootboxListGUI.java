package co.RabbitTale.luckyRabbitFoot.gui;

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

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LootboxListGUI implements GUI {

    private static final int ROWS = 6;
    private static final int PAGE_SIZE = 45; // 9x5, leaving bottom row for navigation
    private static final int PREV_BUTTON_SLOT = 45;
    private static final int NEXT_BUTTON_SLOT = 53;
    private static final int CLOSE_BUTTON_SLOT = 49;

    private final LuckyRabbitFoot plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Lootbox> lootboxes;
    private int currentPage = 0;

    public LootboxListGUI(Player player) {
        this.plugin = LuckyRabbitFoot.getInstance();
        this.player = player;

        // Debug logs
        plugin.getLogger().info("Loading lootboxes into GUI...");
        plugin.getLogger().info("Total lootboxes in manager: " + plugin.getLootboxManager().getAllLootboxes().size());
        plugin.getLogger().info("Lootbox IDs: " + String.join(", ", plugin.getLootboxManager().getLootboxNames()));

        this.lootboxes = new ArrayList<>(plugin.getLootboxManager().getAllLootboxes());

        // More debug
        plugin.getLogger().info("Loaded lootboxes into GUI: " + lootboxes.size());
        for (Lootbox box : lootboxes) {
            plugin.getLogger().info("Lootbox: " + box.getId() + " - " + box.getDisplayName());
        }

        this.inventory = Bukkit.createInventory(this, ROWS * 9,
                Component.text("Lootboxes"));

        updateInventory();
    }

    public static void openGUI(Player player) {
        player.openInventory(new LootboxListGUI(player).getInventory());
    }

    @Override
    public @NotNull
    Inventory getInventory() {
        return inventory;
    }

    private void updateInventory() {
        inventory.clear();

        // Debug log
        plugin.getLogger().info("Updating inventory with " + lootboxes.size() + " lootboxes");

        // Add lootbox items
        int startIndex = currentPage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && startIndex + i < lootboxes.size(); i++) {
            Lootbox lootbox = lootboxes.get(startIndex + i);
            ItemStack item = createLootboxItem(lootbox);
            inventory.setItem(i, item);
            plugin.getLogger().info("Added lootbox to slot " + i + ": " + lootbox.getId());
        }

        // Add navigation buttons
        if (currentPage > 0) {
            inventory.setItem(PREV_BUTTON_SLOT, createNavigationButton("Previous Page", Material.ARROW));
        }

        if ((currentPage + 1) * PAGE_SIZE < lootboxes.size()) {
            inventory.setItem(NEXT_BUTTON_SLOT, createNavigationButton("Next Page", Material.ARROW));
        }

        inventory.setItem(CLOSE_BUTTON_SLOT, createNavigationButton("Close", Material.BARRIER));
    }

    private ItemStack createLootboxItem(Lootbox lootbox) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        // Set display name with MiniMessage format support
        meta.displayName(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        // Add lore lines
        for (String loreLine : lootbox.getLore()) {
            lore.add(MiniMessage.miniMessage().deserialize(loreLine)
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

        if (player.hasPermission("luckyrabbitfoot.admin")) {
            lore.add(Component.text("Shift + Left Click to remove")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationButton(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name)
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
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
        } else if (slot < PAGE_SIZE && slot >= 0) {
            int index = currentPage * PAGE_SIZE + slot;
            if (index < lootboxes.size()) {
                Lootbox lootbox = lootboxes.get(index);

                if (event.isShiftClick() && event.isLeftClick() && player.hasPermission("luckyrabbitfoot.admin")) {
                    // Show delete confirmation
                    new LootboxDeleteConfirmationGUI(player, lootbox).show();
                } else {
                    // Show contents
                    new LootboxContentGUI(player, lootbox).show();
                }
            }
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Cleanup if needed
    }
}
