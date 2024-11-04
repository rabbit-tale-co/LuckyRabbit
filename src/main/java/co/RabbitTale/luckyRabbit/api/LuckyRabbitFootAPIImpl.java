package co.RabbitTale.luckyRabbit.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;

public class LuckyRabbitFootAPIImpl {
    private final LootboxAPI api;

    public LuckyRabbitFootAPIImpl(LuckyRabbit plugin) {
        this.api = new LootboxAPI(plugin);
    }

    public Collection<Lootbox> getAllLootboxes() {
        return api.getAllLootboxes();
    }

    public Lootbox getLootbox(String id) {
        return api.getLootbox(id);
    }

    public void createLootbox(String name, AnimationType animationType) {
        api.createLootbox(name, animationType);
    }

    public void deleteLootbox(String id) {
        api.deleteLootbox(id);
    }

    public void addItem(String id, ItemStack item, double chance, String rarity) {
        api.addItem(id, item, chance, rarity);
    }

    public void placeLootbox(String id, Location location) {
        api.placeLootbox(id, location);
    }

    public int getKeyCount(UUID playerId, String lootboxId) {
        return api.getKeyCount(playerId, lootboxId);
    }

    public void giveKeys(UUID playerId, String lootboxId, int amount) {
        api.giveKeys(playerId, lootboxId, amount);
    }

    public void removeKeys(UUID playerId, String lootboxId, int amount) {
        api.removeKeys(playerId, lootboxId, amount);
    }

    public boolean hasKey(UUID playerId, String lootboxId) {
        return api.hasKey(playerId, lootboxId);
    }

    public List<Location> getLootboxLocations(String id) {
        return api.getLootboxLocations(id);
    }

    public void openPreview(Player player, String lootboxId) {
        api.openPreview(player, lootboxId);
    }
}
