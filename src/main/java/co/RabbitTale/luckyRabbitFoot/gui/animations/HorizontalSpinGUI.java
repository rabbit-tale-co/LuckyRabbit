package co.RabbitTale.luckyRabbitFoot.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.Reward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HorizontalSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 27;
    private static final int WINNING_SLOT = 13; // Middle slot
    private static final int ITEMS_ROW_START = 9;
    private static final int ITEMS_ROW_END = 17;
    private static final Material[] GLASS_COLORS = {
        Material.RED_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE,
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE
    };
    private int glassColorIndex = 0;
    private List<ItemStack> spinSequence;
    private List<Integer> delays;

    public HorizontalSpinGUI(LuckyRabbitFoot plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(40);
        decorateGUI();
        initializeAnimation();
    }

    private void initializeAnimation() {
        // Generate sequence and delays
        this.spinSequence = generateSpinSequence(totalSteps, WINNING_SLOT);
        this.delays = generateDelays(totalSteps, 60);

        if (spinSequence == null) {
            throw new IllegalStateException("Failed to initialize animation sequence or delays");
        }
    }

    @Override
    protected void startAnimation() {
        if (spinSequence == null || delays == null) {
            initializeAnimation();
        }
        animateSpinSequence(0);
    }

    private void animateSpinSequence(int index) {
        if (index >= delays.size()) {
            finishAnimation();
            return;
        }

        // Update items in the middle row
        for (int i = ITEMS_ROW_START; i <= ITEMS_ROW_END; i++) {
            int sequenceIndex = index + i - ITEMS_ROW_START;
            if (sequenceIndex < spinSequence.size()) {
                ItemStack item = spinSequence.get(sequenceIndex);

                // Add glow effect to the winning slot
                if (i == WINNING_SLOT) {
                    item = addGlowEffect(item);
                }

                inventory.setItem(i, item);
            }
        }

        // Play sound with varying pitch
        playTickSound();
        updateGlassColors();
        showSelectedSlot();

        // Schedule next animation frame
        int delay = delays.get(index);
        Bukkit.getScheduler().runTaskLater(plugin, () -> animateSpinSequence(index + 1), delay);
    }

    @Override
    protected List<ItemStack> generateSpinSequence(int totalSteps, int winningSlot) {
        List<ItemStack> sequence = new ArrayList<>();
        List<Reward> availableRewards = new ArrayList<>(possibleRewards);
        availableRewards.remove(finalReward);
        Random random = new Random();

        int visibleItems = ITEMS_ROW_END - ITEMS_ROW_START + 1;
        int sequenceLength = totalSteps + visibleItems - 1;

        // Fill with random items
        for (int i = 0; i < sequenceLength - visibleItems; i++) {
            sequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
        }

        // Add final sequence ensuring winning item position
        for (int i = 0; i < visibleItems; i++) {
            if (ITEMS_ROW_START + i == WINNING_SLOT) {
                sequence.add(finalReward.displayItem());
            } else {
                sequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
            }
        }

        return sequence;
    }

    @Override
    protected void decorateGUI() {
        // Fill borders with glass panes
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i > 17 || i == 9 || i == 17) {
                inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
    }

    @Override
    protected void updateItems() {
        // Update items in the middle row
        for (int i = ITEMS_ROW_START; i <= ITEMS_ROW_END; i++) {
            ItemStack item;
            if (currentStep >= totalSteps - 5 && i == WINNING_SLOT) {
                item = finalReward.displayItem();
            } else {
                item = getRandomRewardItem();
            }

            if (i == WINNING_SLOT) {
                item = addGlowEffect(item);
            }

            inventory.setItem(i, item);
        }

        showSelectedSlot();
        updateGlassColors();
    }

    @Override
    protected int getAnimationDuration() {
        return 100; // 5 seconds (100 ticks)
    }

    private void showSelectedSlot() {
        // Add arrows pointing to the selected slot
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.displayName(Component.text("⬇ Selected Item ⬇").color(NamedTextColor.YELLOW));
        arrow.setItemMeta(arrowMeta);

        inventory.setItem(4, arrow); // Top arrow
        inventory.setItem(22, arrow); // Bottom arrow
    }

    private void updateGlassColors() {
        glassColorIndex = (glassColorIndex + 1) % GLASS_COLORS.length;

        // Top row
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createGlassPane((glassColorIndex + i) % GLASS_COLORS.length));
        }

        // Bottom row
        for (int i = 18; i < 27; i++) {
            inventory.setItem(i, createGlassPane((glassColorIndex + i) % GLASS_COLORS.length));
        }

        // Side columns
        inventory.setItem(ITEMS_ROW_START - 1, createGlassPane(glassColorIndex)); // Left border
        inventory.setItem(ITEMS_ROW_END + 1, createGlassPane(glassColorIndex)); // Right border
    }

    private ItemStack createGlassPane(int colorIndex) {
        ItemStack glass = new ItemStack(GLASS_COLORS[colorIndex]);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" ")); // Empty name
        glass.setItemMeta(meta);
        return glass;
    }
}
