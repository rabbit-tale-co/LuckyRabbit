package co.RabbitTale.luckyRabbit.effects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.INFO_COLOR;
import net.kyori.adventure.text.Component;

public class CreatorEffects implements Listener {

    /* === CONFIG ==================================================== */
    private static final List<UUID> CREATOR_UUIDS = List.of(
            UUID.fromString("0b348919-8719-4904-b0f7-5c1313ad125f"),
            UUID.fromString("cc112d91-7812-4695-aa72-ec0ce750a567")
    );
    private static final Particle ATOM_PARTICLE = Particle.END_ROD; // ← one type

    /* === STATE ===================================================== */
    private final LuckyRabbit plugin;
    private final Map<UUID, Boolean> particlesEnabled = new HashMap<>();
    private final Map<UUID, UUID> creatorParrots = new HashMap<>();

    private double angleX = 0, angleY = 0, angleZ = 0;

    /* === CTOR ====================================================== */
    public CreatorEffects(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    /* =============================================================== */
    public void startEffects(@NotNull Player player) {
        if (!CREATOR_UUIDS.contains(player.getUniqueId())) {
            return;
        }

        particlesEnabled.putIfAbsent(player.getUniqueId(), true);
        spawnOrGetParrot(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (particlesEnabled.get(player.getUniqueId())) {
                    spawnAtomicAnimation(player);
                }

                angleX = (angleX + Math.PI / 20) % (Math.PI * 2);
                angleY = (angleY + Math.PI / 15) % (Math.PI * 2);
                angleZ = (angleZ + Math.PI / 12) % (Math.PI * 2);
            }
        }.runTaskTimer(plugin, 0L, 2L);  // refresh every 2 ticks (fast & smooth)
    }

    /**
     * Returns a unit vector that spins through all directions over time.
     */
    private static org.bukkit.util.Vector movingAxis(long tick) {
        /* use three incommensurable angular velocities so the axis never
       repeats a simple pattern (feel free to tweak the numbers)       */
        double a = tick * 0.017;    // ~360° every 370 ticks
        double b = tick * 0.013;    // ~360° every 480 ticks
        double c = tick * 0.019;    // ~360° every 330 ticks

        double x = Math.sin(a);
        double y = Math.sin(b);
        double z = Math.sin(c);
        double len = Math.sqrt(x * x + y * y + z * z);
        return new org.bukkit.util.Vector(x / len, y / len, z / len);
    }


    /* === ATOM PARTICLES =========================================== */
    private void spawnAtomicAnimation(Player player) {
        long tick = player.getWorld().getFullTime();
        Location centre = player.getLocation().add(0, 3, 0);

        /* radius & angular speed */
        double r = 0.45;
        double omega = 0.35; // rad/tick

        /* axis that slowly precesses */
        org.bukkit.util.Vector axis = movingAxis(tick);

        /* three phase-shifted angles so dots are evenly spaced */
        double[] phases = {
            0,
            2 * Math.PI / 3,
            4 * Math.PI / 3,};

        for (double phase : phases) {
            double angle = tick * omega + phase;

            /* position on a circle in the plane perpendicular to axis */
            double cos = Math.cos(angle), sin = Math.sin(angle);

            /* choose two vectors orthogonal to axis to span the plane */
            org.bukkit.util.Vector u = axis.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0));
            if (u.lengthSquared() < 1e-6) // axis nearly parallel to Y
            {
                u = axis.clone().crossProduct(new org.bukkit.util.Vector(1, 0, 0));
            }
            u.normalize();

            org.bukkit.util.Vector v = axis.clone().crossProduct(u).normalize();

            org.bukkit.util.Vector offset = u.multiply(cos * r).add(v.multiply(sin * r));

