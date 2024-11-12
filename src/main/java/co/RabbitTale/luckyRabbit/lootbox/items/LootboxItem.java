package co.RabbitTale.luckyRabbit.lootbox.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.RabbitTale.luckyRabbit.lootbox.rewards.RewardAction;
import co.RabbitTale.luckyRabbit.lootbox.rewards.RewardRarity;
import co.RabbitTale.luckyRabbit.utils.Logger;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

@Getter
public abstract class LootboxItem {

    private final ItemStack item;
    private final String id;
    private final double chance;
    private final String rarity;
    private final RewardAction action;
    private final ConfigurationSection originalConfig;

    public LootboxItem(ItemStack item, String id, double chance, String rarity, RewardAction action, ConfigurationSection originalConfig) {
        this.item = item;
        this.id = id;
        this.chance = chance;
        this.rarity = rarity;
        this.action = action;
        this.originalConfig = originalConfig;
    }

    public static LootboxItem fromConfig(ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Configuration section cannot be null");
        }

        String id = section.getName();
        double chance = section.getDouble("chance", 100.0);
        String rarity = section.getString("rarity", "COMMON");
        RewardAction action = RewardAction.fromConfig(section.getConfigurationSection("action"));

        // Check for Oraxen item first
        String oraxenId = section.getString("oraxen_item");
        if (oraxenId != null) {
            // Create Oraxen item
            ItemBuilder itemBuilder = OraxenItems.getItemById(oraxenId);
            if (itemBuilder != null) {
                ItemStack oraxenItem = itemBuilder.build();

                // Apply any additional meta from config
                ConfigurationSection itemSection = section.getConfigurationSection("item");
                if (itemSection != null) {
                    applyItemMeta(oraxenItem, itemSection);
                }

                return new OraxenLootboxItem(oraxenItem, oraxenId, id, chance, rarity, section);
            } else {
                Logger.error("Failed to load Oraxen item: " + oraxenId);
            }
        }

        // If not Oraxen, create as Minecraft item
        ConfigurationSection itemSection = section.getConfigurationSection("item");
        if (itemSection == null) {
            throw new IllegalArgumentException("Missing item section in config");
        }

        // Get item properties
        String materialName = itemSection.getString("type", "STONE");
        int amount = itemSection.getInt("amount", 1);

        // Create the item
        ItemStack item = new ItemStack(org.bukkit.Material.valueOf(materialName.toUpperCase()), amount);

        // Apply metadata from config
        applyItemMeta(item, itemSection);

        return new MinecraftLootboxItem(item, id, chance, rarity, action, section);
    }

    protected static void applyItemMeta(ItemStack item, ConfigurationSection itemSection) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // Apply metadata from config
        if (itemSection.contains("meta")) {
            ConfigurationSection metaSection = itemSection.getConfigurationSection("meta");
            if (metaSection != null) {
                // Set display name with MiniMessage formatting
                if (metaSection.contains("display-name")) {
                    String displayName = metaSection.getString("display-name");
                    if (displayName != null) {
                        Component nameComponent = MiniMessage.miniMessage().deserialize(displayName)
                                .decoration(TextDecoration.ITALIC, false);
                        meta.displayName(nameComponent);
                    }
                }

                // Set lore with MiniMessage formatting
                if (metaSection.contains("lore")) {
                    List<String> configLore = metaSection.getStringList("lore");
                    List<Component> lore = configLore.stream()
                            .map(line -> MiniMessage.miniMessage().deserialize(line)
                                    .decoration(TextDecoration.ITALIC, false))
                            .collect(Collectors.toList());
                    meta.lore(lore);
                }
            }
        }

        item.setItemMeta(meta);
    }

    public ItemStack getDisplayItem() {
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();

            // Preserve original lore if it exists (especially for Oraxen items)
            if (meta.hasLore()) {
                lore.addAll(Objects.requireNonNull(meta.lore()));
            }

            // Add rarity and chance information
            lore.add(Component.empty());
            lore.add(Component.text("Rarity: ")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(rarity)
                            .color(RewardRarity.valueOf(rarity.toUpperCase()).getColor())
                            .decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text(String.format("Chance: %.1f%%", chance))
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    public void save(ConfigurationSection config) {
        config.set("id", id);
        config.set("chance", chance);
        config.set("rarity", rarity);

        // Let subclasses handle their specific save operations
        saveSpecific(config);
    }

    // Abstract method for subclass-specific save operations
    protected abstract void saveSpecific(ConfigurationSection config);
}
