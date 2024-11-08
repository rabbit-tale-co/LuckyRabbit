package co.RabbitTale.luckyRabbit.api;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.gui.LootboxContentGUI;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LuckyRabbitAPIImpl implements LuckyRabbitAPI {
    private final LuckyRabbit plugin;

    public LuckyRabbitAPIImpl(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<String> getAllLootboxes() {
        return plugin.getLootboxManager().getAllLootboxes()
                .stream()
                .map(Lootbox::getId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean lootboxExists(String id) {
        return plugin.getLootboxManager().getLootbox(id) != null;
    }

    @Override
    public void createLootbox(String name, String animationType) {
        AnimationType type = AnimationType.valueOf(animationType.toUpperCase());
        plugin.getLootboxManager().createLootbox(name, type);
    }

    @Override
    public void deleteLootbox(String id) {
        plugin.getLootboxManager().deleteLootbox(id);
    }

    @Override
    public void addItem(String id, ItemStack item, double chance, String rarity) {
        plugin.getLootboxManager().addItem(null, id, item, rarity, chance);
    }

    @Override
    public void placeLootbox(String id, Location location) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
        if (lootbox != null) {
            lootbox.addLocation(location);
            plugin.getLootboxManager().saveLootbox(lootbox);
        }
    }

    @Override
    public int getKeyCount(UUID playerId, String lootboxId) {
        return plugin.getUserManager().getKeyCount(playerId, lootboxId);
    }

    @Override
    public void giveKeys(UUID playerId, String lootboxId, int amount) {
        plugin.getUserManager().addKeys(playerId, lootboxId, amount);
    }

    @Override
    public void removeKeys(UUID playerId, String lootboxId, int amount) {
        plugin.getUserManager().removeKeys(playerId, lootboxId, amount);
    }

    @Override
    public boolean hasKey(UUID playerId, String lootboxId) {
        return plugin.getUserManager().hasKey(playerId, lootboxId);
    }

    @Override
    public List<Location> getLootboxLocations(String id) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(id);
        return lootbox != null ? lootbox.getLocations() : List.of();
    }

    @Override
    public void openPreview(Player player, String lootboxId) {
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
        if (lootbox != null) {
            new LootboxContentGUI(player, lootbox).show();
        }
    }
}
