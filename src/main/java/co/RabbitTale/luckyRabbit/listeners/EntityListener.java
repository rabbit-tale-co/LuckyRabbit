package co.RabbitTale.luckyRabbit.listeners;

import java.util.List;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.metadata.MetadataValue;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.gui.LootboxContentGUI;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/*
 * EntityListener.java
 *
 * Handles entity-related events for lootbox entities.
 * Manages lootbox interaction and GUI opening.
 *
 * Features:
 * - Lootbox entity interaction
 * - Permission checking
 * - Example lootbox restrictions
 * - GUI opening
 */
public class EntityListener implements Listener {
    private final LuckyRabbit plugin;

    /**
     * Creates a new entity listener.
     *
     * @param plugin The LuckyRabbit plugin instance
     */
    public EntityListener(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles entity click events.
     * Opens lootbox GUI when players click lootbox entities.
     *
     * @param event The click event
     */
    @EventHandler
    public void onEntityClick(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Check if clicked entity is an armor stand
        if (!(entity instanceof ArmorStand)) {
            return;
        }

        // Check if it's our lootbox entity
        if (!entity.hasMetadata("LootboxEntity")) {
            return;
        }

        event.setCancelled(true);

        // Get lootbox ID from metadata
        List<MetadataValue> metadata = entity.getMetadata("LootboxEntity");
        if (metadata.isEmpty()) {
            return;
        }

        String lootboxId = metadata.get(0).asString();
        Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);

        if (lootbox == null) {
            return;
        }

        // Check if it's an example lootbox and player is not an admin
        if (plugin.getLootboxManager().isExampleLootbox(lootboxId) && !player.hasPermission("luckyrabbit.admin")) {
            player.sendMessage(Component.text("This is an example lootbox - only administrators can open it!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Open GUI without back button but with open button
        new LootboxContentGUI(player, lootbox, false, true).show();
    }
}
