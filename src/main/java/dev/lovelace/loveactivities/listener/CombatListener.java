package dev.lovelace.loveactivities.listener;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class CombatListener implements Listener {

    private final LoveActivities plugin;

    public CombatListener(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        checkCombatAndCancel(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            checkCombatAndCancel(damager);
        }
        if (event.getEntity() instanceof Player victim) {
            checkCombatAndCancel(victim);
        }
    }

    private void checkCombatAndCancel(Player player) {
        if (player == null || !player.isOnline()) return;

        // 1. Active game session
        if (plugin.getSessionManager().isInGame(player.getUniqueId())) {
            GameSession session = plugin.getSessionManager().getSession(player.getUniqueId());
            if (session != null && session.getState() == GameState.PLAYING) {
                SoundUtil.playError(player);
                session.cancelAndRefund("autolose_pvp");
                return;
            }
        }

        // 2. Active request / challenge
        if (plugin.getRequestManager().hasActiveRequest(player.getUniqueId())) {
            plugin.getRequestManager().cancelRequestByChat(player);
        }
    }
}
