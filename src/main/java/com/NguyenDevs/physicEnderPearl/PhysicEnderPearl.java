package com.NguyenDevs.physicEnderPearl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PhysicEnderPearl extends JavaPlugin implements Listener {
    private NamespacedKey bounces;
    private final Set<UUID> allowed = new HashSet<>();
    private double bounciness = 0.85D;
    private double verticalBounciness = 0.7D;
    private int maxBounces = 5;
    private boolean bounceSoundEnabled;
    private Sound bounceSound;
    private float bounceSoundVolume;
    private float bounceSoundPitch;
    private HashSet<String> disabledWorlds;
    private double friction = 0.98D;
    private double minVelocityThreshold = 0.03D;
    private boolean teleportParticleEnabled;
    private int teleportParticleCount;
    private boolean teleportSoundEnabled;
    private Sound teleportSound;
    private float teleportSoundVolume;
    private float teleportSoundPitch;

    @Override
    public void onEnable() {
        this.bounces = new NamespacedKey(this, "bounces");
        this.getServer().getPluginManager().registerEvents(this, this);
        PEPCommand pepCommand = new PEPCommand(this);
        this.getCommand("physicenderpearl").setExecutor(pepCommand);
        this.getCommand("physicenderpearl").setTabCompleter(pepCommand);
        this.reload();
    }

    void reload() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        this.bounciness = Math.min(Math.max(this.getConfig().getDouble("bounciness", 0.85D), 0.1D), 1.0D);
        this.verticalBounciness = Math.min(Math.max(this.getConfig().getDouble("vertical-bounciness", 0.7D), 0.1D), 1.0D);
        this.maxBounces = Math.max(this.getConfig().getInt("max-bounces", 5), 1);
        this.bounceSoundEnabled = this.getConfig().getBoolean("bounce-sound.enabled", true);
        this.bounceSound = Sound.BLOCK_SLIME_BLOCK_FALL;

        try {
            String soundName = this.getConfig().getString("bounce-sound.name", "BLOCK_SLIME_BLOCK_FALL");
            this.bounceSound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[PhysicEnderPearl] '" + this.getConfig().getString("bounce-sound.name") + "' is not a valid sound name! Using default: BLOCK_SLIME_BLOCK_FALL");
        }

        this.bounceSoundVolume = (float) Math.max(this.getConfig().getDouble("bounce-sound.volume", 1.0), 0.0);
        this.bounceSoundPitch = (float) Math.max(this.getConfig().getDouble("bounce-sound.pitch", 1.0), 0.1);
        this.disabledWorlds = new HashSet<>(this.getConfig().getStringList("disabled-worlds"));
        this.friction = Math.min(Math.max(this.getConfig().getDouble("friction", 0.98D), 0.1D), 1.0D);
        this.minVelocityThreshold = Math.max(this.getConfig().getDouble("min-velocity-threshold", 0.03D), 0.01D);
        this.teleportParticleEnabled = this.getConfig().getBoolean("teleport-particle.enabled", true);
        this.teleportParticleCount = Math.max(this.getConfig().getInt("teleport-particle.count", 16), 1);
        this.teleportSoundEnabled = this.getConfig().getBoolean("teleport-sound.enabled", true);
        this.teleportSound = Sound.ENTITY_ENDERMAN_TELEPORT;

        try {
            String teleportSoundName = this.getConfig().getString("teleport-sound.name", "ENTITY_ENDERMAN_TELEPORT");
            this.teleportSound = Sound.valueOf(teleportSoundName);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[PhysicEnderPearl] '" + this.getConfig().getString("teleport-sound.name") + "' is not a valid sound name! Using default: ENTITY_ENDERMAN_TELEPORT");
        }

        this.teleportSoundVolume = (float) Math.max(this.getConfig().getDouble("teleport-sound.volume", 1.0), 0.0);
        this.teleportSoundPitch = (float) Math.max(this.getConfig().getDouble("teleport-sound.pitch", 1.0), 0.1);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.ENDER_PEARL) {
            if (!this.allowed.contains(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            } else {
                this.allowed.remove(event.getPlayer().getUniqueId());
                if (event.getPlayer() != null) {
                    Player player = event.getPlayer();
                    Location locTo = event.getTo(); // Destination position
                    Location locFrom = event.getFrom(); // Starting position

                    // Create sequential particle effects
                    if (this.teleportParticleEnabled) {
                        new BukkitRunnable() {
                            int phase = 1; // 1 = locFrom (vị trí đi), 2 = locTo (vị trí tới)
                            int i = 0;

                            @Override
                            public void run() {
                                if (phase == 1) {
                                    // Giai đoạn 1: Hiệu ứng tại vị trí đi (locFrom)
                                    // Từ trên xuống, chiều kim đồng hồ (giữ nguyên như cũ)
                                    if (i >= teleportParticleCount) {
                                        i = 0;
                                        phase = 2; // Chuyển sang giai đoạn 2
                                        return;
                                    }

                                    double angle = -2 * Math.PI * i / teleportParticleCount; // Chiều kim đồng hồ
                                    double x = Math.cos(angle) * 1.0; // Radius of 1 block
                                    double z = Math.sin(angle) * 1.0;
                                    double yOffset = 2.1 - (1.9 * i / teleportParticleCount); // Từ trên xuống (y+2.1 to y+0.2)

                                    // Particles at source (locFrom)
                                    player.getWorld().spawnParticle(
                                            Particle.SCULK_SOUL,
                                            locFrom.clone().add(x, yOffset, z),
                                            2, // Number of particles per point
                                            0.1, 0.1, 0.1, // Small offset
                                            0 // Particle speed
                                    );

                                } else if (phase == 2) {
                                    // Giai đoạn 2: Hiệu ứng tại vị trí tới (locTo)
                                    // Từ dưới lên, chiều kim đồng hồ
                                    if (i >= teleportParticleCount) {
                                        cancel(); // Kết thúc hoàn toàn
                                        return;
                                    }

                                    double angle = -2 * Math.PI * i / teleportParticleCount; // Chiều kim đồng hồ
                                    double x = Math.cos(angle) * 1.0; // Radius of 1 block
                                    double z = Math.sin(angle) * 1.0;
                                    double yOffset = 0.2 + (1.9 * i / teleportParticleCount); // Từ dưới lên (y+0.2 to y+2.1)

                                    // Particles at destination (locTo)
                                    player.getWorld().spawnParticle(
                                            Particle.SCULK_SOUL,
                                            locTo.clone().add(x, yOffset, z),
                                            2, // Number of particles per point
                                            0.1, 0.1, 0.1, // Small offset
                                            0 // Particle speed
                                    );
                                }
                                i++;
                            }
                        }.runTaskTimer(this, 0L, 1L); // Run every tick (1/20 second)
                    }

                    // Play teleport sound at both source and destination
                    if (this.teleportSoundEnabled) {
                        player.getWorld().playSound(
                                locFrom, // Sound at source position
                                this.teleportSound,
                                this.teleportSoundVolume,
                                this.teleportSoundPitch
                        );
                        player.getWorld().playSound(
                                locTo, // Sound at destination position
                                this.teleportSound,
                                this.teleportSoundVolume,
                                this.teleportSoundPitch
                        );
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntityType() == EntityType.ENDER_PEARL && event.getEntity().getShooter() instanceof Player) {
            Player p = (Player) event.getEntity().getShooter();
            if (!p.hasPermission("bouncyenderpearls.enabled") || this.disabledWorlds.contains(p.getWorld().getName())) {
                this.allowed.add(p.getUniqueId());
                event.getEntity().getPersistentDataContainer().set(this.bounces, PersistentDataType.INTEGER, this.maxBounces + 1);
            } else {
                event.getEntity().getPersistentDataContainer().set(this.bounces, PersistentDataType.INTEGER, 0);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() != EntityType.ENDER_PEARL) return;

        EnderPearl old = (EnderPearl) event.getEntity();
        Vector n;

        // Calculate normal vector
        if (event.getHitEntity() != null) {
            n = old.getLocation().toVector().subtract(event.getHitEntity().getBoundingBox().getCenter()).normalize();
        } else if (event.getHitBlockFace() != null) {
            n = event.getHitBlockFace().getDirection();
        } else {
            n = new Vector(0, 1, 0); // Default bounce upward
        }

        PersistentDataContainer data = old.getPersistentDataContainer();
        int bounceCount = 0;
        if (data.has(this.bounces, PersistentDataType.INTEGER)) {
            bounceCount = data.get(this.bounces, PersistentDataType.INTEGER);
        }

        // Check max bounces or low velocity
        if (bounceCount >= this.maxBounces || old.getVelocity().lengthSquared() < this.minVelocityThreshold * this.minVelocityThreshold) {
            if (old.getShooter() instanceof Player) {
                Player player = (Player) old.getShooter();
                this.allowed.add(player.getUniqueId());
                // Do not remove EnderPearl immediately to let Minecraft handle teleportation
                return;
            }
        }

        // Calculate reflection velocity
        Vector velocity = old.getVelocity().clone();
        double dotProduct = velocity.dot(n);
        Vector reflection = velocity.subtract(n.multiply(2.0 * dotProduct));

        // Apply bounciness, friction, and decay
        double decayFactor = Math.pow(0.95, bounceCount + 1);
        double effectiveBounciness = Math.abs(n.getY()) > 0.5 ? this.verticalBounciness : this.bounciness;
        reflection = reflection.multiply(effectiveBounciness * this.friction * decayFactor);

        // Check minimum velocity
        if (reflection.lengthSquared() < this.minVelocityThreshold * this.minVelocityThreshold) {
            if (old.getShooter() instanceof Player) {
                Player player = (Player) old.getShooter();
                this.allowed.add(player.getUniqueId());
                // Do not remove EnderPearl immediately to let Minecraft handle teleportation
                return;
            }
        }

        // Spawn new EnderPearl
        EnderPearl pearlNew = (EnderPearl) old.getWorld().spawn(old.getLocation(), EnderPearl.class);
        pearlNew.setShooter(old.getShooter());

        // Call new launch event
        ProjectileLaunchEvent launchEvent = new ProjectileLaunchEvent(pearlNew);
        Bukkit.getPluginManager().callEvent(launchEvent);
        if (!launchEvent.isCancelled()) {
            pearlNew.setVelocity(reflection);
            PersistentDataContainer newData = pearlNew.getPersistentDataContainer();
            newData.set(this.bounces, PersistentDataType.INTEGER, bounceCount + 1);
            if (this.bounceSoundEnabled) {
                pearlNew.getWorld().playSound(pearlNew.getLocation(), this.bounceSound, this.bounceSoundVolume, this.bounceSoundPitch);
            }
        }

        // Remove old EnderPearl
        old.remove();
    }
}