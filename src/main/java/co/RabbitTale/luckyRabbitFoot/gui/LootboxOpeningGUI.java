package co.RabbitTale.luckyRabbitFoot.gui;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.gui.animations.CircleSpinGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.HorizontalSpinGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.PinPointSpinGUI;
import co.RabbitTale.luckyRabbitFoot.gui.animations.ThreeInRowSpinGUI;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.animation.AnimationType;
import org.bukkit.entity.Player;

public class LootboxOpeningGUI {

    private final LuckyRabbitFoot plugin;
    private final Player player;
    private final Lootbox lootbox;

    public LootboxOpeningGUI(LuckyRabbitFoot plugin, Player player, Lootbox lootbox) {
        this.plugin = plugin;
        this.player = player;
        this.lootbox = lootbox;
    }

    public void open() {
        AnimationType type = plugin.getLootboxManager()
                .getLootbox(lootbox.getId())
                .getAnimationType();

        // Create appropriate GUI based on animation type
        LootboxGUI gui = switch (type) {
            case PIN_POINT ->
                new PinPointSpinGUI(plugin, player, lootbox);
            case CIRCLE ->
                new CircleSpinGUI(plugin, player, lootbox);
            case THREE_IN_ROW ->
                new ThreeInRowSpinGUI(plugin, player, lootbox);
            default ->
                new HorizontalSpinGUI(plugin, player, lootbox);
        };

        // Open the inventory
        player.openInventory(gui.getInventory());
    }
}