            centre.getWorld().spawnParticle(ATOM_PARTICLE, centre.clone().add(offset), 1, 0, 0, 0, 0);
        }

        /* tiny core sparkle every few ticks */
        if (tick % 6 == 0) {
            centre.getWorld().spawnParticle(ATOM_PARTICLE, centre, 1, 0, 0, 0, 0);
        }
    }

    /* === PARROT ==================================================== */
    private void spawnOrGetParrot(Player player) {
        UUID playerId = player.getUniqueId();

        // First, remove ALL parrots this player owns in ALL worlds
        removeAllParrotsFor(playerId);

        // Now spawn a fresh companion
        Parrot parrot = (Parrot) player.getWorld().spawnEntity(
                player.getLocation().add(0, 1, 0), EntityType.PARROT);

        parrot.setRemoveWhenFarAway(false);
        parrot.setTamed(true);
        parrot.setOwner(player);
        parrot.setInvulnerable(true);
        parrot.customName(Component.text("✦ Lucky Rabbit Companion ✦").color(INFO_COLOR));
        parrot.setCustomNameVisible(true);

        // Choose a random variant
        Parrot.Variant[] variants = Parrot.Variant.values();
        parrot.setVariant(variants[new java.util.Random().nextInt(variants.length)]);

        // Store the reference
        creatorParrots.put(playerId, parrot.getUniqueId());
    }

    /* Helper method to remove all parrots for a player */
    private void removeAllParrotsFor(UUID playerId) {
        // Remove any existing entry in our tracking map
        if (creatorParrots.containsKey(playerId)) {
            UUID parrotId = creatorParrots.get(playerId);
            Entity entity = Bukkit.getEntity(parrotId);
            if (entity != null) {
                entity.remove();
            }
            creatorParrots.remove(playerId);
        }

        // Find and remove all parrots owned by this player in all worlds
        Bukkit.getWorlds().forEach(world
                -> world.getEntitiesByClass(Parrot.class).stream()
                        .filter(p -> p.isTamed() && p.getOwner() != null
                        && p.getOwner().getUniqueId().equals(playerId))
                        .forEach(Entity::remove)
        );
    }

    /* Helper that applies name and random colour if not already yet */
    private void ensureParrotMeta(Parrot parrot) {
        if (parrot.customName() == null) {
            parrot.customName(Component.text("✦ Lucky Rabbit Companion ✦").color(INFO_COLOR));
            parrot.setCustomNameVisible(true);
        }

        parrot.getVariant();
    }

    /* Prevent damage to parrots from any source */
    @EventHandler
    public void onParrotHit(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();

        // Check if the damaged entity is a parrot owned by a creator
        if (damaged instanceof Parrot parrot
                && parrot.isTamed()
                && parrot.getOwner() != null
                && isCreator(parrot.getOwner().getUniqueId())) {

            // Cancel the damage event
            event.setCancelled(true);

            // For good measure, make sure it's invulnerable
            parrot.setInvulnerable(true);
        }
    }

    /* Protect parrot from ALL types of damage */
    @EventHandler
    public void onParrotDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Is this a parrot?
        if (!(entity instanceof Parrot parrot)) {
            return;
        }

        // Is it tamed with an owner?
        if (!parrot.isTamed() || parrot.getOwner() == null) {
            return;
        }

        // Is the owner a creator?
        if (isCreator(parrot.getOwner().getUniqueId())) {
            // Cancel ALL damage
            event.setCancelled(true);
            parrot.setInvulnerable(true);
        }
    }

    /* === TOGGLE COMMAND HELPERS =================================== */
    public boolean toggleParticlesVisibility(Player p) {
        if (!isCreator(p.getUniqueId())) {
            return false;
        }
        boolean newState = !particlesEnabled.getOrDefault(p.getUniqueId(), true);
        particlesEnabled.put(p.getUniqueId(), newState);
        return newState;
    }

    /* === UTILITIES ================================================= */
    public static boolean isCreator(UUID id) {
        return CREATOR_UUIDS.contains(id);
    }

    /* Remove parrot when creator leaves the server */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Only proceed if this is a creator
        if (!isCreator(playerUUID)) {
            return;
        }

        // If we have a parrot for this creator, remove it
        if (creatorParrots.containsKey(playerUUID)) {
            UUID parrotUUID = creatorParrots.get(playerUUID);
            Parrot parrot = (Parrot) Bukkit.getEntity(parrotUUID);

            if (parrot != null && parrot.isValid()) {
                parrot.remove(); // Remove the parrot entity
            }

            // Remove the entry from our tracking map
            creatorParrots.remove(playerUUID);
        }
    }

    /* Handle creator joining - ensure only one parrot exists */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Only proceed if this is a creator
        if (!isCreator(player.getUniqueId())) {
            return;
        }

        // Schedule a delayed task to handle parrot spawning
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // This will remove all existing parrots and spawn exactly one new one
            spawnOrGetParrot(player);
        }, 20L); // 1 second delay
    }
}
