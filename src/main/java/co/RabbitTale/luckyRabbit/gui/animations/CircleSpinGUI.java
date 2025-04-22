package co.RabbitTale.luckyRabbit.gui.animations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.rewards.Reward;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Circular spin animation that stops on the pre‑selected reward.
 */
public final class CircleSpinGUI extends BaseAnimationGUI {

    /* ──────────────── layout ──────────────── */
    private static final int GUI_SIZE = 45;
    private static final int WINNING_SLOT = 4;                 // top centre
    private static final int[] CIRCLE_SLOTS = { // clockwise
        3, 4, 5,
        11, 15,
        24, 33,
        41, 40, 39,
        29, 20
    };

    /* ───────── timing (≈3s) ───────── */
    private static final int FAST_LOOPS = 2;  // 1 tick / frame
    private static final int EASE_FRAMES = 10; // cubic slow‑down
    private static final int END_DELAY_TICKS = 6;  // last moving frame
    private static final int FINAL_FREEZE_TICKS = 5;  // pause at the end
    /* ────────────────────────────────── */

 /* runtime state */
    private final List<ItemStack> ring;
    private int headIndex = 0;
    // Backup of the ring items to handle potential threading issues
    private List<ItemStack> ringBackup = new ArrayList<>();
    // Static fallback items for worst-case scenario
    private static List<ItemStack> staticFallbackItems = null;
    // Flag to track if we're using fallback mode
    private boolean usingFallbackAnimation = false;
    // Store the arrow item to ensure it persists throughout animation
    private ItemStack bottomArrow = null;

    /* index of WINNING_SLOT inside CIRCLE_SLOTS                    */
    private static final int WIN_INDEX
            = java.util.stream.IntStream.range(0, CIRCLE_SLOTS.length)
                    .filter(i -> CIRCLE_SLOTS[i] == WINNING_SLOT)
                    .findFirst().orElse(0);

    /* ------------------------------------------------------------------ */
    public CircleSpinGUI(LuckyRabbit plugin, Player player, Lootbox lootbox) {
        super(plugin, player, lootbox, GUI_SIZE);
        this.ring = Collections.synchronizedList(new ArrayList<>());
    }

