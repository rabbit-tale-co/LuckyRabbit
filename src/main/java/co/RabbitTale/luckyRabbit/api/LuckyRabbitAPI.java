package co.RabbitTale.luckyRabbit.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Public API interface for LuckyRabbit plugin
 */
public interface LuckyRabbitAPI {

    void deleteLootbox(String id);

    void addItem(String id, ItemStack item, double chance, String rarity);

    void placeLootbox(String id, Location location);

    int getKeyCount(UUID playerId, String lootboxId);

    void giveKeys(UUID playerId, String lootboxId, int amount);

    void removeKeys(UUID playerId, String lootboxId, int amount);

    boolean hasKey(UUID playerId, String lootboxId);

    List<Location> getLootboxLocations(String id);

    void openPreview(Player player, String lootboxId);
}
