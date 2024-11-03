package co.RabbitTale.luckyRabbitFoot.listeners;

import org.bukkit.plugin.PluginManager;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;

public class ListenerManager {
    private final LuckyRabbitFoot plugin;

    public ListenerManager(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        Logger.info("Registering event listeners...");
        PluginManager pm = plugin.getServer().getPluginManager();

        // Register GUI listener
        pm.registerEvents(new GUIListener(), plugin);

        // Register Player listener
        pm.registerEvents(new PlayerListener(plugin), plugin);
    }
}
