package co.RabbitTale.luckyRabbit.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

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
import co.RabbitTale.luckyRabbit.gui.animations.BaseAnimationGUI;
import co.RabbitTale.luckyRabbit.gui.animations.CascadeSpinGUI;
import co.RabbitTale.luckyRabbit.gui.animations.CircleSpinGUI;
import co.RabbitTale.luckyRabbit.gui.animations.HorizontalSpinGUI;
import co.RabbitTale.luckyRabbit.gui.animations.PinPointSpinGUI;
import co.RabbitTale.luckyRabbit.gui.animations.ThreeInRowSpinGUI;
import co.RabbitTale.luckyRabbit.gui.utils.GUIUtils;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.*;

public class LootboxContentGUI implements GUI {

    private static final int ROWS = 5;
    private static final int PAGE_SIZE = 21; // 7x3 grid in middle
    private static final int PREV_PAGE_SLOT = 39; // Bottom left (adjusted for 5 rows)
    private static final int NEXT_PAGE_SLOT = 41; // Bottom right (adjusted for 5 rows)
    private static final int OPEN_BUTTON_SLOT = 40; // Bottom middle (adjusted for 5 rows)
    private static final int EXIT_BUTTON_SLOT = 44; // Bottom right corner (adjusted for 5 rows)

    private final LuckyRabbit plugin;
    private final Player player;
    private final Lootbox lootbox;
    private final Inventory inventory;
    private int currentPage = 0;
    private final List<LootboxItem> items;
    private final boolean showBackButton;
    private final boolean showOpenButton;

    public LootboxContentGUI(Player player, Lootbox lootbox) {
        this(player, lootbox, true, false);
    }

    public LootboxContentGUI(Player player, Lootbox lootbox, boolean showBackButton) {
        this(player, lootbox, showBackButton, true);
    }

