package co.RabbitTale.luckyRabbit.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public API interface for LuckyRabbit plugin
 */
public interface LuckyRabbitAPI {
    /**
     * Get all available lootbox IDs
     *
     * @return Collection of lootbox IDs
     */
    Collection<String> getAllLootboxes();

    /**
     * Check if a lootbox exists
     *
     * @param id Lootbox ID
     * @return true if lootbox exists
     */
    boolean lootboxExists(String id);

    /**
     * Create a new lootbox
     *
     * @param name          Lootbox name
     * @param animationType Animation type (HORIZONTAL, VERTICAL, etc.)
     */
    void createLootbox(String name, String animationType);

    /**
     * Delete a lootbox
     *
     * @param id Lootbox ID
     */
    void deleteLootbox(String id);

    /**
     * Add an item to a lootbox
     *
     * @param id     Lootbox ID
     * @param item   Item to add
     * @param chance Drop chance (0-100)
     * @param rarity Item rarity
     */
    void addItem(String id, ItemStack item, double chance, String rarity);

    /**
     * Place a lootbox at a location
     *
     * @param id       Lootbox ID
     * @param location Location to place the lootbox
     */
    void placeLootbox(String id, Location location);

    /**
     * Get the number of keys a player has for a specific lootbox
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @return Number of keys
     */
    int getKeyCount(UUID playerId, String lootboxId);

    /**
     * Give keys to a player
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @param amount    Number of keys to give
     */
    void giveKeys(UUID playerId, String lootboxId, int amount);

    /**
     * Remove keys from a player
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @param amount    Number of keys to remove
     */
    void removeKeys(UUID playerId, String lootboxId, int amount);

    /**
     * Check if a player has a key for a specific lootbox
     *
     * @param playerId  Player UUID
     * @param lootboxId Lootbox ID
     * @return true if player has at least one key
     */
    boolean hasKey(UUID playerId, String lootboxId);

    /**
     * Get all lootbox locations
     *
     * @param id Lootbox ID
     * @return List of locations where this lootbox is placed
     */
    List<Location> getLootboxLocations(String id);

    /**
     * Open lootbox preview GUI for a player
     *
     * @param player    Player to show the GUI to
     * @param lootboxId Lootbox ID
     */
    void openPreview(Player player, String lootboxId);
}
