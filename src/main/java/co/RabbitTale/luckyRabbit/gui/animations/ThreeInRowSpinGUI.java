package co.RabbitTale.luckyRabbit.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.rewards.Reward;

/**
 * Triple slot‑machine animation.<br>
 * If the three centre‑row symbols match the player wins the jackpot
 * ({@link BaseAnimationGUI#finalReward}); otherwise he gets a basic consolation
 * item (or nothing – tweak {@link #basicRewardItem}).
 */
public final class ThreeInRowSpinGUI extends BaseAnimationGUI {

    /* layout ------------------------------------------------------- */
    private static final int GUI_SIZE = 27;
    private static final int[] SLOT = {11, 13, 15};          // L C R
    private static final int SPIN_FRAMES = 35;                 // total ticks
    private static final int STOP_L = 25, STOP_C = 30, STOP_R = 34; // when each stops
    private static final int FINAL_FREEZE_TICKS = 20;         // longer pause at the end (1 second)

    /* jackpot logic ------------------------------------------------ */
    private static final double JACKPOT_CHANCE = 0.25;         // 25%
    private final boolean isJackpot;

    /* consolation item if not jackpot (set to null for "nothing")   */
    private final ItemStack basicRewardItem = new ItemStack(Material.COOKIE);

    /* runtime ------------------------------------------------------ */
    private Random rng;  // Not final so it can be initialized safely
    private final List<Reward> fillers = new ArrayList<>();
    private int frame = 0;

    public ThreeInRowSpinGUI(LuckyRabbit plugin, Player player, Lootbox box) {
        super(plugin, player, box, GUI_SIZE);
        setTotalSteps(SPIN_FRAMES);

        // Initialize Random object here to ensure it's not null
        this.rng = new Random();

        /* decide outcome BEFORE animation starts */
        this.isJackpot = rng.nextDouble() < JACKPOT_CHANCE;

        /* prepare a list of filler rewards that are NOT the jackpot */
        for (Reward r : possibleRewards) {
            if (!r.equals(finalReward)) {
                fillers.add(r);
            }
        }

        decorateGUI();
        generateDelays(totalSteps, 50);  // keeps quadratic ease (2.5s)
    }

    /* -------------------------------------------------------------- */
    @Override
    protected void decorateGUI() {
        fillEmptySlots(SLOT);   // black glass rest of GUI
        // blue separators (purely cosmetic)
        for (int s : new int[]{10, 12, 14, 16}) {
            inventory.setItem(s, new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        }
    }

    /* -------------------------------------------------------------- */
    @Override
    protected void updateItems() {
        try {
            /* left reel ------------------------------------------------ */
            if (frame < STOP_L) {
                inventory.setItem(SLOT[0], randomDisplayItem());
            } else if (frame == STOP_L) {
                inventory.setItem(SLOT[0],
                        isJackpot ? finalReward.displayItem()
                                : randomFillerItem());
            }

            /* centre reel --------------------------------------------- */
            if (frame < STOP_C) {
                inventory.setItem(SLOT[1], randomDisplayItem());
            } else if (frame == STOP_C) {
                inventory.setItem(SLOT[1], finalReward.displayItem()); // always stop on reward sprite
            }

            /* right reel ---------------------------------------------- */
            if (frame < STOP_R) {
                inventory.setItem(SLOT[2], randomDisplayItem());
            } else if (frame == STOP_R) {
                inventory.setItem(SLOT[2],
                        isJackpot ? finalReward.displayItem()
                                : randomFillerItem());
            }

            frame++;
        } catch (Exception e) {
            // Log the error and proceed safely
            Bukkit.getLogger().warning("Error updating slot machine items: " + e.getMessage());
            // If there's an error, attempt to recover by ending animation gracefully
            if (!isFinished) {
                finishAnimation();
            }
        }
    }

    /* -------------------------------------------------------------- */
    @Override
    protected void finishAnimation() {
        // This method is now a placeholder since we've split its functionality
        // Still set isFinished to true to indicate animation is complete
        isFinished = true;
    }

    /* -------------------------------------------------------------- */
    @Override
    protected int getAnimationDuration() {
        return 50;
    }

    @Override
    protected List<ItemStack> generateSpinSequence(int steps, int win) {
        return List.of();        // not used – reels spin independently
    }

    /* helpers ------------------------------------------------------ */
    private ItemStack randomFillerItem() {
        try {
            // Ensure rng is initialized
            if (rng == null) {
                rng = new Random();
            }

            return fillers.isEmpty()
                    ? randomDisplayItem()
                    : fillers.get(rng.nextInt(fillers.size())).displayItem();
        } catch (Exception e) {
            // Log the error and return a fallback item
            Bukkit.getLogger().warning("Error generating random filler item: " + e.getMessage());
            return new ItemStack(Material.BARRIER);
        }
    }

    /**
     * Returns a random item to display during the spinning animation. This
     * creates visual variety as the reels spin.
     *
     * @return A random ItemStack for display purposes
     */
    private ItemStack randomDisplayItem() {
        try {
            // Ensure rng is initialized
            if (rng == null) {
                rng = new Random();
            }

            // Possible display items for the spinning animation
            Material[] displayMaterials = {
                Material.DIAMOND,
                Material.EMERALD,
                Material.GOLD_INGOT,
                Material.GOLDEN_APPLE,
                Material.EXPERIENCE_BOTTLE,
                Material.ENDER_PEARL,
                Material.NETHERITE_INGOT,
                Material.TOTEM_OF_UNDYING,
                Material.ENCHANTED_BOOK,
                Material.NETHER_STAR
            };

            // Randomly select a material from the array
            Material randomMaterial = displayMaterials[rng.nextInt(displayMaterials.length)];

            // Create and return a new ItemStack with the random material
            return new ItemStack(randomMaterial);
        } catch (Exception e) {
            // Log the error and return a fallback item
            Bukkit.getLogger().warning("Error generating random display item: " + e.getMessage());
            return new ItemStack(Material.BARRIER);
        }
    }

    /**
     * Override the animation sequence to add a longer freeze at the end. This
     * makes the final result visible for longer before closing.
     */
    @Override
    protected void animateSequence(int step) {
        if (step >= totalSteps) {
            // Play success sounds immediately
            playSuccessSounds();

            // But delay the actual GUI closing
            Bukkit.getScheduler().runTaskLater(plugin, this::closeGUI, FINAL_FREEZE_TICKS);
            return;
        }

        // Continue with the normal animation flow
        currentStep = step;
        try {
            updateItems();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error in animation sequence: " + e.getMessage());
            // If there's an error, still try to continue
        }

        playTickSound();

        // Schedule next frame with calculated delay
        int delay = getDelayForStep(step);
        Bukkit.getScheduler().runTaskLater(plugin, () -> animateSequence(step + 1), delay);
    }

    /**
     * Plays the success sounds but doesn't close the GUI yet.
     */
    private void playSuccessSounds() {
        super.playTickSound(); // One last "pling"

        if (isJackpot) {
            // Play winning sound
            player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1.2f);
        } else {
            // Play "losing" sound
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1, 1);
        }
    }

    /**
     * Closes the GUI and gives the rewards.
     */
    private void closeGUI() {
        if (isJackpot) {
            // Give the real reward - don't play the sound again
            player.getInventory().addItem(finalReward.displayItem());
        } else if (basicRewardItem != null) {
            // Give consolation prize - don't play the sound again
            player.getInventory().addItem(basicRewardItem.clone());
        }

        player.closeInventory();
    }
}
