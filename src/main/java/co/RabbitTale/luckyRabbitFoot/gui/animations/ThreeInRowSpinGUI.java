package co.RabbitTale.luckyRabbitFoot.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.Reward;

public class ThreeInRowSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 27;
    private static final int[] ROW_SLOTS = {11, 13, 15}; // Środkowy rząd z trzema slotami
    private static final int FINAL_REWARD_SLOT = 13; // Środkowy slot

    public ThreeInRowSpinGUI(LuckyRabbitFoot plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(35);
    }

    @Override
    protected void decorateGUI() {
        // Wypełnij GUI czarnymi szybami, pozostawiając miejsca na przedmioty
        fillEmptySlots(ROW_SLOTS);

        // Dodaj dekoracyjne szyby wokół slotów
        ItemStack decorativeGlass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        for (int slot : new int[]{10, 12, 14, 16}) { // Szyby po bokach przedmiotów
            inventory.setItem(slot, decorativeGlass);
        }
    }

    @Override
    protected void updateItems() {
        // Aktualizuj przedmioty w trzech slotach
        for (int slot : ROW_SLOTS) {
            if (currentStep >= totalSteps - 5 && slot == FINAL_REWARD_SLOT) {
                // Pokaż finalną nagrodę w środkowym slocie
                inventory.setItem(slot, finalReward.displayItem());
            } else {
                // Losowe przedmioty w pozostałych slotach
                inventory.setItem(slot, getRandomRewardItem());
            }
        }
    }

    @Override
    protected int getAnimationDuration() {
        return 50; // 2.5 sekundy
    }

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
