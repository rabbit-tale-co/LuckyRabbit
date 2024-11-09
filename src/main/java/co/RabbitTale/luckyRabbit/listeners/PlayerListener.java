package co.RabbitTale.luckyRabbit.listeners;

import java.util.UUID;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.effects.CreatorEffects;
import co.RabbitTale.luckyRabbit.lootbox.entity.LootboxEntity;
import co.RabbitTale.luckyRabbit.utils.Logger;

public class PlayerListener implements Listener {

    private final LuckyRabbit plugin;
    private final CreatorEffects creatorEffects;

    public PlayerListener(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.creatorEffects = new CreatorEffects(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getUserManager().loadUserData(event.getPlayer().getUniqueId());

        // Start creator effects if applicable
        if (CreatorEffects.isCreator(event.getPlayer().getUniqueId())) {
            creatorEffects.startEffects(event.getPlayer());
        }

        // Update visibility of all lootbox entities for the player
        for (Entity entity : event.getPlayer().getWorld().getEntities()) {
            if (entity instanceof ArmorStand && entity.hasMetadata("LootboxEntity")) {
                try {
                    // Get the entity's UUID from the armor stand itself
                    UUID entityUUID = entity.getUniqueId();
                    LootboxEntity lootboxEntity = plugin.getLootboxManager().getEntityById(entityUUID);

                    if (lootboxEntity != null) {
                        lootboxEntity.show(event.getPlayer());
                    }
                } catch (Exception e) {
                    Logger.error("Failed to process lootbox entity visibility: " + e.getMessage());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getUserManager().unloadUserData(event.getPlayer().getUniqueId());
    }
}
