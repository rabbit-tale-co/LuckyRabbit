package co.RabbitTale.luckyRabbit.config;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

import java.io.File;

public class LootboxConfig {

    public LootboxConfig(LuckyRabbit plugin) {
        File lootboxDirectory = new File(plugin.getDataFolder(), "lootboxes");

        if (!lootboxDirectory.exists()) {
            if (!lootboxDirectory.mkdirs()) {
                Logger.error("Failed to create lootboxes directory!");
            }
        }
    }
}
