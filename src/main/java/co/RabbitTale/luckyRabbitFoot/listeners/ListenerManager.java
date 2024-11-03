package co.RabbitTale.luckyRabbitFoot.listeners;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import org.bukkit.plugin.PluginManager;

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
    }
}
