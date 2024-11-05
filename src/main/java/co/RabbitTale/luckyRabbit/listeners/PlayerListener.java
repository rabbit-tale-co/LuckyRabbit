package co.RabbitTale.luckyRabbit.listeners;

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

public class PlayerListener implements Listener {

    private final LuckyRabbit plugin;

    public PlayerListener(LuckyRabbit plugin) {
        this.plugin = plugin;
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
            Logger.info("Clicked lootbox with ID: " + lootboxId); // Debug log

            Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
            if (lootbox == null) {
                Logger.error("Could not find lootbox with ID: " + lootboxId);
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
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getUserManager().unloadUserData(event.getPlayer().getUniqueId());
    }
}
