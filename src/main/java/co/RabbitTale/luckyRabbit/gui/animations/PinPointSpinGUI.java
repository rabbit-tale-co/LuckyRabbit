package co.RabbitTale.luckyRabbit.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.rewards.Reward;

/*
 * PinPointSpinGUI.java
 *
 * Single slot animation where items rapidly change.
 * Premium feature only.
 *
 * Features:
 * - Single slot focus
 * - Rapid item cycling
 * - Center position emphasis
 * - Simple border design
 *
 * Layout:
 * - 3x9 inventory
 * - Single center slot (13)
 * - Static border
 * - Minimal decoration
 */
public class PinPointSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 27;
    private static final int CENTER_SLOT = 13;

    /**
     * Creates a new pin-point animation GUI.
     *
     * @param plugin Plugin instance
     * @param player Player viewing the animation
     * @param lootbox Lootbox being opened
     */
    public PinPointSpinGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(30);
    }

    /**
     * Sets up the GUI decoration. Creates simple border around center slot.
     */
    @Override
    protected void decorateGUI() {
        fillEmptySlots(CENTER_SLOT);
    }

    /**
     * Updates the animation frame. Changes item in center slot. Shows final
     * reward in last 5 steps.
     */
    @Override
    protected void updateItems() {
        if (currentStep >= totalSteps - 5) {
            inventory.setItem(CENTER_SLOT, finalReward.displayItem());
        } else {
            inventory.setItem(CENTER_SLOT, getRandomRewardItem());
        }
    }

    /**
     * Gets the animation duration in ticks.
     *
     * @return Duration in ticks (40 = 2 seconds)
     */
    @Override
    protected int getAnimationDuration() {
        return 40;
    }

    /**
     * Generates the sequence of items for the animation. Creates a sequence of
     * random items with final reward at end.
     *
     * @param totalSteps Total animation steps
     * @param winningSlot Slot where winning item will land
     * @return List of items for animation
     */
    @Override
    protected List<ItemStack> generateSpinSequence(int totalSteps, int winningSlot) {
        List<ItemStack> spinSequence = new ArrayList<>();
        List<Reward> availableRewards = new ArrayList<>(possibleRewards);
        availableRewards.remove(finalReward);
        Random random = new Random();

        // For pin-point animation, we only need a sequence of single items
        for (int i = 0; i < totalSteps - 1; i++) {
            spinSequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
        }

        // Add final reward at the end
        spinSequence.add(finalReward.displayItem());

        return spinSequence;
    }
}
