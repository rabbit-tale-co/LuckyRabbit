package co.RabbitTale.luckyRabbit.listeners;

import org.bukkit.plugin.PluginManager;

import co.RabbitTale.luckyRabbit.LuckyRabbit;

/*
 * ListenerManager.java
 *
 * Manages event listener registration for the LuckyRabbit plugin.
 * Central point for registering all plugin listeners.
 */
public class ListenerManager {

    private final LuckyRabbit plugin;

    /**
     * Creates a new listener manager.
     *
     * @param plugin The LuckyRabbit plugin instance
     */
    public ListenerManager(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all plugin event listeners. Called during plugin
     * initialization.
     */
    public void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();

        // Register all listeners
        pm.registerEvents(new PlayerListener(plugin), plugin);
        pm.registerEvents(new GUIListener(), plugin);
    }
}
