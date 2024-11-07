package co.RabbitTale.luckyRabbit.effects;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.UUID;

import static co.RabbitTale.luckyRabbit.commands.LootboxCommand.INFO_COLOR;

public class CreatorEffects {

    private static final List<UUID> CREATOR_UUIDS = List.of(
            UUID.fromString("0b348919-8719-4904-b0f7-5c1313ad125f")
            //UUID.fromString("another-uuid-here")
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
    private double particleAngle = 0;

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
                spawnParticles(player);

                colorIndex = (colorIndex + 1) % GLASS_COLORS.length;
                particleAngle = (particleAngle + Math.PI / 16) % (Math.PI * 2);
            }
        }.runTaskTimer(plugin, 0L, 5L); // Run every 5 ticks (4 times per second)
    }

    private void updateHatColor(Player player) {
        ItemStack helmet = new ItemStack(GLASS_COLORS[colorIndex]);
        ItemMeta meta = helmet.getItemMeta();

        // Use Adventure API for display name
        meta.displayName(Component.text("✦ Lucky Rabbit Creator ✦")
                .color(INFO_COLOR)); // Gold color (similar to §6)

        meta.setUnbreakable(true);
        helmet.setItemMeta(meta);

        // Save previous helmet if exists and it's not a glass block
        ItemStack previousHelmet = player.getInventory().getHelmet();
        if (previousHelmet != null && !isStainedGlass(previousHelmet.getType())) {
            player.getInventory().addItem(previousHelmet);
        }

        player.getInventory().setHelmet(helmet);
    }

    private boolean isStainedGlass(Material material) {
        return material.name().endsWith("STAINED_GLASS");
    }

    private void spawnParticles(Player player) {
        double radius = 0.8;
        double height;
        double particlesPerRing = 3;

        for (int i = 0; i < 2; i++) {
            height = i * 0.5;
            for (int j = 0; j < particlesPerRing; j++) {
                double angle = particleAngle + (j * ((Math.PI * 2) / particlesPerRing));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                player.getWorld().spawnParticle(
                        Particle.SPELL_WITCH,
                        player.getLocation().add(x, height, z),
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    public static boolean isCreator(UUID uuid) {
        return CREATOR_UUIDS.contains(uuid);
    }
}
