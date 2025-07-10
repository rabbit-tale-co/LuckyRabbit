package co.RabbitTale.luckyRabbit.lootbox.animation;

import org.bukkit.entity.Player;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.gui.animations.*;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;

public enum AnimationType {
    HORIZONTAL {
        @Override
        public BaseAnimationGUI createGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
            return new HorizontalSpinGUI(plugin, player, lootbox);
        }
    },
    CIRCLE {
        @Override
        public BaseAnimationGUI createGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
            return new CircleSpinGUI(plugin, player, lootbox);
        }
    },
    CASCADE {
        @Override
        public BaseAnimationGUI createGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
            return new CascadeSpinGUI(plugin, player, lootbox);
        }
    },
    PIN_POINT {
        @Override
        public BaseAnimationGUI createGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
            return new PinPointSpinGUI(plugin, player, lootbox);
        }
    },
    THREE_IN_ROW {
        @Override
        public BaseAnimationGUI createGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
            return new ThreeInRowSpinGUI(plugin, player, lootbox);
        }
    };

    public abstract BaseAnimationGUI createGUI(LuckyRabbit plugin, Player player, Lootbox lootbox);
}
