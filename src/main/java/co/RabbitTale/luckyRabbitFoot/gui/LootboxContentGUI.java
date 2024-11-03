package co.RabbitTale.luckyRabbitFoot.gui;

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

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.gui.animations.BaseAnimationGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.CascadeSpinGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.CircleSpinGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.HorizontalSpinGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.PinPointSpinGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.ThreeInRowSpinGUI;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.RewardRarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class LootboxContentGUI implements GUI {

    private static final int GUI_SIZE = 54;
    private static final int OPEN_BUTTON_SLOT = 49;
    private static final int EXIT_BUTTON_SLOT = 53;
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 51;
    private static final int ITEMS_PER_PAGE = 45;

    private final LuckyRabbitFoot plugin;
    private final Player player;
    private final Lootbox lootbox;
    private final Inventory inventory;
    private int currentPage = 0;
    private final List<LootboxItem> items;

    public LootboxContentGUI(Player player, Lootbox lootbox) {
        this.plugin = LuckyRabbitFoot.getInstance();
        this.player = player;
        this.lootbox = lootbox;
        this.inventory = createInventory();
        this.items = new ArrayList<>(lootbox.getItems().values());
        updateInventory();
    }

    private Inventory createInventory() {
        return Bukkit.createInventory(this, GUI_SIZE,
                Component.text("Lootbox: " + PlainTextComponentSerializer.plainText()
                        .serialize(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))));
    }

    public void show() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();

        // Add items
        int startIndex = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && startIndex + i < items.size(); i++) {
            LootboxItem item = items.get(startIndex + i);
            inventory.setItem(i, createDisplayItem(item));
        }

        // Add navigation buttons
        if (currentPage > 0) {
            inventory.setItem(PREV_PAGE_SLOT, createNavigationButton("Previous Page", Material.ARROW));
        }

        if ((currentPage + 1) * ITEMS_PER_PAGE < items.size()) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavigationButton("Next Page", Material.ARROW));
        }

        // Add open button
        int keyCount = plugin.getLootboxManager().getKeyCount(player.getUniqueId(), lootbox.getId());
        ItemStack openButton = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta openMeta = openButton.getItemMeta();
        openMeta.displayName(Component.text("Open Lootbox")
                .color(keyCount > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> openLore = new ArrayList<>();
        openLore.add(Component.text("You have " + keyCount + " key(s)")
                .color(keyCount > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        if (keyCount > 0) {
            openLore.add(Component.text("Click to open!")
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            openLore.add(Component.text("You need a key to open this lootbox!")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }
        openMeta.lore(openLore);
        openButton.setItemMeta(openMeta);
        inventory.setItem(OPEN_BUTTON_SLOT, openButton);

        // Add exit button
        inventory.setItem(EXIT_BUTTON_SLOT, createNavigationButton("Back to List", Material.BARRIER));
    }

    private ItemStack createDisplayItem(LootboxItem item) {
        ItemStack displayItem = item.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();

        // Add rarity and chance info
        lore.add(Component.empty());

        // Get rarity color from RewardRarity enum
        RewardRarity rarity = RewardRarity.valueOf(item.getRarity().toUpperCase());
        lore.add(Component.text("Rarity: " + rarity.getDisplayName())
                .color(rarity.getColor())
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text(String.format("Chance: %.2f%%", item.getChance()))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        if (player.hasPermission("luckyrabbitfoot.admin")) {
            lore.add(Component.empty());
            lore.add(Component.text("Shift + Left Click to remove")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        displayItem.setItemMeta(meta);
        return displayItem;
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

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        int slot = event.getSlot();

        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
        } else if (slot == NEXT_PAGE_SLOT && (currentPage + 1) * ITEMS_PER_PAGE < items.size()) {
            currentPage++;
            updateInventory();
        } else if (slot == EXIT_BUTTON_SLOT) {
            player.closeInventory();
            LootboxListGUI.openGUI(player);
        } else if (slot == OPEN_BUTTON_SLOT) {
            int keyCount = plugin.getLootboxManager().getKeyCount(player.getUniqueId(), lootbox.getId());

            if (keyCount > 0) {
                // Check if lootbox has items
                if (lootbox.getItems().isEmpty()) {
                    player.sendMessage(Component.text("This lootbox is empty!")
                        .color(NamedTextColor.RED));
                    return;
                }

                try {
                    // Use key before creating animation
                    plugin.getLootboxManager().useKey(player.getUniqueId(), lootbox.getId());

                    BaseAnimationGUI animationGUI = switch (lootbox.getAnimationType()) {
                        case PIN_POINT -> new PinPointSpinGUI(plugin, player, lootbox);
                        case CIRCLE -> new CircleSpinGUI(plugin, player, lootbox);
                        case CASCADE -> new CascadeSpinGUI(plugin, player, lootbox);
                        case THREE_IN_ROW -> new ThreeInRowSpinGUI(plugin, player, lootbox);
                        default -> new HorizontalSpinGUI(plugin, player, lootbox);
                    };

                    // Close inventory and show animation
                    player.closeInventory();
                    animationGUI.show();

                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error creating animation GUI: {0}", e.getMessage());
                    e.printStackTrace();

                    // Refund the key and show error message
                    plugin.getLootboxManager().addKeys(player.getUniqueId(), lootbox.getId(), 1);
                    player.sendMessage(Component.text("Error opening lootbox: " + e.getMessage())
                        .color(NamedTextColor.RED));
                }
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(Component.text("You don't have a key for this lootbox!")
                    .color(NamedTextColor.RED));
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
}
