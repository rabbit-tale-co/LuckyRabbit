package co.RabbitTale.luckyRabbitFoot.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.Reward;

public class PinPointSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 27;
    private static final int CENTER_SLOT = 13;

    public PinPointSpinGUI(LuckyRabbitFoot plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(30);
    }

    @Override
    protected void decorateGUI() {
        fillEmptySlots(CENTER_SLOT);
    }

    @Override
    protected void updateItems() {
        if (currentStep >= totalSteps - 5) {
            inventory.setItem(CENTER_SLOT, finalReward.displayItem());
        } else {
            inventory.setItem(CENTER_SLOT, getRandomRewardItem());
        }
    }

    @Override
    protected int getAnimationDuration() {
        return 40;
    }

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
