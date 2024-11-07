package co.RabbitTale.luckyRabbit.gui.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class GUIUtils {

    public static void setupBorder(Inventory inventory, int rows) {
        inventory.clear();

        // Add glass pane border
        for (int i = 0; i < rows * 9; i++) {
            // First and last row
            if (i < 9 || i >= (rows - 1) * 9) {
                inventory.setItem(i, createBorderItem());
            }
            // Side borders
            else if (i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, createBorderItem());
            }
        }
    }

    public static ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createNavigationButton(String name, Material material, boolean enabled) {
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
            meta.lore(List.of(Component.text("Not available")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static void setupNavigationButtons(Inventory inventory, int currentPage, int totalItems,
                                            int pageSize, int prevSlot, int nextSlot) {
        boolean hasNextPage = (currentPage + 1) * pageSize < totalItems;

        // Previous page button
        if (currentPage > 0) {
            inventory.setItem(prevSlot, createNavigationButton("Previous Page", Material.ARROW, true));
        }

        // Next page button
        if (hasNextPage) {
            inventory.setItem(nextSlot, createNavigationButton("Next Page", Material.ARROW, true));
        }
    }
}
