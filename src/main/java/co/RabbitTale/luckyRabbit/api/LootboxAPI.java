package co.RabbitTale.luckyRabbit.api;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.gui.LootboxContentGUI;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public API for LuckyRabbitFoot plugin
 * Other plugins can use this API to interact with lootboxes
 *
 * @param plugin Plugin instance
 */
public record LootboxAPI(LuckyRabbit plugin) {

    /**
     * Get all available lootboxes
     *
     * @return Collection of all lootboxes
     */
    public Collection<Lootbox> getAllLootboxes() {
        return plugin.getLootboxManager().getAllLootboxes();
    }

    /**
     * Get a specific lootbox by ID
     *
     * @param id Lootbox ID
     * @return Lootbox object or null if not found
     */
    public Lootbox getLootbox(String id) {
        return plugin.getLootboxManager().getLootbox(id);
    }

    /**
     * Create a new lootbox
     *
     * @param name          Lootbox name
     * @param animationType Animation type
     * @throws IllegalArgumentException if lootbox with this name already exists
     */
    public void createLootbox(String name, AnimationType animationType) {
        plugin.getLootboxManager().createLootbox(name, animationType);
    }

    /**
     * Delete a lootbox
     *
     * @param id Lootbox ID
     */
    public void deleteLootbox(String id) {
        plugin.getLootboxManager().deleteLootbox(id);
    }

    /**
     * Add an item to a lootbox
     *
     * @param id     Lootbox ID
     * @param item   Item to add
     * @param chance Drop chance (0-100)
     * @param rarity Item rarity
     */
//    public void addItem(String id, ItemStack item, double chance, String rarity) {
//        LootboxItem lootboxItem = new LootboxItem(item, UUID.randomUUID().toString(), chance, rarity);
//        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
//        if (lootbox != null) {
//            lootbox.addItem(lootboxItem);
//            plugin.getLootboxManager().saveLootbox(lootbox);
//        }
//    }

    /**
     * Place a lootbox at a location
     *
     * @param id       Lootbox ID
     * @param location Location to place the lootbox
     */
    public void placeLootbox(String id, Location location) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
        if (lootbox != null) {
            lootbox.addLocation(location);
            plugin.getLootboxManager().saveLootbox(lootbox);
        }
    }

    /**
     * Get the number of keys a player has for a specific lootbox
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @return Number of keys
     */
    public int getKeyCount(UUID playerId, String lootboxId) {
        return plugin.getUserManager().getKeyCount(playerId, lootboxId);
    }

    /**
     * Give keys to a player
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @param amount    Number of keys to give
     */
    public void giveKeys(UUID playerId, String lootboxId, int amount) {
        plugin.getUserManager().addKeys(playerId, lootboxId, amount);
    }

    /**
     * Remove keys from a player
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @param amount    Number of keys to remove
     */
    public void removeKeys(UUID playerId, String lootboxId, int amount) {
        plugin.getUserManager().removeKeys(playerId, lootboxId, amount);
    }

    /**
     * Check if a player has a key for a specific lootbox
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @return true if player has at least one key
     */
    public boolean hasKey(UUID playerId, String lootboxId) {
        return plugin.getUserManager().hasKey(playerId, lootboxId);
    }

    /**
     * Get all lootbox locations
     *
     * @param id Lootbox ID
     * @return List of locations where this lootbox is placed
     */
    public List<Location> getLootboxLocations(String id) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
        return lootbox != null ? lootbox.getLocations() : List.of();
    }

    /**
     * Open lootbox preview GUI for a player
     *
     * @param player    Player to show the GUI to
     * @param lootboxId Lootbox ID
     */
    public void openPreview(Player player, String lootboxId) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
        if (lootbox != null) {
            new LootboxContentGUI(player, lootbox).show();
        }
    }

}