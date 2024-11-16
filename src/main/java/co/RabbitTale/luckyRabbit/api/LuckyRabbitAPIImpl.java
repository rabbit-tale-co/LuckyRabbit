package co.RabbitTale.luckyRabbit.api;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.gui.LootboxContentGUI;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/*
 * LuckyRabbitAPIImpl.java
 *
 * Implementation of the LuckyRabbit API interface.
 * Provides actual functionality for all API methods.
 *
 * Features:
 * - Lootbox management
 * - Key management
 * - Location tracking
 * - GUI access
 * - Data validation
 */
public class LuckyRabbitAPIImpl implements LuckyRabbitAPI {

    private final LuckyRabbit plugin;

    /**
     * Creates a new API implementation instance.
     *
     * @param plugin The LuckyRabbit plugin instance
     */
    public LuckyRabbitAPIImpl(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    /**
     * Deletes a lootbox.
     *
     * @param id Lootbox ID
     */
    @Override
    public void deleteLootbox(String id) {
        plugin.getLootboxManager().deleteLootbox(id);
    }

    /**
     * Adds an item to a lootbox.
     *
     * @param id Lootbox ID
     * @param item Item to add
     * @param chance Drop chance (0-100)
     * @param rarity Item rarity
     */
    @Override
    public void addItem(String id, ItemStack item, double chance, String rarity) {
        plugin.getLootboxManager().addItem(null, id, item, rarity, chance);
    }

    /**
     * Places a lootbox at a location.
     *
     * @param id Lootbox ID
     * @param location Location to place the lootbox
     */
    @Override
    public void placeLootbox(String id, Location location) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
        if (lootbox != null) {
            lootbox.addLocation(location);
            plugin.getLootboxManager().saveLootbox(lootbox);
        }
    }

    /**
     * Gets the number of keys a player has.
     *
     * @param playerId Player UUID
     * @param lootboxId Lootbox ID
     * @return Number of keys
     */
    @Override
    public int getKeyCount(UUID playerId, String lootboxId) {
        return plugin.getUserManager().getKeyCount(playerId, lootboxId);
    }

    /**
     * Gives keys to a player.
     *
     * @param playerId Player UUID
     * @param lootboxId Lootbox ID
     * @param amount Number of keys to give
     */
    @Override
    public void giveKeys(UUID playerId, String lootboxId, int amount) {
        plugin.getUserManager().addKeys(playerId, lootboxId, amount);
    }

    /**
     * Removes keys from a player.
     *
     * @param playerId Player UUID
     * @param lootboxId Lootbox ID
     * @param amount Number of keys to remove
     */
    @Override
    public void removeKeys(UUID playerId, String lootboxId, int amount) {
        plugin.getUserManager().removeKeys(playerId, lootboxId, amount);
    }

    /**
     * Checks if a player has a key.
     *
     * @param playerId Player UUID
     * @param lootboxId Lootbox ID
     * @return true if player has at least one key
     */
    @Override
    public boolean hasKey(UUID playerId, String lootboxId) {
        return plugin.getUserManager().hasKey(playerId, lootboxId);
    }

    /**
     * Gets all lootbox locations.
     *
     * @param id Lootbox ID
     * @return List of locations where this lootbox is placed
     */
    @Override
    public List<Location> getLootboxLocations(String id) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
        return lootbox != null ? lootbox.getLocations() : List.of();
    }

    /**
     * Opens lootbox preview GUI for a player.
     *
     * @param player Player to show the GUI to
     * @param lootboxId Lootbox ID
     */
    public void openPreview(Player player, String lootboxId) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
        if (lootbox != null) {
            new LootboxContentGUI(player, lootbox).show();
        }
    }
}
