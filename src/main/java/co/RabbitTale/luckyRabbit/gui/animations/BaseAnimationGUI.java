package co.RabbitTale.luckyRabbit.gui.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import co.RabbitTale.luckyRabbit.lootbox.entity.LootboxEntity;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.DESCRIPTION_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.ITEM_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.SEPARATOR_COLOR;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.TARGET_COLOR;
import co.RabbitTale.luckyRabbit.gui.LootboxGUI;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.lootbox.items.LootboxItem;
import co.RabbitTale.luckyRabbit.lootbox.rewards.Reward;
import co.RabbitTale.luckyRabbit.lootbox.rewards.RewardRarity;
import co.RabbitTale.luckyRabbit.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public abstract class BaseAnimationGUI extends LootboxGUI {

    protected final Player player;
    protected final List<Reward> possibleRewards;
    protected final Reward finalReward;
    protected final Lootbox lootbox;
    protected int currentStep = 0;
    protected int totalSteps;
    protected List<Integer> delays;

    protected BaseAnimationGUI(LuckyRabbit plugin, Player player, Lootbox lootbox, int guiSize) {
        // First call super with temporary inventory
        super(plugin, Bukkit.createInventory(null, guiSize, Component.empty()));

        // Now create the proper inventory with this as holder
        this.inventory = Bukkit.createInventory(this, guiSize,
                Component.text("Opening: ")
                        .append(Component.text(PlainTextComponentSerializer.plainText()
                                .serialize(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName())))));

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
        // Get the original item and check for amount range
        ItemStack displayItem = item.getDisplayItem().clone();
        String amountStr = String.valueOf(displayItem.getAmount());
        int minAmount = displayItem.getAmount();
        int maxAmount = displayItem.getAmount();

        // Check if amount contains a range (e.g., "8-16")
        if (amountStr.contains("-")) {
            try {
                String[] range = amountStr.split("-");
                minAmount = Integer.parseInt(range[0]);
                maxAmount = Integer.parseInt(range[1]);
                // Generate random amount between min and max (inclusive)
                int randomAmount = minAmount + new Random().nextInt(maxAmount - minAmount + 1);
                displayItem.setAmount(randomAmount);
            } catch (Exception e) {
                Logger.error("Failed to parse item amount range: " + amountStr);
            }
        }

        // Add amount range to lore if it's a range
        if (minAmount != maxAmount) {
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();
                lore.add(0, Component.text("Amount: " + minAmount + "-" + maxAmount)
                        .color(NamedTextColor.GRAY));
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }
        }

        return new Reward(
                item,
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

        // Special effects for legendary items
        if (finalReward.rarity() == RewardRarity.LEGENDARY) {
            // Get the lootbox entity location
            Location lootboxLocation = null;
            for (LootboxEntity entity : plugin.getLootboxManager().getAllEntities()) {
                if (entity.getLootboxId().equals(lootbox.getId())) {
                    lootboxLocation = entity.getLocation();
                    break;
                }
            }

            // If we found the lootbox location, play effects there
            if (lootboxLocation != null) {
                final Location effectLocation = lootboxLocation.clone().add(0, 1, 0); // Slightly above the entity

                // Play special sounds at entity location for everyone to hear
                effectLocation.getWorld().playSound(effectLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                effectLocation.getWorld().playSound(effectLocation, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 2.0f);
                effectLocation.getWorld().playSound(effectLocation, Sound.ENTITY_WITHER_SPAWN, 0.3f, 2.0f);

                // Create particle effects
                new BukkitRunnable() {
                    double y = 0;
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks >= 40) { // 2 seconds of effects
                            this.cancel();
                            return;
                        }

                        Location particleLoc = effectLocation.clone().add(0, y, 0);

                        // Spiral effect
                        double radius = 1.5;
                        for (double degree = 0; degree < 360; degree += 20) {
                            double radian = Math.toRadians(degree);
                            double x = Math.cos(radian) * radius;
                            double z = Math.sin(radian) * radius;

                            Location spawnLoc = particleLoc.clone().add(x, 0, z);

                            // Dragon breath particles rising up
                            effectLocation.getWorld().spawnParticle(
                                Particle.DRAGON_BREATH,
                                spawnLoc,
                                1, 0, 0, 0, 0
                            );

                            // End rod particles for extra effect
                            effectLocation.getWorld().spawnParticle(
                                Particle.END_ROD,
                                spawnLoc,
                                1, 0, 0, 0, 0.05
                            );
                        }

                        // Lightning effect (visual only)
                        if (ticks % 5 == 0) {
                            effectLocation.getWorld().strikeLightningEffect(particleLoc);
                        }

                        y += 0.1;
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }

        // Give reward after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Get reward item and prepare for giving
            ItemStack rewardItem = finalReward.displayItem().clone();
            ItemMeta meta = rewardItem.getItemMeta();

            // Initialize rewardName with a default value
            Component rewardName = meta != null && meta.hasDisplayName() ?
                    MiniMessage.miniMessage().deserialize(PlainTextComponentSerializer.plainText()
                            .serialize(Objects.requireNonNull(meta.displayName()))) :
                    Component.text(rewardItem.getType().name());

            // Check original config for amount range
            ConfigurationSection itemSection = finalReward.item().getOriginalConfig() != null ?
                finalReward.item().getOriginalConfig().getConfigurationSection("item") : null;

            if (itemSection != null) {
                String amountStr = itemSection.getString("amount");
                if (amountStr != null && amountStr.contains("-")) {
                    try {
                        String[] range = amountStr.split("-");
                        int min = Integer.parseInt(range[0]);
                        int max = Integer.parseInt(range[1]);
                        int randomAmount = min + new Random().nextInt(max - min + 1);
                        rewardItem.setAmount(randomAmount);
                        Logger.debug("Generated random amount: " + randomAmount + " (range: " + min + "-" + max + ")");
                    } catch (Exception e) {
                        Logger.error("Failed to parse item amount range: " + amountStr);
                    }
                }
            }

            if (finalReward.action() != null) {
                // For virtual rewards, use display name and first lore line
                if (meta != null && meta.hasLore() && !Objects.requireNonNull(meta.lore()).isEmpty()) {
                    String firstLoreLine = PlainTextComponentSerializer.plainText()
                            .serialize(Objects.requireNonNull(meta.lore()).get(0));

                    // Extract the actual reward from lore (e.g., "Adds 1000 coins" -> "1000 coins")
                    String reward = firstLoreLine.replaceFirst(".*?([0-9]+.*?)$", "$1");

                    // Update rewardName for virtual rewards
                    rewardName = meta.hasDisplayName() ?
                            MiniMessage.miniMessage().deserialize(PlainTextComponentSerializer.plainText()
                                    .serialize(Objects.requireNonNull(meta.displayName()))) :
                            Component.text(reward).color(NamedTextColor.YELLOW);

                    // Execute the action
                    finalReward.action().execute(player);
                }
            } else {
                // For physical items, clean the lore and give the item
                if (meta != null && meta.hasLore()) {
                    List<Component> lore = new ArrayList<>(Objects.requireNonNull(meta.lore()));

                    // Remove amount range, chance and rarity lines
                    lore.removeIf(line -> {
                        String plainText = PlainTextComponentSerializer.plainText().serialize(line);
                        return plainText.startsWith("Amount:") ||
                               plainText.startsWith("Chance:") ||
                               plainText.startsWith("Rarity:") ||
                               plainText.isEmpty(); // Remove empty lines
                    });

                    // Remove any trailing empty lines
                    while (!lore.isEmpty() && PlainTextComponentSerializer.plainText()
                            .serialize(lore.get(lore.size() - 1)).isEmpty()) {
                        lore.remove(lore.size() - 1);
                    }

                    meta.lore(lore);
                    rewardItem.setItemMeta(meta);
                }

                // Give the cleaned item
                player.getInventory().addItem(rewardItem);
            }

            // Global broadcast message
            Component broadcastMessage = Component.text("» ")
                    .color(SEPARATOR_COLOR)
                    .append(Component.text(player.getName())
                            .color(TARGET_COLOR))
                    .append(Component.text(" has won ")
                            .color(DESCRIPTION_COLOR))
                    .append(Component.text(rewardItem.getAmount() + "x ")
                            .color(ITEM_COLOR))
                    .append(rewardName
                            .color(ITEM_COLOR))
                    .append(Component.text(" (")
                            .color(DESCRIPTION_COLOR))
                    .append(Component.text(finalReward.rarity().toString())
                            .color(finalReward.rarity().getColor()))
                    .append(Component.text(") from ")
                            .color(DESCRIPTION_COLOR))
                    .append(MiniMessage.miniMessage().deserialize(lootbox.getDisplayName()))
                    .append(Component.text("!")
                            .color(DESCRIPTION_COLOR));

            // Broadcast to all players
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(broadcastMessage);
            }

            // Increment open count and save
            lootbox.incrementOpenCount();
            plugin.getLootboxManager().saveLootbox(lootbox);

            player.closeInventory();
            isProcessingReward = false;
        }, 20L);
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

    // Make sure the inventory is properly associated with this GUI
    @Override
    public @NotNull Inventory getInventory() {
        if (inventory.getHolder() != this) {
            // If somehow the holder is wrong, create a new inventory with correct holder
            Component title = inventory.getViewers().isEmpty() ?
                Component.text("Opening Lootbox") : // Default title if no viewers
                inventory.getViewers().get(0).getOpenInventory().title(); // Get title from view

            Inventory newInv = Bukkit.createInventory(this, inventory.getSize(), title);
            newInv.setContents(inventory.getContents());
            this.inventory = newInv;
        }
        return inventory;
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
