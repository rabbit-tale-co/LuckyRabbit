package co.RabbitTale.luckyRabbitFoot.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.gui.LootboxGUI;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import co.RabbitTale.luckyRabbitFoot.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.Reward;
import co.RabbitTale.luckyRabbitFoot.lootbox.rewards.RewardRarity;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import static co.RabbitTale.luckyRabbitFoot.commands.LootboxCommand.*;

public abstract class BaseAnimationGUI extends LootboxGUI {

    protected final Player player;
    protected final List<Reward> possibleRewards;
    protected final Reward finalReward;
    protected final Lootbox lootbox;
    protected int currentStep = 0;
    protected int totalSteps;
    protected List<Integer> delays;
    protected boolean isShuffling = true;

    protected BaseAnimationGUI(LuckyRabbitFoot plugin, Player player, Lootbox lootbox, int guiSize) {
        super(plugin, Bukkit.createInventory(null, guiSize,
                Component.text("Opening: ")
                        .append(Component.text(PlainTextComponentSerializer.plainText()
                                .serialize(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))))));
        this.player = player;
        this.lootbox = lootbox;

        // Convert LootboxItems to Rewards
        this.possibleRewards = new ArrayList<>(lootbox.getItems().values().stream()
                .map(this::convertToReward)
                .toList());

        if (possibleRewards.isEmpty()) {
            throw new IllegalStateException("No rewards available in lootbox!");
        }

        this.finalReward = selectFinalReward();

        // Initialize delays list before starting animation
        this.delays = new ArrayList<>();

        // Set default total steps if not set by child class
        if (this.totalSteps <= 0) {
            this.totalSteps = 40; // Default value
        }

