package co.RabbitTale.luckyRabbit.commands;

import org.bukkit.command.PluginCommand;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

public class CommandManager {
    private final LuckyRabbit plugin;

    public CommandManager(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {

        // Register main lootbox command
        PluginCommand lootboxCommand = plugin.getCommand("lootbox");
        if (lootboxCommand != null) {
            LootboxCommand executor = new LootboxCommand(plugin);
            LootboxTabCompleter tabCompleter = new LootboxTabCompleter(plugin);

            lootboxCommand.setExecutor(executor);
            lootboxCommand.setTabCompleter(tabCompleter);

            // TODO: log when debug
            Logger.info("Registered /lootbox command");
        } else {
            Logger.error("Failed to register /lootbox command - command not found in plugin.yml");
        }
    }
}