    /* ==================================================================
     *  BaseAnimationGUI hooks
     * ================================================================== */
    /**
     * Build static background & the spinning ring.
     */
    @Override
    protected void decorateGUI() {
        /* Total frames = fast laps + easing frames + 1 freeze frame */
        setTotalSteps(FAST_LOOPS * CIRCLE_SLOTS.length + EASE_FRAMES + 1);

        // Debug logging to track initialization
        plugin.getLogger().info("Initializing animation with totalSteps: " + totalSteps
                + ", rewards size: " + (possibleRewards != null ? possibleRewards.size() : "null")
                + ", finalReward: " + (finalReward != null ? "present" : "null"));

        synchronized (this) {
            buildRing();                           // inserts finalDisplayItem

            // Create a backup of the ring items
            ringBackup = new ArrayList<>(ring);

            // Also create a static backup that can't be affected by instance lifecycle
            staticFallbackItems = new ArrayList<>(ring);

            // Debug log ring size after building
            plugin.getLogger().info("Ring size after buildRing(): " + (ring != null ? ring.size() : "null")
                    + ", backup size: " + ringBackup.size()
                    + ", static size: " + (staticFallbackItems != null ? staticFallbackItems.size() : "null"));
        }

        fillEmptySlots(CIRCLE_SLOTS);
        for (int slot : CIRCLE_SLOTS) {
            inventory.setItem(slot, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        placeArrows(WINNING_SLOT, new String[]{"bottom"}, NamedTextColor.YELLOW);
        // Store the arrow item to ensure it persists
        bottomArrow = inventory.getItem(WINNING_SLOT + 9);
        plugin.getLogger().info("Stored bottom arrow item: " + (bottomArrow != null ? "success" : "null"));
    }

    /**
     * We supply our own delay list, so duration is unused.
     */
    @Override
    protected int getAnimationDuration() {
        return 0;
    }

    @Override
    List<Integer> generateDelays(int ignore1, int ignore2) {
        List<Integer> d = new ArrayList<>();
        for (int i = 0; i < FAST_LOOPS * CIRCLE_SLOTS.length; i++) {
            d.add(1);
        }
        d.addAll(cubicDelays(EASE_FRAMES, 1, END_DELAY_TICKS));
        return d;
    }

    /**
     * Draw one frame.
     */
    @Override
    protected void updateItems() {
        // If we're already in fallback mode, use the independent animation
        if (usingFallbackAnimation) {
            updateItemsFallbackMethod();
            return;
        }

        // First try using the primary ring list
        List<ItemStack> itemSource = null;

        // Try all possible sources in order of preference
        if (ring != null && !ring.isEmpty()) {
            itemSource = ring;
        } else if (ringBackup != null && !ringBackup.isEmpty()) {
            plugin.getLogger().warning("Primary ring is empty, using backup ring with size: " + ringBackup.size());
            itemSource = ringBackup;
        } else if (staticFallbackItems != null && !staticFallbackItems.isEmpty()) {
            plugin.getLogger().warning("Both primary and backup rings are empty, using static fallback with size: " + staticFallbackItems.size());
            itemSource = staticFallbackItems;
        }

        // If all sources are empty, switch to independent fallback animation
        if (itemSource == null || itemSource.isEmpty()) {
            plugin.getLogger().warning("All ring sources are empty - switching to independent fallback animation");
            usingFallbackAnimation = true;
            updateItemsFallbackMethod();
            return;
        }

        try {
            for (int i = 0; i < CIRCLE_SLOTS.length; i++) {
                int invSlot = CIRCLE_SLOTS[i];
                int ringIdx = (headIndex + i) % itemSource.size();
                ItemStack it = itemSource.get(ringIdx);

                if (it == null) {
                    it = new ItemStack(Material.BARRIER);
                }

                if (invSlot == WINNING_SLOT) {
                    it = addGlowEffect(it);
                }
                inventory.setItem(invSlot, it);
            }

            /* advance ring unless we are on the final (freeze) frame */
            if (currentStep < totalSteps - 1) {
                headIndex = (headIndex + 1) % itemSource.size();
            }

            // Reapply the arrow decoration after updating items
            if (bottomArrow != null) {
                inventory.setItem(WINNING_SLOT + 9, bottomArrow);
            }

            updateWhirlpoolGlass(currentStep);
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating items: " + e.getMessage() + "\n"
                    + "Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
            // Switch to fallback on error
            usingFallbackAnimation = true;
            updateItemsFallbackMethod();
        }
    }

    /**
     * Independent fallback animation that doesn't rely on the ring. This is
     * used when all other methods fail.
     */
    private void updateItemsFallbackMethod() {
        try {
            // Calculate animation progress (0.0 to 1.0)
            float progress = (float) currentStep / (float) totalSteps;

            // In fallback mode, we just rotate different colored glass
            // Use different colors based on frame
            Material[] materials = {
                Material.RED_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.GREEN_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE,
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE,
                Material.PINK_STAINED_GLASS_PANE,
                Material.BROWN_STAINED_GLASS_PANE
            };

            // Choose winning item for final frames or when animation is slowing down
            ItemStack winningItem;
            if (finalDisplayItem != null) {
                winningItem = finalDisplayItem;
            } else {
                winningItem = new ItemStack(Material.PAPER);
            }

            // Determine which slot should show the winning item
            boolean showFinalItem = progress > 0.90f; // Only in the very final stages

            // Update all circle slots
            for (int i = 0; i < CIRCLE_SLOTS.length; i++) {
                int invSlot = CIRCLE_SLOTS[i];

                // On the final frames, put prize item precisely in WINNING_SLOT
                if (showFinalItem && invSlot == WINNING_SLOT) {
                    inventory.setItem(invSlot, addGlowEffect(winningItem.clone()));
                } else {
                    // For all other slots, show rotating colors
                    int itemColorIndex = (currentStep * 2 + i) % materials.length;
                    inventory.setItem(invSlot, new ItemStack(materials[itemColorIndex]));
                }
            }

            // Reapply the arrow decoration after updating items
            if (bottomArrow != null) {
                inventory.setItem(WINNING_SLOT + 9, bottomArrow);
            } else if (currentStep == 0) {
                // If bottomArrow is null but this is the first frame, create a new arrow
                ItemStack arrow = createArrow("bottom", NamedTextColor.YELLOW);
                inventory.setItem(WINNING_SLOT + 9, arrow);
                bottomArrow = arrow;
            }

            // Update background whirlpool effect
            updateWhirlpoolGlass(currentStep);

            // Stop when we reach the end
            if (currentStep >= totalSteps - 1 && !isFinished) {
                finishAnimation();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in fallback animation: " + e.getMessage());
            // If even the fallback fails, just end the animation
            if (!isFinished) {
                finishAnimation();
            }
        }
    }

    @Override
    protected List<ItemStack> generateSpinSequence(int steps, int win) {
        return ring;   // not used elsewhere, but required by abstract
    }

    /* ==================================================================
     *  Helpers
     * ================================================================== */
    /**
     * Fills {@link #ring} so the pre‑selected reward lands in WINNING_SLOT.
     */
    private void buildRing() {
        if (ring == null) {
            // Ensure ring is initialized
            // Using reflection to access the final field if needed during parent constructor call
            try {
                java.lang.reflect.Field field = CircleSpinGUI.class.getDeclaredField("ring");
                field.setAccessible(true);
                field.set(this, new ArrayList<>());
                plugin.getLogger().info("Initialized ring via reflection");
            } catch (Exception e) {
                // If reflection fails, log the error but continue with workaround
                plugin.getLogger().severe("Error initializing ring: " + e.getMessage());
                return; // Avoid NPE by returning early
            }
        }

        ring.clear();

        // Check if possibleRewards is null or empty - this is critical
        if (possibleRewards == null || possibleRewards.isEmpty()) {
            plugin.getLogger().severe("possibleRewards list is null or empty - cannot build animation ring");
            // Create a fallback ring with default items to avoid division by zero
            createDefaultRing();
            return;
        }

        List<Reward> pool = new ArrayList<>(possibleRewards);

        // Check if finalReward is null
        if (finalReward != null) {
            pool.remove(finalReward);  // leave only fillers
        } else {
            plugin.getLogger().warning("finalReward is null - using all rewards as fillers");
        }

        // Check if we have enough rewards to build the ring
        if (pool.isEmpty()) {
            plugin.getLogger().severe("No filler rewards available for animation - using finalReward");
            // If there are no fillers but we have a finalReward, use it as the only item
            if (finalReward != null && finalDisplayItem != null) {
                createRingWithSingleItem(finalDisplayItem);
            } else {
                // Create a fallback ring with default items
                createDefaultRing();
            }
            return;
        }

        Random rnd = new Random();
        /* visible frames + items simultaneously visible on ring */
        int needed = totalSteps - 1 + CIRCLE_SLOTS.length;

        try {
            for (int i = 0; i < needed; i++) {
                ItemStack item = pool.get(rnd.nextInt(pool.size())).displayItem();
                if (item == null) {
                    item = new ItemStack(Material.BARRIER);  // Fallback if displayItem is null
                }
                ring.add(item);
            }

            /* slot that will appear in WINNING_SLOT on the final moving frame */
            int rewardPos = (totalSteps - 1 + WIN_INDEX - 1) % ring.size();

            // Check if finalDisplayItem is null
            if (finalDisplayItem != null) {
                ring.set(rewardPos, finalDisplayItem);          // << ONE shared item
            } else {
                plugin.getLogger().warning("finalDisplayItem is null - using PAPER");
                ring.set(rewardPos, new ItemStack(Material.PAPER));
            }

            // Log the position for debugging
            plugin.getLogger().info("Placed final reward at position " + rewardPos
                    + ", which should appear at WINNING_SLOT " + WINNING_SLOT
                    + " (WIN_INDEX: " + WIN_INDEX + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("Error building ring: " + e.getMessage());
            // If any exception occurs, create a default ring
            createDefaultRing();
        }

        plugin.getLogger().info("Built ring with " + ring.size() + " items");
    }

    /**
     * Creates a default ring with placeholder items when the normal ring
     * creation fails.
     */
    private void createDefaultRing() {
        synchronized (this) {
            ring.clear();
            // Add placeholder items
            for (int i = 0; i < CIRCLE_SLOTS.length * 2; i++) {
                ItemStack placeholder = new ItemStack(Material.BARRIER);
                ring.add(placeholder);
            }
            // Add a paper item to represent a prize
            ring.add(new ItemStack(Material.PAPER));
            plugin.getLogger().info("Created default fallback ring with " + ring.size() + " items");
        }
    }

    /**
     * Creates a ring filled with copies of a single item.
     */
    private void createRingWithSingleItem(ItemStack item) {
        synchronized (this) {
            ring.clear();
            // Create needed number of items (copies of the same item)
            int needed = totalSteps - 1 + CIRCLE_SLOTS.length;
            for (int i = 0; i < needed; i++) {
                ring.add(item.clone());
            }
            plugin.getLogger().info("Created ring with " + ring.size() + " copies of the same item");
        }
    }

    /**
     * Gracefully terminates the animation if errors occur.
     */
    private void stopAnimation() {
        plugin.getLogger().warning("Stopping animation due to errors");
        if (!isFinished) {
            finishAnimation();
        }
    }

    private static List<Integer> cubicDelays(int frames, int start, int end) {
        List<Integer> out = new ArrayList<>(frames);
        for (int i = 0; i < frames; i++) {
            double t = (double) i / (frames - 1);
            double eased = 1 - Math.pow(1 - t, 3);
            out.add(Math.max(1, (int) Math.round(start + eased * (end - start))));
        }
        return out;
    }

    /* whirlpool glass -------------------------------------------------- */
    private boolean isCircleSlot(int s) {
        for (int c : CIRCLE_SLOTS) {
            if (c == s) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createGlassPane(int idx) {
        return createGlassPane(GLASS_COLORS[idx % GLASS_COLORS.length], " ");
    }

    private void updateWhirlpoolGlass(int step) {
        final int cx = 4, cy = 2;
        final double speed = 0.06;
        for (int slot = 0; slot < GUI_SIZE; slot++) {
            if (isCircleSlot(slot) || slot == WINNING_SLOT + 9) { // Skip circle slots and arrow slot
                continue;
            }
            int x = slot % 9, y = slot / 9;
            double ang = Math.atan2(cy - y, x - cx);
            double base = (ang + Math.PI) / (2 * Math.PI);
            int idx = (int) (base * GLASS_COLORS.length
                    + step * speed * GLASS_COLORS.length)
                    % GLASS_COLORS.length;
            inventory.setItem(slot, createGlassPane(idx));
        }
    }

    /* slow‑down & freeze ---------------------------------------------- */
    @Override
    protected void animateSequence(int step) {
        if (step >= totalSteps) {
            plugin.getServer().getScheduler()
                    .runTaskLater(plugin, this::finishAnimation, FINAL_FREEZE_TICKS);
            return;
        }
        currentStep = step;
        updateItems();
        playTickSound();
        int delay = delays.get(Math.min(step, delays.size() - 1));
        plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> animateSequence(step + 1), delay);
    }
}