        decorateGUI();
        setupAnimation();
    }

    protected void setTotalSteps(int steps) {
        if (steps <= 0) {
            throw new IllegalArgumentException("Total steps must be greater than 0");
        }
        this.totalSteps = steps;
    }

    private Reward convertToReward(LootboxItem item) {
        return new Reward(
                item.getItem(),
                item.getChance(),
                RewardRarity.valueOf(item.getRarity().toUpperCase()),
                item.getAction()
        );
    }

    private void setupAnimation() {
        if (totalSteps <= 0) {
            throw new IllegalStateException("Total steps must be set before starting animation!");
        }

        // Generate delays based on animation duration
        this.delays = generateDelays(totalSteps, getAnimationDuration());

        if (delays.isEmpty()) {
            throw new IllegalStateException("No animation delays generated!");
        }

        startAnimation();
    }

    List<Integer> generateDelays(int totalSteps, int totalDuration) {
        List<Integer> delays = new ArrayList<>();
        double[] cumulativeTimes = new double[totalSteps + 1];

        for (int i = 0; i <= totalSteps; i++) {
            double t = (double) i / totalSteps;
            double easedT = t * t; // Quadratic easing
            cumulativeTimes[i] = totalDuration * easedT;
            if (i > 0) {
                int delay = (int) Math.round(cumulativeTimes[i] - cumulativeTimes[i - 1]);
                delays.add(Math.max(delay, 1));
            }
        }
        return delays;
    }

    ItemStack addGlowEffect(ItemStack item) {
        ItemStack glowingItem = item.clone();
        ItemMeta meta = glowingItem.getItemMeta();
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        glowingItem.setItemMeta(meta);
        return glowingItem;
    }

    private Reward selectFinalReward() {
        if (possibleRewards.isEmpty()) {
            throw new IllegalStateException("No rewards available!");
        }

        double totalWeight = possibleRewards.stream()
                .mapToDouble(Reward::chance)
                .sum();

        double random = Math.random() * totalWeight;
        double currentWeight = 0;

        for (Reward reward : possibleRewards) {
            currentWeight += reward.chance();
            if (currentWeight >= random) {
                return reward;
            }
        }

        return possibleRewards.get(0);
    }

    protected void startAnimation() {
        if (delays.isEmpty()) {
            throw new IllegalStateException("Animation delays not initialized!");
        }

        Bukkit.getScheduler().runTaskTimer(plugin, (task) -> {
            if (currentStep >= totalSteps) {
                task.cancel();
                finishAnimation();
                return;
            }

            updateItems();
            playTickSound();
            currentStep++;
        }, 0L, Math.max(1, delays.get(Math.min(currentStep, delays.size() - 1))));
    }

    protected void finishAnimation() {
        isFinished = true;
        isProcessingReward = true;

        // Play winning sounds
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);

        // Give reward after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Get reward name based on metadata
            Component rewardName;
            ItemStack rewardItem = finalReward.displayItem();
            ItemMeta meta = rewardItem.getItemMeta();

            if (finalReward.action() != null) {
                // For virtual rewards, use display name and first lore line
                if (meta != null && meta.hasLore() && !Objects.requireNonNull(meta.lore()).isEmpty()) {
                    String firstLoreLine = PlainTextComponentSerializer.plainText()
                        .serialize(Objects.requireNonNull(meta.lore()).get(0));

                    // Extract the actual reward from lore (e.g., "Adds 1000 coins" -> "1000 coins")
                    String reward = firstLoreLine.replaceFirst(".*?([0-9]+.*?)$", "$1");

                    // Use MiniMessage to parse the display name with color codes
                    rewardName = meta.hasDisplayName() ?
                        MiniMessage.miniMessage().deserialize(PlainTextComponentSerializer.plainText()
                            .serialize(Objects.requireNonNull(meta.displayName()))) :
                        Component.text(reward).color(NamedTextColor.YELLOW);
                } else {
                    rewardName = meta != null && meta.hasDisplayName() ?
                        MiniMessage.miniMessage().deserialize(PlainTextComponentSerializer.plainText()
                            .serialize(Objects.requireNonNull(meta.displayName()))) :
                        Component.text(rewardItem.getType().name());
                }
            } else {
                // For physical items, use item display name with color support
                rewardName = meta != null && meta.hasDisplayName() ?
                    MiniMessage.miniMessage().deserialize(PlainTextComponentSerializer.plainText()
                        .serialize(Objects.requireNonNull(meta.displayName()))) :
                    Component.text(rewardItem.getType().name());
            }

            // Message for winner
            assert rewardName != null;
            Component winnerMessage = Component.text("You won ")
                .color(DESCRIPTION_COLOR)
                .append(rewardName)
                .append(Component.text("!")
                    .color(DESCRIPTION_COLOR));
            player.sendMessage(winnerMessage);

            // Global broadcast message
            Component broadcastMessage = Component.text("» ")
                .color(SEPARATOR_COLOR)
                .append(Component.text(player.getName())
                    .color(TARGET_COLOR))
                .append(Component.text(" has won ")
                    .color(DESCRIPTION_COLOR))
                .append(rewardName)
                .append(Component.text(" from ")
                    .color(DESCRIPTION_COLOR))
                .append(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))
                .append(Component.text("!")
                    .color(DESCRIPTION_COLOR));

            // Broadcast to all players
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(broadcastMessage);
            }

            // Give the reward
            finalReward.give(player);

            // Increment open count and save
            lootbox.incrementOpenCount();
            plugin.getLootboxManager().saveLootbox(lootbox);

            player.closeInventory();
            isProcessingReward = false;
        }, 20L);
    }

    // Helper class to get color from component
    private static class TextColorGetter {
        public static TextColor getColorFromComponent(Component component) {
            if (component.color() != null) {
                return component.color();
            }
            return NamedTextColor.YELLOW; // Default color
        }
    }

    protected void playTickSound() {
        float pitch = 0.5f + (1.5f * (1.0f - ((float) currentStep / totalSteps)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, pitch);
    }

    protected ItemStack getRandomRewardItem() {
        if (possibleRewards.isEmpty()) {
            return new ItemStack(Material.BARRIER); // Fallback item
        }
        return possibleRewards.get(new Random().nextInt(possibleRewards.size())).displayItem();
    }

    protected void fillEmptySlots(int... excludedSlots) {
        outer:
        for (int i = 0; i < inventory.getSize(); i++) {
            for (int excluded : excludedSlots) {
                if (i == excluded) {
                    continue outer;
                }
            }
            inventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    // Abstract methods that must be implemented by specific animations
    protected abstract void decorateGUI();

    protected abstract int getAnimationDuration(); // Duration in ticks

    public void show() {
        player.openInventory(inventory);
    }

    protected abstract void updateItems();

    protected abstract List<ItemStack> generateSpinSequence(int totalSteps, int winningSlot);
}
