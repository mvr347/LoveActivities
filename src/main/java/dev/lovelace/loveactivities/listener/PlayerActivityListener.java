package dev.lovelace.loveactivities.listener;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;

public class PlayerActivityListener implements Listener {

    private final LoveActivities plugin;

    public PlayerActivityListener(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim().toLowerCase();

        if (message.equals("отменить") || message.equals("cancel") || message.equals("отмена")) {
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getRequestManager().cancelRequestByChat(player);
                });
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getSettingsManager().loadAsync(uuid);
        plugin.getStatsManager().loadAsync(uuid);

        // Claim queued offline payouts
        plugin.getOfflinePayoutDao().claimPayouts(uuid).thenAccept(claimedAmount -> {
            if (claimedAmount > 0 && plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getLoveCoreBridge().give(player, claimedAmount);
                        plugin.getLocaleManager().send(player, "bet_refunded", Map.of(
                                "amount", String.valueOf(claimedAmount),
                                "currency", plugin.getLoveCoreBridge().currencyName()
                        ));
                    }
                });
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        GameSession session = plugin.getSessionManager().getSession(uuid);
        if (session != null) {
            session.autoLose(uuid, "autolose_disconnect");
        }

        plugin.getRequestManager().cancelAllForPlayer(uuid);
        plugin.getSettingsManager().unload(uuid);
        plugin.getStatsManager().unload(uuid);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        GameSession session = plugin.getSessionManager().getSession(uuid);
        if (session != null) {
            session.autoLose(uuid, "autolose_death");
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        GameSession session = plugin.getSessionManager().getSession(uuid);
        if (session != null && event.getTo() != null) {
            Player opp = Bukkit.getPlayer(session.getOpponent(uuid));
            if (opp != null && opp.isOnline()) {
                double maxDist = plugin.getConfigManager().getMaxDistance();
                if (event.getTo().getWorld() != opp.getWorld() || event.getTo().distanceSquared(opp.getLocation()) > (maxDist * maxDist)) {
                    session.autoLose(uuid, "autolose_distance");
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        GameSession session = plugin.getSessionManager().getSession(uuid);
        if (session != null) {
            session.autoLose(uuid, "autolose_distance");
        }
    }
}
