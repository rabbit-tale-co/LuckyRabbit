package co.RabbitTale.luckyRabbit.listeners;

import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.gui.LootboxContentGUI;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.utils.Logger;
import co.RabbitTale.luckyRabbit.effects.CreatorEffects;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;
import co.RabbitTale.luckyRabbit.lootbox.entity.LootboxEntity;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final LuckyRabbit plugin;
    private final CreatorEffects creatorEffects;

    public PlayerListener(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.creatorEffects = new CreatorEffects(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        try {
            Entity entity = event.getRightClicked();
            if (!(entity instanceof ArmorStand)) {
                return;
            }

            if (!entity.hasMetadata("LootboxEntity")) {
                return;
            }

            event.setCancelled(true);
            Player player = event.getPlayer();

            // Get the lootbox ID from metadata and log it
            if (entity.getMetadata("LootboxEntity").isEmpty()) {
                Logger.error("LootboxEntity metadata is empty!");
                return;
            }

            String lootboxId = entity.getMetadata("LootboxEntity").get(0).asString();
            Logger.debug("Clicked lootbox with ID: " + lootboxId);

            Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
            if (lootbox == null) {
                Logger.error("Could not find lootbox with ID: " + lootboxId);
                return;
            }

            // Check if it's an example lootbox and player is not an admin
            if (plugin.getLootboxManager().isExampleLootbox(lootboxId) && !player.hasPermission("luckyrabbit.admin")) {
                player.sendMessage(Component.text("This is an example lootbox - only admins can open it!")
                    .color(LootboxCommand.ERROR_COLOR));
                return;
            }

            // Open the GUI directly
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new LootboxContentGUI(player, lootbox).show();
            });

        } catch (Exception e) {
            Logger.error("Error handling lootbox interaction: " + e.getMessage());
            e.printStackTrace();
        }
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
