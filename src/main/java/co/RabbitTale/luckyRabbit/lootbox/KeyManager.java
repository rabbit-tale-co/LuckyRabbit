package co.RabbitTale.luckyRabbit.lootbox;

import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

public class KeyManager {
    private final LuckyRabbit plugin;
    private final NamespacedKey KEY_COUNT_KEY;

    public KeyManager(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.KEY_COUNT_KEY = new NamespacedKey(plugin, "key_count");
    }

    /**
     * Sprawdza czy gracz ma klucz o danej nazwie
     */
    public boolean hasKey(Player player, String keyName) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "key_" + keyName.toLowerCase());
        Integer count = pdc.get(key, PersistentDataType.INTEGER);
        return count != null && count > 0;
    }

    /**
     * Dodaje klucz graczowi
     */
    public void addKey(Player player, String keyName) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "key_" + keyName.toLowerCase());
        int count = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
        pdc.set(key, PersistentDataType.INTEGER, count + 1);
    }

    /**
     * Usuwa klucz graczowi
     */
    public void removeKey(Player player, String keyName) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "key_" + keyName.toLowerCase());
        int count = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
        if (count > 0) {
            pdc.set(key, PersistentDataType.INTEGER, count - 1);
        }
    }

    /**
     * Pobiera ilosc kluczy gracza
     */
    public int getKeyCount(Player player, String keyName) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "key_" + keyName.toLowerCase());
        return pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }
}
