package co.RabbitTale.luckyRabbit.listeners;

import org.bukkit.plugin.PluginManager;

import co.RabbitTale.luckyRabbit.LuckyRabbit;

public class ListenerManager {
    private final LuckyRabbit plugin;

    public ListenerManager(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();

        // Register all listeners
        pm.registerEvents(new PlayerListener(plugin), plugin);
        pm.registerEvents(new GUIListener(), plugin);
    }
}