    public LootboxContentGUI(Player player, Lootbox lootbox, boolean showBackButton, boolean showOpenButton) {
        this.plugin = LuckyRabbit.getInstance();
        this.player = player;
        this.lootbox = lootbox;
        this.items = new ArrayList<>(lootbox.getItems().values());
        this.showBackButton = showBackButton;
        this.showOpenButton = showOpenButton || player.hasPermission("luckyrabbit.admin");

        // Calculate total pages
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));

        // Create inventory with page info in title - without color
        String displayName = PlainTextComponentSerializer.plainText()
            .serialize(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()));
        this.inventory = Bukkit.createInventory(this, ROWS * 9,
                Component.text(displayName + " (Page " + (currentPage + 1) + "/" + totalPages + ")"));

        updateInventory();
    }

    private void updateInventory() {
        GUIUtils.setupBorder(inventory, ROWS);

        // Add items in middle 3 rows (7x3 grid)
        int startIndex = currentPage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && startIndex + i < items.size(); i++) {
            // Calculate position in the 7x3 grid (left to right, top to bottom)
            int row = (i / 7) + 1; // Start from row 1
            int col = (i % 7) + 1; // Start from col 1
            int slot = row * 9 + col;

            LootboxItem item = items.get(startIndex + i);
            inventory.setItem(slot, createDisplayItem(item));
        }

        // Add navigation buttons
        inventory.setItem(PREV_PAGE_SLOT, GUIUtils.createNavigationButton("Previous Page",
            Material.ARROW, currentPage > 0));
        inventory.setItem(NEXT_PAGE_SLOT, GUIUtils.createNavigationButton("Next Page",
            Material.ARROW, (currentPage + 1) * PAGE_SIZE < items.size()));

        // Add open button only if allowed
        if (showOpenButton) {
            updateOpenButton();
        }

        // Only add exit button if showing back button
        if (showBackButton) {
            inventory.setItem(EXIT_BUTTON_SLOT, GUIUtils.createNavigationButton("Back to List", Material.BARRIER, true));
        }

        // Update title with current page - without color
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
        String displayName = PlainTextComponentSerializer.plainText()
            .serialize(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()));
        player.openInventory(Bukkit.createInventory(this, ROWS * 9,
                Component.text(displayName + " (Page " + (currentPage + 1) + "/" + totalPages + ")")));
        player.getOpenInventory().getTopInventory().setContents(inventory.getContents());
    }

    private void updateOpenButton() {
        int keyCount = plugin.getUserManager().getKeyCount(player.getUniqueId(), lootbox.getId());
        ItemStack openButton = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta openMeta = openButton.getItemMeta();
        openMeta.displayName(Component.text("Open Lootbox")
                .color(keyCount > 0 ? ITEM_COLOR : ERROR_COLOR));

        List<Component> openLore = new ArrayList<>();
        openLore.add(Component.text("You have " + keyCount + " key(s)")
                .color(keyCount > 0 ? ITEM_COLOR : ERROR_COLOR));
        if (keyCount > 0) {
            openLore.add(Component.empty());
            openLore.add(Component.text("Click to open!")
                    .color(INFO_COLOR));
        } else {
            openLore.add(Component.text("You need a key to open this lootbox!")
                    .color(ERROR_COLOR));
        }
        openMeta.lore(openLore);
        openButton.setItemMeta(openMeta);
        inventory.setItem(OPEN_BUTTON_SLOT, openButton);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);

        int slot = event.getRawSlot();

        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
        } else if (slot == NEXT_PAGE_SLOT && (currentPage + 1) * PAGE_SIZE < items.size()) {
            currentPage++;
            updateInventory();
        } else if (slot == EXIT_BUTTON_SLOT && showBackButton) {
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        } else if (slot == OPEN_BUTTON_SLOT && showOpenButton) {
            handleOpenButton();
        } else {
            // Calculate if click was in valid item area
            int row = slot / 9;
            int col = slot % 9;

            if (row > 0 && row < ROWS - 1 && col > 0 && col < 8) {
                // Calculate the index in the items list
                int index = currentPage * PAGE_SIZE + ((row - 1) * 7) + (col - 1);

                if (index < items.size() && event.isShiftClick() && event.isLeftClick() && player.hasPermission("luckyrabbit.admin")) {
                    // Show delete confirmation for item
                    LootboxItem item = items.get(index);
                    new ItemDeleteConfirmationGUI(player, lootbox, item).show();
                }
            }
        }
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

    private ItemStack createDisplayItem(LootboxItem item) {
        // Get the display item with proper formatting from LootboxItem
        ItemStack displayItem = item.getDisplayItem();
        ItemMeta meta = displayItem.getItemMeta();

        if (meta != null) {
            List<Component> lore = new ArrayList<>();

            // Get existing lore from the display item
            if (meta.hasLore()) {
                lore.addAll(Objects.requireNonNull(meta.lore()));
            }

            // Only add chance and rarity if they're not already in the lore
            boolean hasChance = false;
            boolean hasRarity = false;

            for (Component loreLine : lore) {
                String plainText = PlainTextComponentSerializer.plainText().serialize(loreLine);
                if (plainText.contains("Chance:")) hasChance = true;
                if (plainText.contains("Rarity:")) hasRarity = true;
            }

            // Add empty line before stats if needed
            if ((!hasChance || !hasRarity) && !lore.isEmpty()) {
                lore.add(Component.empty());
            }

            // Add missing stats
            if (!hasChance) {
                lore.add(Component.text("Chance: " + item.getChance() + "%")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            }
            if (!hasRarity) {
                lore.add(Component.text("Rarity: " + item.getRarity())
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            }

            // Add admin lore if needed
            if (player.hasPermission("luckyrabbit.admin")) {
                lore.add(Component.empty());
                lore.add(Component.text("ADMIN - Shift + Left Click to remove")
                    .color(ERROR_COLOR)
                    .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    private void handleOpenButton() {
        int keyCount = plugin.getUserManager().getKeyCount(player.getUniqueId(), lootbox.getId());

        if (keyCount > 0) {
            // Check if lootbox has items
            if (lootbox.getItems().isEmpty()) {
                player.sendMessage(Component.text("This lootbox is empty!")
                        .color(ERROR_COLOR));
                return;
            }

            try {
                // Use key before creating animation
                plugin.getUserManager().useKey(player.getUniqueId(), lootbox.getId());

                BaseAnimationGUI animationGUI = switch (lootbox.getAnimationType()) {
                    case PIN_POINT -> new PinPointSpinGUI(plugin, player, lootbox);
                    case CIRCLE -> new CircleSpinGUI(plugin, player, lootbox);
                    case CASCADE -> new CascadeSpinGUI(plugin, player, lootbox);
                    case THREE_IN_ROW -> new ThreeInRowSpinGUI(plugin, player, lootbox);
                    default -> new HorizontalSpinGUI(plugin, player, lootbox);
                };

                // Close inventory and show animation
                player.closeInventory();
                player.openInventory(animationGUI.getInventory());

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error creating animation GUI: {0}", e.getMessage());
                e.printStackTrace();

                // Refund the key and show error message
                plugin.getUserManager().addKeys(player.getUniqueId(), lootbox.getId(), 1);
                player.sendMessage(Component.text("Error opening lootbox: " + e.getMessage())
                        .color(ERROR_COLOR));
            }
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("You don't have a key for this lootbox!")
                    .color(ERROR_COLOR));
        }
    }

    public void show() {
        player.openInventory(inventory);
    }
}
