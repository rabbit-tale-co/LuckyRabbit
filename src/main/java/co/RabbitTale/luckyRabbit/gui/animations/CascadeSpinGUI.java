package co.RabbitTale.luckyRabbit.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.rewards.Reward;

public class CascadeSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 54;
    private static final int[][] SLOTS = {
        {10, 11, 12, 13, 14, 15, 16},
        {19, 20, 21, 22, 23, 24, 25},
        {28, 29, 30, 31, 32, 33, 34},
        {37, 38, 39, 40, 41, 42, 43}
    };

    public CascadeSpinGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(60);
    }

    @Override
    protected void decorateGUI() {
        fillEmptySlots();
    }

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

    @Override
    protected int getAnimationDuration() {
        return 100;
    }

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
