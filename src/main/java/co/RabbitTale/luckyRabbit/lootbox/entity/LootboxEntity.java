package co.RabbitTale.luckyRabbit.lootbox.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.utils.Logger;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class LootboxEntity {

    private final LuckyRabbit plugin;
    private final ArmorStand armorStand;
    private final String lootboxId;
    private final UUID uniqueId;
    private double time = 0;
    private BukkitTask animationTask;
    private BukkitTask particleTask;
    private boolean isAnimating = false;
    private static final double HOVER_HEIGHT = 0.15;
    private static final double ROTATION_SPEED = 0.05;
    private final List<ArmorStand> nameStands = new ArrayList<>();
    private final List<Double> yOffsets = new ArrayList<>();

    public LootboxEntity(LuckyRabbit plugin, Location location, Lootbox lootbox) {
        this.plugin = plugin;
        this.lootboxId = lootbox.getId();
        this.uniqueId = UUID.randomUUID();

        // Center the location
        location = location.getBlock().getLocation().add(0.5, -0.5, 0.5);

        // Create main armor stand without name
        this.armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        setupMainArmorStand(lootbox);

        // Calculate total content height
        List<String> descriptions = lootbox.getDescriptions();
        int totalLines = 1 + descriptions.size(); // title + descriptions
        double lineHeight = 0.25; // height between lines
        double totalHeight = totalLines * lineHeight;

        // Start from calculated height above chest
        double baseNameY = location.getY() + 2.0; // Base height above chest

        // Title at top
        ArmorStand titleStand = spawnNameStand(
                new Location(location.getWorld(), location.getX(), baseNameY + totalHeight, location.getZ()),
                lootbox.getTitle()
        );
        nameStands.add(titleStand);
        yOffsets.add(baseNameY + totalHeight - location.getY());

        // Descriptions (max 5) below title
        double currentY = baseNameY + totalHeight - lineHeight; // Start below title
        for (int i = 0; i < Math.min(5, descriptions.size()); i++) {
            ArmorStand descStand = spawnNameStand(
                    new Location(location.getWorld(), location.getX(), currentY, location.getZ()),
                    descriptions.get(i)
            );
            nameStands.add(descStand);
            yOffsets.add(currentY - location.getY());
            currentY -= lineHeight;
        }

        startAnimation();
        startParticleEffects();
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getLootboxId() {
        return lootboxId;
    }

    private void setupMainArmorStand(Lootbox lootbox) {
        // Basic setup
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setInvulnerable(true);
        armorStand.setCustomNameVisible(false); // No name on main
        armorStand.customName(null);

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

        // Store both the lootbox ID and entity UUID in metadata
        armorStand.setMetadata("LootboxEntity", new FixedMetadataValue(plugin, lootboxId));
        armorStand.setMetadata("LootboxEntityUUID", new FixedMetadataValue(plugin, uniqueId.toString()));

        Logger.debug("Set metadata for lootbox: " + lootboxId + " with UUID: " + uniqueId);
    }

    private ArmorStand spawnNameStand(Location loc, String text) {
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.customName(MiniMessage.miniMessage().deserialize(text));
        stand.setMetadata("LootboxEntity", new FixedMetadataValue(plugin, lootboxId));
        stand.setMetadata("LootboxEntityUUID", new FixedMetadataValue(plugin, uniqueId.toString()));
        return stand;
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

                // Update main position
                Location newLoc = baseLocation.clone();
                newLoc.setY(baseLocation.getY() + yOffset);

                // Only update if position changed significantly
                if (Math.abs(newLoc.getY() - lastY) > 0.001) {
                    armorStand.teleport(newLoc);
                    // Update name stands positions
                    for (int i = 0; i < nameStands.size(); i++) {
                        Location nameLoc = newLoc.clone();
                        nameLoc.setY(newLoc.getY() + yOffsets.get(i));
                        nameStands.get(i).teleport(nameLoc);
                    }
                    lastY = newLoc.getY();
                }

                // Update rotation (only main)
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
        // Remove name stands first
        for (ArmorStand stand : nameStands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        nameStands.clear();
        yOffsets.clear();
        if (armorStand != null && !armorStand.isDead()) {
            armorStand.remove();
        }
    }

    public void show(Player player) {
        // Get lootbox ID from the armorstand's metadata
        String lootboxId = armorStand.getMetadata("LootboxEntity").get(0).asString();

        // Always show for admins, hide for non-admins if it's an example lootbox
        if (plugin.getLootboxManager().isExampleLootbox(lootboxId) && !player.hasPermission("luckyrabbit.admin")) {
            armorStand.setCustomNameVisible(false);
            armorStand.setVisible(false);
        } else {
            // Show for admins and non-example lootboxes
            armorStand.setCustomNameVisible(true);
            armorStand.setVisible(true);
        }
    }

    public Location getLocation() {
        return armorStand.getLocation();
    }
}
