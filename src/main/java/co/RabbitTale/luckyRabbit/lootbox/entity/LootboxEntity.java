package co.RabbitTale.luckyRabbit.lootbox.entity;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.utils.Logger;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LootboxEntity {

    private final LuckyRabbit plugin;
    private final ArmorStand armorStand;
    private final String lootboxId;
    @Getter
    private final UUID uniqueId;
    private double time = 0;
    private BukkitTask animationTask;
    private BukkitTask particleTask;
    private boolean isAnimating = false;
    private static final double HOVER_HEIGHT = 0.15;
    private static final double ROTATION_SPEED = 0.05;

    public LootboxEntity(LuckyRabbit plugin, Location location, Lootbox lootbox) {
        this.plugin = plugin;
        this.lootboxId = lootbox.getId();
        this.uniqueId = UUID.randomUUID();

        // Center the location
        location = location.getBlock().getLocation().add(0.5, 0, 0.5);

        // Create armor stand
        this.armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        setupArmorStand(lootbox);
        startAnimation();
        startParticleEffects();
    }

    private void setupArmorStand(Lootbox lootbox) {
        // Basic setup
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setInvulnerable(true);
        armorStand.setCustomNameVisible(true);

        // Convert display name to Component
        Component displayName = MiniMessage.miniMessage().deserialize(lootbox.getDisplayName());
        armorStand.customName(displayName);

        armorStand.setPersistent(true);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setMarker(false);
        armorStand.setSmall(false);
        armorStand.setBasePlate(false);
        armorStand.setCollidable(true);

        // Reset initial rotation and position
        armorStand.setRotation(0, 0);
        armorStand.setHeadPose(new EulerAngle(0, 0, 0));

        // Set chest as head
        ItemStack chest = new ItemStack(Material.CHEST);
        armorStand.getEquipment().setHelmet(chest);

        // Lock equipment
        armorStand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);

        // Clear any existing metadata first
        if (armorStand.hasMetadata("LootboxEntity")) {
            armorStand.removeMetadata("LootboxEntity", plugin);
        }

        // Add metadata with just the ID and log it
        armorStand.setMetadata("LootboxEntity", new FixedMetadataValue(plugin, lootboxId));
        Logger.debug("Set metadata for lootbox: " + lootboxId);
    }

    private void startAnimation() {
        if (isAnimating || armorStand.isDead() || !armorStand.isValid()) {
            return;
        }

        if (animationTask != null) {
            animationTask.cancel();
        }

        Location baseLocation = armorStand.getLocation().clone();
        time = 0;

        animationTask = new BukkitRunnable() {
            private double lastY = baseLocation.getY();

            @Override
            public void run() {
                if (armorStand.isDead() || !armorStand.isValid()) {
                    this.cancel();
                    isAnimating = false;
                    return;
                }

                // Update animation
                time += ROTATION_SPEED;
                double yOffset = Math.sin(time) * HOVER_HEIGHT;

                // Update position
                Location newLoc = baseLocation.clone();
                newLoc.setY(baseLocation.getY() + yOffset);

                // Only update if position changed significantly
                if (Math.abs(newLoc.getY() - lastY) > 0.001) {
                    armorStand.teleport(newLoc);
                    lastY = newLoc.getY();
                }

                // Update rotation
                armorStand.setHeadPose(new EulerAngle(0, time, 0));
            }
        }.runTaskTimer(plugin, 0L, 1L);

        isAnimating = true;
    }

    private void startParticleEffects() {
        particleTask = new BukkitRunnable() {
            private double particleTime = 0;

            @Override
            public void run() {
                if (armorStand.isDead() || !armorStand.isValid()) {
                    this.cancel();
                    return;
                }

                particleTime += 0.15;
                Location loc = armorStand.getLocation().add(0, 1.2, 0);

                // Create perfect circle with more points
                double radius = 0.4;
                int points = 4;
                for (int i = 0; i < points; i++) {
                    double angle = particleTime + ((2 * Math.PI * i) / points);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    // Spawn main circle particles
                    Location particleLoc = loc.clone().add(x, 0, z);
                    armorStand.getWorld().spawnParticle(
                            Particle.END_ROD,
                            particleLoc,
                            1,
                            0, 0, 0,
                            0
                    );

                    // Add trailing effect
                    double trailRadius = radius * 0.8;
                    double trailX = Math.cos(angle - 0.5) * trailRadius;
                    double trailZ = Math.sin(angle - 0.5) * trailRadius;
                    Location trailLoc = loc.clone().add(trailX, -0.1, trailZ);
                    armorStand.getWorld().spawnParticle(
                            Particle.SPELL_INSTANT,
                            trailLoc,
                            1,
                            0, 0, 0,
                            0
                    );
                }

                // Occasional sparkle effect
                if (Math.random() < 0.2) {
                    double randomAngle = Math.random() * 2 * Math.PI;
                    double randomRadius = Math.random() * radius;
                    double sparkleX = Math.cos(randomAngle) * randomRadius;
                    double sparkleY = Math.random() * 0.3;
                    double sparkleZ = Math.sin(randomAngle) * randomRadius;

                    armorStand.getWorld().spawnParticle(
                            Particle.SPELL_INSTANT,
                            loc.clone().add(sparkleX, sparkleY, sparkleZ),
                            1,
                            0, 0, 0,
                            0
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void remove() {
        if (animationTask != null) {
            animationTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        if (!armorStand.isDead()) {
            armorStand.remove();
        }
    }
}
