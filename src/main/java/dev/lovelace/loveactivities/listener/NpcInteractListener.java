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

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcInteractListener implements Listener {

    private final LoveActivities plugin;
    private final Random random = new Random();

    // Bukkit fires BOTH PlayerInteractEntityEvent and PlayerInteractAtEntityEvent for a single
    // right-click on some entity types (e.g. ArmorStand-based NPCs). Without this guard that would
    // run handleNpcClick() twice for one click - double-charging the bet and starting two sessions.
    private final Map<UUID, Long> lastNpcInteract = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_MILLIS = 250L;

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

        long now = System.currentTimeMillis();
        Long last = lastNpcInteract.put(player.getUniqueId(), now);
        if (last != null && now - last < DEBOUNCE_MILLIS) {
            return;
        }

        // Check player reputation via LoveBehavior/LoveCore (Bad or Outcast reputation refused)
        if (plugin.getLoveCoreBridge().hasDisallowedReputation(player.getUniqueId())) {
            String refuseBadRep = config.getRandomDialogue("refuse_bad_rep");
            plugin.getNpcManager().speak(player, config.getCustomName(),
                    refuseBadRep != null ? refuseBadRep : "Я не играю с людьми с дурной славой. Проваливай!");
            SoundUtil.playError(player);
            return;
        }

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

        // 1. Check if NPC is in a temporary refusal mood
        if (config.isRefusing()) {
            long remainingSec = Math.max(1, (config.getRefusedUntil() - now) / 1000L);
            long remainingMin = (remainingSec + 59) / 60;
            String refuse = config.getRandomDialogue("refuse");
            if (refuse == null) refuse = "Я сейчас не в настроении играть. Приходи позже.";
            plugin.getNpcManager().speak(player, config.getCustomName(), refuse + " <dark_gray>(Отказывается ещё " + remainingMin + " мин.)</dark_gray>");
            SoundUtil.playError(player);
            return;
        }

        // 2. Check accept chance
        double roll = random.nextDouble();
        double acceptChance = config.getAcceptChance();
        if (roll > acceptChance) {
            // NPC enters a refusal period
            int cooldownMins = config.getRefusalCooldownMinutes();
            if (cooldownMins <= 0) cooldownMins = 3 + random.nextInt(6);
            long refuseUntil = now + (cooldownMins * 60 * 1000L);
            config.setRefusedUntil(refuseUntil);
            plugin.getNpcManager().save();

            String refuse = config.getRandomDialogue("refuse");
            if (refuse == null) refuse = "Не сейчас, путник. Я занят и не хочу играть.";
            plugin.getNpcManager().speak(player, config.getCustomName(), refuse + " <dark_gray>(Отказывается на " + cooldownMins + " мин.)</dark_gray>");
            SoundUtil.playError(player);
            return;
        }

        // Accept & Greet
        String greeting = config.getRandomDialogue("greetings");
        plugin.getNpcManager().speak(player, config.getCustomName(), greeting != null ? greeting : "Сыграем партию!");
        SoundUtil.playChallenge(player);

        // Open Confirmation GUI before starting session
        new dev.lovelace.loveactivities.gui.NpcConfirmationGUI(player, config).open();
    }
}
