package co.RabbitTale.luckyRabbitFoot.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.Reward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class CircleSpinGUI extends BaseAnimationGUI {

    private static final int GUI_SIZE = 54;
    // Slots for items in circle pattern
    private static final int[] CIRCLE_SLOTS = {
        12, 13, 14,    // Top row
        20, 24,        // Middle sides
        29, 33,        // Middle sides
        38, 42,        // Middle sides
        48, 49, 50     // Bottom row
    };
    private static final int WINNING_SLOT = 13; // Top middle slot

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

    public CircleSpinGUI(LuckyRabbitFoot plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        setTotalSteps(50);
        this.spinSequence = generateSpinSequence(totalSteps, WINNING_SLOT);
        this.delays = generateDelays(totalSteps, 80);
        decorateGUI();
    }

    @Override
    protected void startAnimation() {
        if (spinSequence == null || delays == null) {
            this.spinSequence = generateSpinSequence(totalSteps, WINNING_SLOT);
            this.delays = generateDelays(totalSteps, 80);
        }
        animateSpinSequence(0);
    }

    private void animateSpinSequence(int index) {
        if (index >= delays.size()) {
            finishAnimation();
            return;
        }

        // Calculate current position in sequence
        int sequenceIndex = index % CIRCLE_SLOTS.length;

        // Update all slots
        for (int i = 0; i < CIRCLE_SLOTS.length; i++) {
            int slot = CIRCLE_SLOTS[(i + sequenceIndex) % CIRCLE_SLOTS.length];
            ItemStack item;

            if (index >= totalSteps - 5 && slot == WINNING_SLOT) {
                item = finalReward.displayItem();
            } else {
                item = getRandomRewardItem();
            }

            if (slot == WINNING_SLOT) {
                item = addGlowEffect(item);
            }

            inventory.setItem(slot, item);
        }

        playTickSound();
        updateGlassColors();
        placeArrows();

        // Schedule next animation frame
        int delay = delays.get(index);
        new BukkitRunnable() {
            @Override
            public void run() {
                animateSpinSequence(index + 1);
            }
        }.runTaskLater(plugin, delay);
    }

    @Override
    protected void decorateGUI() {
        // Fill entire GUI with black glass first
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Set gray glass in corner slots
        for (int slot : CIRCLE_SLOTS) {
            inventory.setItem(slot, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Add arrows
        placeArrows();
    }

    @Override
    protected void updateItems() {
        updateCircleItems(currentStep);
    }

    private void updateCircleItems(int step) {
        for (int slot : CIRCLE_SLOTS) {
            ItemStack item = (step >= totalSteps - 5 && slot == WINNING_SLOT)
                ? finalReward.displayItem()
                : getRandomRewardItem();

            if (slot == WINNING_SLOT) {
                item = addGlowEffect(item);
            }

            inventory.setItem(slot, item);
        }

        updateGlassColors();
        placeArrows();
    }

    private void updateGlassColors() {
        glassColorIndex = (glassColorIndex + 1) % GLASS_COLORS.length;

        // Update glass colors around the circle area
        for (int i = 0; i < GUI_SIZE; i++) {
            if (!isCircleSlot(i) && !isCornerSlot(i)) {
                inventory.setItem(i, createGlassPane((glassColorIndex + i) % GLASS_COLORS.length));
            }
        }
    }

    private boolean isCircleSlot(int slot) {
        for (int circleSlot : CIRCLE_SLOTS) {
            if (circleSlot == slot) return true;
        }
        return false;
    }

    private boolean isCornerSlot(int slot) {
        for (int cornerSlot : CIRCLE_SLOTS) {
            if (cornerSlot == slot) return true;
        }
        return false;
    }

    private ItemStack createGlassPane(int colorIndex) {
        ItemStack glass = new ItemStack(GLASS_COLORS[colorIndex]);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" "));
        glass.setItemMeta(meta);
        return glass;
    }

    @Override
    protected int getAnimationDuration() {
        return 80;
    }

    @Override
    protected List<ItemStack> generateSpinSequence(int totalSteps, int winningSlot) {
        List<ItemStack> sequence = new ArrayList<>();
        List<Reward> availableRewards = new ArrayList<>(possibleRewards);
        availableRewards.remove(finalReward);
        Random random = new Random();

        // Generate sequence for spinning animation
        for (int i = 0; i < totalSteps; i++) {
            if (i >= totalSteps - 5 && i % CIRCLE_SLOTS.length == 0) {
                sequence.add(finalReward.displayItem());
            } else {
                sequence.add(availableRewards.get(random.nextInt(availableRewards.size())).displayItem());
            }
        }

        return sequence;
    }

    private ItemStack createArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.displayName(Component.text("⬇ Winning Item ⬇")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
        arrow.setItemMeta(arrowMeta);
        return arrow;
    }

    private void placeArrows() {
        ItemStack arrow = createArrow();
        inventory.setItem(4, arrow);   // Top arrow
        inventory.setItem(22, arrow);  // Bottom arrow
    }
}
