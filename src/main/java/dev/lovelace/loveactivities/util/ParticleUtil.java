package dev.lovelace.loveactivities.util;

import dev.lovelace.loveactivities.LoveActivities;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ParticleUtil {

    public static void spawn(Player player, Particle particle, Location loc, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (player == null || !player.isOnline()) return;
        if (LoveActivities.getInstance().getSettingsManager().hasParticles(player.getUniqueId())) {
            player.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra);
        }
    }

    public static void spawnWin(Player player) {
        if (player == null || !player.isOnline()) return;
        spawn(player, Particle.FIREWORK, player.getLocation().add(0, 1.2, 0), 20, 0.4, 0.5, 0.4, 0.08);
        spawn(player, Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.0, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }

    public static void spawnError(Player player) {
        if (player == null || !player.isOnline()) return;
        spawn(player, Particle.SMOKE, player.getLocation().add(0, 1.0, 0), 12, 0.3, 0.3, 0.3, 0.05);
    }

    public static void spawnCountdown(Player player) {
        if (player == null || !player.isOnline()) return;
        spawn(player, Particle.ENCHANT, player.getLocation().add(0, 1.0, 0), 15, 0.3, 0.4, 0.3, 0.2);
    }

    public static void spawnMove(Player player) {
        if (player == null || !player.isOnline()) return;
        spawn(player, Particle.PORTAL, player.getLocation().add(0, 0.8, 0), 10, 0.2, 0.2, 0.2, 0.05);
    }
}
