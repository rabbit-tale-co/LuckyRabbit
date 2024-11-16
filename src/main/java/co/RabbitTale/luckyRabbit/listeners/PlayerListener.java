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

/*
 * PlayerListener.java
 *
 * Handles player-related events for the LuckyRabbit plugin.
 * Manages user data loading/unloading and entity visibility.
 *
 * Features:
 * - User data management on join/quit
 * - Creator effects handling
 * - Lootbox entity visibility control
 * - Per-player entity state management
 */
public class PlayerListener implements Listener {

    private final LuckyRabbit plugin;
    private final CreatorEffects creatorEffects;

    /**
     * Creates a new player listener.
     *
     * @param plugin The LuckyRabbit plugin instance
     */
    public PlayerListener(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.creatorEffects = new CreatorEffects(plugin);
    }

    /**
     * Handles player join events.
     * Loads user data and updates entity visibility.
     *
     * @param event The join event
     */
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

    /**
     * Handles player quit events.
     * Saves and unloads user data.
     *
     * @param event The quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getUserManager().unloadUserData(event.getPlayer().getUniqueId());
    }
}
