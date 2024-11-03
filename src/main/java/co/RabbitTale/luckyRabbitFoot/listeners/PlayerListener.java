package co.RabbitTale.luckyRabbitFoot.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;

public class PlayerListener implements Listener {
    private final LuckyRabbitFoot plugin;

    public PlayerListener(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
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
