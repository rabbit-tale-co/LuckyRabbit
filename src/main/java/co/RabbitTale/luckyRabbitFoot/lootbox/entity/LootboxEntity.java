package co.RabbitTale.luckyRabbitFoot.lootbox.entity;

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

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.lootbox.Lootbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LootboxEntity {
    private final LuckyRabbitFoot plugin;
    private final ArmorStand armorStand;
    private final String lootboxId;
    private final UUID uniqueId;
    private double time = 0;
    private BukkitTask animationTask;
    private BukkitTask particleTask;
    private boolean isAnimating = false;
    private static final double HOVER_HEIGHT = 0.15;
    private static final double ROTATION_SPEED = 0.05;

    public LootboxEntity(LuckyRabbitFoot plugin, Location location, Lootbox lootbox) {
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
        armorStand.setCollidable(false);

        // Reset initial rotation and position
        armorStand.setRotation(0, 0);
        armorStand.setHeadPose(new EulerAngle(0, 0, 0));

        // Set chest as head
        ItemStack chest = new ItemStack(Material.CHEST);
        armorStand.getEquipment().setHelmet(chest);

        // Lock equipment
        armorStand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);

        // Add metadata
        armorStand.setMetadata("LootboxEntity", new FixedMetadataValue(plugin, lootboxId));
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

                particleTime += 0.1;
                Location loc = armorStand.getLocation().add(0, 1, 0);

                // Spiral particles
                double radius = 0.5;
                for (int i = 0; i < 2; i++) {
                    double angle = particleTime * 2 + (i * Math.PI);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    loc.add(x, 0, z);
                    armorStand.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                    loc.subtract(x, 0, z);
                }

                // Random sparkles
                if (Math.random() < 0.3) {
                    double randomX = (Math.random() - 0.5) * 0.8;
                    double randomY = Math.random() * 0.5;
                    double randomZ = (Math.random() - 0.5) * 0.8;
                    armorStand.getWorld().spawnParticle(
                        Particle.SPELL_INSTANT,
                        loc.clone().add(randomX, randomY, randomZ),
                        1, 0, 0, 0, 0
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
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

    public String getLootboxId() {
        return lootboxId;
    }

    public Location getLocation() {
        return armorStand.getLocation();
    }

    public boolean isValid() {
        return !armorStand.isDead() && armorStand.isValid();
    }

    /**
     * Get the unique identifier for this entity
     * @return UUID of this entity
     */
    public UUID getUniqueId() {
        return uniqueId;
    }
}
