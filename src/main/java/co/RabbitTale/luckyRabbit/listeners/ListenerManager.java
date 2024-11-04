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

        // Register GUI listener
        pm.registerEvents(new GUIListener(), plugin);

        // Register Player listener
        pm.registerEvents(new PlayerListener(plugin), plugin);
    }
}
