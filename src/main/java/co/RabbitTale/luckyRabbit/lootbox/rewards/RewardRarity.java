package co.RabbitTale.luckyRabbit.lootbox.rewards;

import net.kyori.adventure.text.format.TextColor;

/*
 * RewardRarity.java
 *
 * Enum defining possible reward rarity levels.
 * Each rarity has a display name and color.
 * Used for visual feedback in GUIs and messages.
 *
 * Rarities (from common to legendary):
 * - COMMON: Gray (#B4B4B4)
 * - UNCOMMON: Green (#78E678)
 * - RARE: Blue (#64B4FF)
 * - EPIC: Purple (#E678E6)
 * - LEGENDARY: Gold (#FFC850)
 */
public enum RewardRarity {
    COMMON("Common", TextColor.color(180, 180, 180)),
    UNCOMMON("Uncommon", TextColor.color(120, 230, 120)),
    RARE("Rare", TextColor.color(100, 180, 255)),
    EPIC("Epic", TextColor.color(230, 120, 230)),
    LEGENDARY("Legendary", TextColor.color(255, 200, 80));

    private final String displayName;
    private final TextColor color;

    /**
     * Creates a new rarity level.
     *
     * @param displayName Name shown in GUI
     * @param color Color used for display
     */
    RewardRarity(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * Gets the display name of this rarity.
     *
     * @return Formatted display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the color for this rarity. Used in GUIs and messages.
     *
     * @return Adventure API TextColor
     */
    public TextColor getColor() {
        return color;
    }
}
 