package co.RabbitTale.luckyRabbitFoot.lootbox.rewards;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

@Getter
public enum RewardRarity {
    COMMON("Common", TextColor.color(180, 180, 180)),
    UNCOMMON("Uncommon", TextColor.color(120, 230, 120)),
    RARE("Rare", TextColor.color(100, 180, 255)),
    EPIC("Epic", TextColor.color(230, 120, 230)),
    LEGENDARY("Legendary", TextColor.color(255, 200, 80));

    private final String displayName;
    private final TextColor color;

    RewardRarity(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

}
