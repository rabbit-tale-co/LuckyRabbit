package co.RabbitTale.luckyRabbit.effects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.INFO_COLOR;
import net.kyori.adventure.text.Component;

public class CreatorEffects implements Listener {

    private static final List<UUID> CREATOR_UUIDS = List.of(
            UUID.fromString("0b348919-8719-4904-b0f7-5c1313ad125f"),
            UUID.fromString("cc112d91-7812-4695-aa72-ec0ce750a567")
    );

    private static final Material[] GLASS_COLORS = {
        Material.RED_STAINED_GLASS,
        Material.ORANGE_STAINED_GLASS,
        Material.YELLOW_STAINED_GLASS,
        Material.LIME_STAINED_GLASS,
        Material.LIGHT_BLUE_STAINED_GLASS,
        Material.BLUE_STAINED_GLASS,
        Material.PURPLE_STAINED_GLASS,
        Material.MAGENTA_STAINED_GLASS
    };

    private final LuckyRabbit plugin;
    private int colorIndex = 0;
    private double particleAngle1 = 0;
    private double particleAngle2 = 0;

    private final Map<UUID, Boolean> particlesEnabled = new HashMap<>();

    public CreatorEffects(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    public void startEffects(Player player) {
        if (!CREATOR_UUIDS.contains(player.getUniqueId())) {
            return;
        }

        // Start the color changing hat and particle effects
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                updateHatColor(player);
                spawnDualRings(player);

                colorIndex = (colorIndex + 1) % GLASS_COLORS.length;
                particleAngle1 = (particleAngle1 + Math.PI / 64) % (Math.PI * 2);
                particleAngle2 = (particleAngle2 - Math.PI / 64) % (Math.PI * 2);
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void updateHatColor(Player player) {
        ItemStack helmet = new ItemStack(GLASS_COLORS[colorIndex]);
        ItemMeta meta = helmet.getItemMeta();
        meta.displayName(Component.text("✦ Lucky Rabbit Creator ✦")
                .color(INFO_COLOR));
        meta.setUnbreakable(true);
        helmet.setItemMeta(meta);

        ItemStack previousHelmet = player.getInventory().getHelmet();
        if (previousHelmet != null && !isStainedGlass(previousHelmet.getType())) {
            player.getInventory().addItem(previousHelmet);
        }

        player.getInventory().setHelmet(helmet);
    }

    private boolean isStainedGlass(Material material) {
        return material.name().endsWith("STAINED_GLASS");
    }

    private void spawnDualRings(Player player) {
        Location loc = player.getLocation().add(0, 1.8, 0);
        double radius = 0.7;
        int particleCount = 30;

        spawnRing(player, loc, radius, particleCount, particleAngle1);
        spawnRing(player, loc, radius, particleCount, particleAngle2);
    }

    private void spawnRing(Player player, Location loc, double radius, int particleCount, double particleAngle) {
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI / particleCount) * i + particleAngle;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            spawnParticleForOthers(player, loc.clone().add(x, 0, z));
        }
    }

    private void spawnParticleForOthers(Player player, Location location) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                // Other players always see particles
                p.spawnParticle(Particle.PORTAL, location, 1, 0, 0, 0, 0);
            } else if (particlesEnabled.getOrDefault(player.getUniqueId(), true)) {
                // Owner only sees particles if enabled
                p.spawnParticle(Particle.PORTAL, location, 1, 0, 0, 0, 0);
            }
        }
    }

    public static boolean isCreator(UUID uuid) {
        return CREATOR_UUIDS.contains(uuid);
    }

    public boolean toggleParticlesVisibility(Player player) {
        if (!CREATOR_UUIDS.contains(player.getUniqueId())) {
            return false;
        }

        boolean currentState = particlesEnabled.getOrDefault(player.getUniqueId(), true);
        boolean newState = !currentState;
        particlesEnabled.put(player.getUniqueId(), newState);
        return newState;
    }
}
