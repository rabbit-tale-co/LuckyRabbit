package co.RabbitTale.luckyRabbit.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.rewards.Reward;

/*
 * ThreeInRowSpinGUI.java
 *
 * Triple slot machine style animation.
 * Three slots spin independently and stop in sequence.
 * Premium feature only.
 *
 * Features:
 * - Three independent slots
 * - Sequential stopping
 * - Center slot emphasis
 * - Synchronized timing
 *
 * Layout:
 * - 3x9 display area
 * - Three main slots (11, 13, 15)
 * - Decorative glass borders
 * - Final reward in center slot
 */
public class ThreeInRowSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 27;
    private static final int[] ROW_SLOTS = {11, 13, 15}; // Middle row with three slots
    private static final int FINAL_REWARD_SLOT = 13; // Center slot

    /**
     * Creates a new three-in-row animation GUI.
     *
     * @param plugin Plugin instance
     * @param player Player viewing the animation
     * @param lootbox Lootbox being opened
     */
    public ThreeInRowSpinGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(35);
    }

    /**
     * Sets up the GUI decoration. Creates glass pane border and slot markers.
     */
    @Override
    protected void decorateGUI() {
        // Fill GUI with gray glass
        fillEmptySlots(ROW_SLOTS);

        // Add glass
        ItemStack decorativeGlass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        for (int slot : new int[]{10, 12, 14, 16}) {
            inventory.setItem(slot, decorativeGlass);
        }
    }

    /**
     * Updates the animation frame. Handles item movement and slot updates.
     */
    @Override
    protected void updateItems() {
        for (int slot : ROW_SLOTS) {
            if (currentStep >= totalSteps - 5 && slot == FINAL_REWARD_SLOT) {
                // Show final reward in middle slot
                inventory.setItem(slot, finalReward.displayItem());
            } else {
                // Random items
                inventory.setItem(slot, getRandomRewardItem());
            }
        }
    }

    /**
     * Gets the animation duration in ticks.
     *
     * @return Duration in ticks (50 = 2.5 seconds)
     */
    @Override
    protected int getAnimationDuration() {
        return 50;
    }

    /**
     * Generates the sequence of items for the animation.
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

        // Calculate total items needed for three slots
        int sequenceLength = totalSteps + ROW_SLOTS.length - 1;

        // Fill the spin sequence with random items
        for (int i = 0; i < sequenceLength - ROW_SLOTS.length; i++) {
            spinSequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
        }

        // Add the final items to ensure the winning item ends up in the middle slot
        for (int i = 0; i < ROW_SLOTS.length; i++) {
            if (i == 1) { // Middle slot (index 1 in ROW_SLOTS array)
                spinSequence.add(finalReward.displayItem());
            } else {
                spinSequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
            }
        }

        return spinSequence;
    }
}
