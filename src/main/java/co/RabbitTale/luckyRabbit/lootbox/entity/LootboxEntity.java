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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.lootbox.Lootbox;
import co.RabbitTale.luckyRabbit.utils.Logger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import co.RabbitTale.luckyRabbit.lootbox.LootboxManager;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import co.RabbitTale.luckyRabbit.gui.LootboxContentGUI;
import co.RabbitTale.luckyRabbit.lootbox.animation.AnimationType;
import co.RabbitTale.luckyRabbit.gui.animations.BaseAnimationGUI;

public class LootboxEntity {

    private final LuckyRabbit plugin;
    private final String lootboxId;
    private final UUID uniqueId;
    private final ArmorStand armorStand;
    private final List<ArmorStand> nameplates = new ArrayList<>();
    private Location location;
    private double time = 0;
    private BukkitTask animationTask;
    private BukkitTask particleTask;
    private boolean isAnimating = false;
    private static final double HOVER_HEIGHT = 0.015;
    private static final double ROTATION_SPEED = 0.05;

    public LootboxEntity(LuckyRabbit plugin, ArmorStand stand, Lootbox lootbox) {
        this.plugin = plugin;
        this.armorStand = stand;
        this.lootboxId = lootbox.getId();
        this.uniqueId = UUID.randomUUID();
        this.location = stand.getLocation();

        setupMainArmorStand();
        createNameplates(lootbox);
        startAnimation();
        startParticleEffects();
        registerInteractionHandler();
    }

    private void setupMainArmorStand() {
        armorStand.setCanPickupItems(false);
        armorStand.setCollidable(true);
        armorStand.setSmall(false);

        // Zablokuj ekwipunek
        armorStand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);
        armorStand.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING);
        armorStand.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING);
        armorStand.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING);
        armorStand.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
        armorStand.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.ADDING_OR_CHANGING);

        // Ustaw skrzynie
        ItemStack chest = new ItemStack(Material.CHEST);
        armorStand.getEquipment().setHelmet(chest);
    }

    private void createNameplates(Lootbox lootbox) {
        // Oblicz ile mamy faktycznych linii (bez pustych)
        int actualLines = 1; // title zawsze sie liczy
        for (String desc : lootbox.getDescriptions()) {
            if (!desc.equals("''") && !desc.equals("")) {
                actualLines++;
            }
        }

        // Ustaw bazowa wysokosc nad skrzynia
        double baseHeight = 2.0;
        double spacing = 0.2; // Odstep miedzy liniami

        // Dostosuj wysokosc bazowa w zaleznosci od ilosci linii
        if (actualLines > 3) {
            baseHeight += (actualLines - 3) * 0.15; // Zwiększone z 0.1 na 0.15 dla lepszej skali
        }

        double currentY = location.getY() + baseHeight;

        // Najpierw tytul
        spawnNameplate(lootbox.getTitle(), currentY, "title");
        currentY -= spacing;

        // Potem opisy
        for (String desc : lootbox.getDescriptions()) {
            if (desc.equals("''") || desc.equals("")) {
                currentY -= spacing;
                continue;
            }
            spawnNameplate(desc, currentY, "description");
            currentY -= spacing;
        }
    }

    private void spawnNameplate(String text, double y, String type) {
        Location nameplateLocation = location.clone();
        nameplateLocation.setY(y);

        ArmorStand nameplate = location.getWorld().spawn(nameplateLocation, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setBasePlate(false);
            as.setInvulnerable(true);
            as.setCustomNameVisible(true);
            as.customName(MiniMessage.miniMessage().deserialize(text));
            as.setMarker(true);

            PersistentDataContainer pdc = as.getPersistentDataContainer();
            pdc.set(LootboxManager.LOOTBOX_ID_KEY, PersistentDataType.STRING, lootboxId);
            pdc.set(LootboxManager.LOOTBOX_UUID_KEY, PersistentDataType.STRING, uniqueId.toString());
            pdc.set(LootboxManager.LOOTBOX_TYPE_KEY, PersistentDataType.STRING, type);

            as.setPersistent(false);
        });

        nameplates.add(nameplate);
    }

    private void startAnimation() {
        if (isAnimating || armorStand.isDead() || !armorStand.isValid()) {
            return;
        }

        if (animationTask != null) {
            animationTask.cancel();
        }

        Location baseLocation = location.clone();
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
                if (Math.abs(newLoc.getY() - lastY) > 0.0025) {
                    armorStand.teleport(newLoc);
                    // Update nameplate positions
                    for (ArmorStand nameplate : nameplates) {
                        Location nameLoc = nameplate.getLocation();
                        nameLoc.setY(newLoc.getY() + (nameLoc.getY() - location.getY()));
                        nameplate.teleport(nameLoc);
                    }
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
                            1, 0, 0, 0, 0
                    );

                    // Add trailing effect
                    double trailRadius = radius * 0.8;
                    double trailX = Math.cos(angle - 0.5) * trailRadius;
                    double trailZ = Math.sin(angle - 0.5) * trailRadius;
                    Location trailLoc = loc.clone().add(trailX, -0.1, trailZ);
                    armorStand.getWorld().spawnParticle(
                            Particle.SPELL_INSTANT,
                            trailLoc,
                            1, 0, 0, 0, 0
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
                            1, 0, 0, 0, 0
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void registerInteractionHandler() {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.HIGH)
            public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
                if (event.getRightClicked() != armorStand) {
                    return;
                }

                event.setCancelled(true);
                Player player = event.getPlayer();

                // Sprawdz czy to przyklad i czy gracz ma uprawnienia
                if (plugin.getLootboxManager().isExampleLootbox(lootboxId) && !player.hasPermission("luckyrabbit.admin")) {
                    return;
                }

                // Pobierz lootbox
                Lootbox lootbox = plugin.getLootboxManager().getLootbox(lootboxId);
                if (lootbox == null) {
                    return;
                }

                // Jesli gracz kucnal, to pokaz menu edycji (tylko dla adminow)
                if (player.isSneaking() && player.hasPermission("luckyrabbit.admin")) {
                    new LootboxContentGUI(player, lootbox).show();
                    return;
                }

                // Pokaz menu podgladu lootboxa
                new LootboxContentGUI(player, lootbox, true, true).show();
            }

            @EventHandler(priority = EventPriority.HIGH)
            public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
                if (event.getRightClicked() == armorStand) {
                    event.setCancelled(true);
                }
            }
        }, plugin);
    }

    public void remove() {
        if (animationTask != null) {
            animationTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }

        // Usun glowny stand
        if (armorStand != null && !armorStand.isDead()) {
            armorStand.remove();
        }

        // Usun wszystkie nameplate'y
        nameplates.forEach(nameplate -> {
            if (nameplate != null && !nameplate.isDead()) {
                nameplate.remove();
            }
        });
        nameplates.clear();
    }

    public Location getLocation() {
        return location.clone();
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getLootboxId() {
        return lootboxId;
    }

    public void show(Player player) {
        // Pokaz tylko jesli gracz ma uprawnienia lub to nie jest przyklad
        if (!plugin.getLootboxManager().isExampleLootbox(lootboxId) || player.hasPermission("luckyrabbit.admin")) {
            nameplates.forEach(nameplate -> nameplate.setCustomNameVisible(true));
        }
    }

    public void hide() {
        nameplates.forEach(nameplate -> nameplate.setCustomNameVisible(false));
    }
}
