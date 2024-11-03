package co.RabbitTale.luckyRabbitFoot.commands;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import org.bukkit.command.PluginCommand;

public class CommandManager {
    private final LuckyRabbitFoot plugin;

    public CommandManager(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        Logger.info("Registering commands...");

        // Register main lootbox command
        PluginCommand lootboxCommand = plugin.getCommand("lootbox");
        if (lootboxCommand != null) {
            LootboxCommand executor = new LootboxCommand(plugin);
            LootboxTabCompleter tabCompleter = new LootboxTabCompleter(plugin);

            lootboxCommand.setExecutor(executor);
            lootboxCommand.setTabCompleter(tabCompleter);

            Logger.info("Registered /lootbox command");
        } else {
            Logger.error("Failed to register /lootbox command - command not found in plugin.yml");
        }
    }
}
