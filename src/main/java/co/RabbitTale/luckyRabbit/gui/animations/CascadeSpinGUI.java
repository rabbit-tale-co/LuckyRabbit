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
 * CascadeSpinGUI.java
 *
 * Waterfall-style animation.
 * Items flow down in a cascading pattern.
 * Premium feature only.
 *
 * Features:
 * - Cascading item movement
 * - Multi-row animation
 * - Synchronized timing
 * - Flow direction control
 *
 * Layout:
 * - 6x9 display grid
 * - Multiple animation rows
 * - Flow indicators
 * - Border decoration
 */
public class CascadeSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 54;
    private static final int[][] SLOTS = {
        {10, 11, 12, 13, 14, 15, 16}, // Top row
        {19, 20, 21, 22, 23, 24, 25}, // Second row
        {28, 29, 30, 31, 32, 33, 34}, // Third row
        {37, 38, 39, 40, 41, 42, 43}  // Bottom row
    };

    /**
     * Creates a new cascade animation GUI.
     *
     * @param plugin Plugin instance
     * @param player Player viewing the animation
     * @param lootbox Lootbox being opened
     */
    public CascadeSpinGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(60);
    }

    /**
     * Sets up the GUI decoration.
     * Creates border and flow indicators.
     */
    @Override
    protected void decorateGUI() {
        fillEmptySlots();
    }

    /**
     * Updates the animation frame.
     * Handles cascading item movement.
     * Shows final reward in center slot at end.
     */
    @Override
    protected void updateItems() {
        for (int row = 0; row < SLOTS.length; row++) {
            for (int col = 0; col < SLOTS[row].length; col++) {
                int slot = SLOTS[row][(col + currentStep + row) % SLOTS[row].length];
                inventory.setItem(slot, getRandomRewardItem());
            }
        }

        if (currentStep >= totalSteps - 5) {
            inventory.setItem(31, finalReward.displayItem());
        }
    }

    /**
     * Gets the animation duration in ticks.
     *
     * @return Duration in ticks (100 = 5 seconds)
     */
    @Override
    protected int getAnimationDuration() {
        return 100;
    }

    /**
     * Generates the sequence of items for the animation.
     * Creates a cascading pattern with final reward.
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

        // Calculate total items needed for the cascade animation
        int totalSlots = 0;
        for (int[] row : SLOTS) {
            totalSlots += row.length;
        }
        int sequenceLength = totalSteps + totalSlots - 1;

        // Fill the spin sequence with random items
        for (int i = 0; i < sequenceLength - totalSlots; i++) {
            spinSequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
        }

        // Add the final items to ensure the winning item ends up in the correct position
        for (int i = 0; i < totalSlots; i++) {
            if (i == winningSlot) {
                spinSequence.add(finalReward.displayItem());
            } else {
                spinSequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
            }
        }

        return spinSequence;
    }
}
