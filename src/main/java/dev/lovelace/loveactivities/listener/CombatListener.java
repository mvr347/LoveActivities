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

public class CombatListener implements Listener {

    private final LoveActivities plugin;

    public CombatListener(LoveActivities plugin) {
        this.plugin = plugin;
    }

    // Только реальный PvP между самими соперниками по активной игре форфейтит матч.
    // Раньше сюда же попадал ЛЮБОЙ урон (в т.ч. EntityDamageEvent — падение, лава, утопление,
    // удар моба) и урон от посторонних игроков, из-за чего чужая ставка отменялась без всякого
    // отношения к PvP, а любой прохожий мог сорвать чужую партию одним ударом.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) return;
        if (!damager.isOnline() || !victim.isOnline()) return;

        GameSession session = plugin.getSessionManager().getSession(damager.getUniqueId());
        if (session != null && session.getState() == GameState.PLAYING && session.containsPlayer(victim.getUniqueId())) {
            SoundUtil.playError(damager);
            SoundUtil.playError(victim);
            session.cancelAndRefund("autolose_pvp");
        }
    }
}
