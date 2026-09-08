package dev.lovelace.loveactivities.listener;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.manager.NpcActivityConfig;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Random;

public class NpcInteractListener implements Listener {

    private final LoveActivities plugin;
    private final Random random = new Random();

    public NpcInteractListener(LoveActivities plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        handleNpcClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handleNpcClick(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleNpcClick(Player player, Entity entity, org.bukkit.event.Cancellable event) {
        if (player == null || entity == null) return;
        NpcActivityConfig config = plugin.getNpcManager().getNpcConfig(entity.getUniqueId());
        if (config == null) return;

        event.setCancelled(true);

        if (plugin.getSessionManager().isInGame(player.getUniqueId())) {
            plugin.getNpcManager().speak(player, config.getCustomName(), "Ты уже участвуешь в игре! Закончи сначала текущую партию.");
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRequestManager().hasActiveRequest(player.getUniqueId())) {
            plugin.getNpcManager().speak(player, config.getCustomName(), "У тебя есть активный вызов от другого игрока. Разберись сначала с ним!");
            SoundUtil.playError(player);
            return;
        }

        // Check accept chance
        if (random.nextDouble() > config.getAcceptChance()) {
            String refuse = config.getRandomDialogue("refuse");
            plugin.getNpcManager().speak(player, config.getCustomName(), refuse != null ? refuse : "Не сейчас, путник. Я занят.");
            SoundUtil.playClick(player);
            return;
        }

        // Accept & Greet
        String greeting = config.getRandomDialogue("greetings");
        plugin.getNpcManager().speak(player, config.getCustomName(), greeting != null ? greeting : "Сыграем партию!");
        SoundUtil.playChallenge(player);

        // Start session
        plugin.getSessionManager().startNpcSession(player, config.getGameType(), config.getDefaultBet(), config.getCustomName(), config);
    }
}
