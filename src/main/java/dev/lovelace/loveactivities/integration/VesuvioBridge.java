package dev.lovelace.loveactivities.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Pure-reflection bridge to Vesuvio AntiCheat (zero hard dependency).
 * Protects mini-games and currency bets against high-risk or suspect players.
 *
 * Author: Lovelace
 */
public final class VesuvioBridge {

    private VesuvioBridge() {}

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Vesuvio");
    }

    public static boolean isHighRisk(Player player) {
        if (player == null || !isAvailable()) return false;
        return isHighRisk(player.getUniqueId());
    }

    public static boolean isHighRisk(UUID uuid) {
        if (!isAvailable()) return false;
        try {
            Class<?> providerClass = Class.forName("net.lovelace.vesuvio.api.VesuvioProvider");
            Method isAvailMethod = providerClass.getMethod("isAvailable");
            if (!((boolean) isAvailMethod.invoke(null))) return false;

            Method getMethod = providerClass.getMethod("get");
            Object api = getMethod.invoke(null);
            if (api == null) return false;

            Class<?> apiClass = Class.forName("net.lovelace.vesuvio.api.VesuvioAPI");
            Method isHighRiskMethod = apiClass.getMethod("isHighRisk", UUID.class);
            boolean highRisk = (boolean) isHighRiskMethod.invoke(api, uuid);
            if (highRisk) return true;

            Method isSuspectMethod = apiClass.getMethod("isSuspect", UUID.class);
            return (boolean) isSuspectMethod.invoke(api, uuid);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
