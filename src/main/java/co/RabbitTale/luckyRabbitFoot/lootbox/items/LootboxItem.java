package co.RabbitTale.luckyRabbitFoot.lootbox.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.RewardAction;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

@Getter
public class LootboxItem {
    private final String id;
    private final ItemStack item;
    private final double chance;
    private final String rarity;
    private final RewardAction action;

    private String generateId() {
        // Generate a unique ID based on item type and a random UUID
        String itemType = item.getType().name().toLowerCase();
        String shortUUID = UUID.randomUUID().toString().substring(0, 8);
        return itemType + "-" + shortUUID;
    }

    public LootboxItem(ItemStack item) {
        this.item = item.clone();
        this.id = generateId();
        this.chance = 1.0;
        this.rarity = "COMMON";
        this.action = null;
    }

    public LootboxItem(ItemStack item, String id, double chance, String rarity) {
        this.item = item.clone();
        this.id = id;
        this.chance = chance;
        this.rarity = rarity;
        this.action = null;
    }

    public LootboxItem(ItemStack item, String id, double chance, String rarity, RewardAction action) {
        this.item = item.clone();
        this.id = id;
        this.chance = chance;
        this.rarity = rarity;
        this.action = action;
    }

    public static LootboxItem fromConfig(ConfigurationSection config) {
        String id = config.getString("id");
        double chance = config.getDouble("chance", 1.0);
        String rarity = config.getString("rarity", "COMMON");

        // Check for Oraxen item first
        String oraxenId = config.getString("oraxen_item");
        if (oraxenId != null) {
            try {
                // Try to get Oraxen item
                var oraxenItem = io.th0rgal.oraxen.api.OraxenItems.getItemById(oraxenId);
                if (oraxenItem != null) {
                    ItemStack item = oraxenItem.build();
                    return new OraxenLootboxItem(item, oraxenId, id, chance, rarity);
                } else {
                    // If Oraxen item not found, create a placeholder item
                    return getLootboxItem(id, chance, rarity, oraxenId);
                }
            } catch (Exception e) {
                // Log the error but don't fail the entire loading process
                Logger.warning("Failed to load Oraxen item " + oraxenId + ": " + e.getMessage());
                // Create placeholder item
                return getLootboxItem(id, chance, rarity, oraxenId);
            }
        }

        // Get regular item section
        ConfigurationSection itemSection = config.getConfigurationSection("item");
        if (itemSection == null) {
            throw new IllegalArgumentException("Missing item section in config for item: " + id);
        }

        // Get material type
        String materialName = itemSection.getString("type");
        if (materialName == null) {
            throw new IllegalArgumentException("Missing material type in item config for: " + id);
        }

        // Create ItemStack
        Material material = Material.valueOf(materialName.toUpperCase());
        int amount = itemSection.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);

        // Set meta if exists
        ItemMeta meta = item.getItemMeta();
        if (meta != null && itemSection.contains("meta")) {
            ConfigurationSection metaSection = itemSection.getConfigurationSection("meta");
            if (metaSection != null) {
                // Set display name
                if (metaSection.contains("display-name")) {
                    meta.displayName(MiniMessage.miniMessage()
                        .deserialize(Objects.requireNonNull(metaSection.getString("display-name"))));
                }

                // Set lore
                if (metaSection.contains("lore")) {
                    List<Component> lore = metaSection.getStringList("lore").stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
                        .collect(Collectors.toList());
                    meta.lore(lore);
                }

                // Handle enchantments
                if (metaSection.contains("enchants")) {
                    ConfigurationSection enchants = metaSection.getConfigurationSection("enchants");
                    if (enchants != null) {
                        for (String enchantName : enchants.getKeys(false)) {
                            try {
                                NamespacedKey key = NamespacedKey.minecraft(enchantName.toLowerCase());
                                Enchantment enchant = Enchantment.getByKey(key);
                                if (enchant != null) {
                                    meta.addEnchant(enchant, enchants.getInt(enchantName), true);
                                }
                            } catch (IllegalArgumentException e) {
                                // Skip invalid enchantments
                            }
                        }
                    }
                }

                // Add glow effect if specified
                if (metaSection.getBoolean("glow", false)) {
                    meta.addEnchant(Enchantment.LUCK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
            item.setItemMeta(meta);
        }

        RewardAction action = RewardAction.fromConfig(config.getConfigurationSection("action"));

        return new LootboxItem(item, id, chance, rarity, action);
    }

    @NotNull
    private static LootboxItem getLootboxItem(String id, double chance, String rarity, String oraxenId) {
        ItemStack placeholder = new ItemStack(Material.BARRIER);
        ItemMeta meta = placeholder.getItemMeta();
        meta.displayName(Component.text("Missing Oraxen Item: " + oraxenId)
            .color(NamedTextColor.RED));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("This item will be available")
            .color(NamedTextColor.GRAY));
        lore.add(Component.text("when Oraxen is fully loaded")
            .color(NamedTextColor.GRAY));
        meta.lore(lore);
        placeholder.setItemMeta(meta);
        return new OraxenLootboxItem(placeholder, oraxenId, id, chance, rarity);
    }

    public boolean matches(ItemStack other) {
        if (other == null) return false;
        return item.isSimilar(other);
    }

    public void save(ConfigurationSection config) {
        config.set("id", id);
        config.set("chance", chance);
        config.set("rarity", rarity);
        config.set("item", item.serialize());
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("item", item.serialize());
        data.put("id", id);
        data.put("chance", chance);
        data.put("rarity", rarity);
        return data;
    }

    public void give(Player player) {
        if (action != null) {
            // If there's an action, execute it and don't give the physical item
            action.execute(player);
        } else if (this instanceof OraxenLootboxItem) {
            // If it's an Oraxen item, give the physical item
            player.getInventory().addItem(item.clone());
        } else {
            // Check if the item is a virtual reward by checking its type
            boolean isVirtualReward = item.getType() == Material.GOLD_INGOT || // Virtual coins
                                    item.getType() == Material.NAME_TAG ||     // Roles
                                    item.getType() == Material.TOTEM_OF_UNDYING || // MVP role
                                    item.getType() == Material.DRAGON_EGG ||   // Owner role
                                    item.getType() == Material.EXPERIENCE_BOTTLE; // XP boost

            if (!isVirtualReward) {
                // Only give physical item if it's not a virtual reward
                player.getInventory().addItem(item.clone());
            }
        }
    }
}
