package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.BetSelectionGUI;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RequestManager {

    private final LoveActivities plugin;
    private final Map<String, ActivityRequest> requestsById = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerActiveRequest = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private org.bukkit.scheduler.BukkitTask cleanupTask;

    public RequestManager(LoveActivities plugin) {
        this.plugin = plugin;
    }

    public void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpired, 20L, 20L);
    }

    public void sendRequest(Player sender, Player receiver, GameType gameType) {
        sendRequest(sender, receiver, gameType, null);
    }

    public void sendRequest(Player sender, Player receiver, GameType gameType, String subMode) {
        UUID senderId = sender.getUniqueId();
        UUID receiverId = receiver.getUniqueId();

        if (senderId.equals(receiverId)) {
            plugin.getLocaleManager().send(sender, "error_self_challenge");
            SoundUtil.playError(sender);
            return;
        }

        // Cooldown check
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(senderId, 0L);
        int cdSec = plugin.getConfigManager().getRequestCooldownSeconds();
        if (now - last < cdSec * 1000L) {
            long remaining = Math.max(1, (cdSec * 1000L - (now - last)) / 1000L);
            plugin.getLocaleManager().send(sender, "request_cooldown", Map.of("seconds", String.valueOf(remaining)));
            SoundUtil.playError(sender);
            return;
        }

        // Check if sender busy
        if (hasActiveRequest(senderId) || plugin.getSessionManager().isInGame(senderId)) {
            plugin.getLocaleManager().send(sender, "error_you_busy");
            SoundUtil.playError(sender);
            return;
        }

        // Check if receiver busy
        if (hasActiveRequest(receiverId) || plugin.getSessionManager().isInGame(receiverId)) {
            plugin.getLocaleManager().send(sender, "error_target_busy");
            SoundUtil.playError(sender);
            return;
        }

        // Check receiver DND
        if (plugin.getSettingsManager().isDnd(receiverId)) {
            plugin.getLocaleManager().send(sender, "error_target_dnd");
            SoundUtil.playError(sender);
            return;
        }

        // Check blacklists
        if (plugin.getSettingsManager().isGameBlacklisted(senderId, gameType)) {
            plugin.getLocaleManager().send(sender, "error_game_blacklisted_by_you");
            SoundUtil.playError(sender);
            return;
        }
        if (plugin.getSettingsManager().isGameBlacklisted(receiverId, gameType)) {
            plugin.getLocaleManager().send(sender, "error_game_blacklisted_by_target");
            SoundUtil.playError(sender);
            return;
        }

        // Distance check
        if (sender.getWorld() != receiver.getWorld() || sender.getLocation().distance(receiver.getLocation()) > plugin.getConfigManager().getMaxDistance()) {
            plugin.getLocaleManager().send(sender, "error_too_far", Map.of("distance", String.valueOf(plugin.getConfigManager().getMaxDistance())));
            SoundUtil.playError(sender);
            return;
        }

        // Create request
        ActivityRequest req = new ActivityRequest(senderId, receiverId, gameType, subMode, plugin.getConfigManager().getRequestTimeoutSeconds());
        requestsById.put(req.getId(), req);
        playerActiveRequest.put(senderId, req.getId());
        playerActiveRequest.put(receiverId, req.getId());
        cooldowns.put(senderId, now);

        String lang = plugin.getConfigManager().getLanguage();
        String gameName = gameType.getDisplayName(lang);
        if ("poker".equalsIgnoreCase(subMode)) {
            gameName = "Покер";
        } else if ("war".equalsIgnoreCase(subMode)) {
            gameName = "Пьяница";
        } else if ("durak".equalsIgnoreCase(subMode)) {
            gameName = "Дурак";
        } else if ("dice_poker".equalsIgnoreCase(subMode)) {
            gameName = "Покер на костях";
        } else if ("dice_classic".equalsIgnoreCase(subMode)) {
            gameName = "Кидание костей";
        }

        Map<String, String> senderMap = Map.of(
                "player", receiver.getName(),
                "game", gameName,
                "id", req.getId()
        );
        plugin.getLocaleManager().send(sender, "request_sent", senderMap);
        SoundUtil.playClick(sender);

        Map<String, String> receiverMap = Map.of(
                "player", sender.getName(),
                "game", gameName,
                "id", req.getId()
        );
        plugin.getLocaleManager().send(receiver, "request_received", receiverMap);
        SoundUtil.playChallenge(receiver);

        plugin.getBetLogger().log("Challenge sent: " + sender.getName() + " -> " + receiver.getName() + " for " + gameType.name() + (subMode != null ? " (" + subMode + ")" : "") + " [Req ID: " + req.getId() + "]");
    }

    public void acceptRequest(Player player, String requestId) {
        ActivityRequest req = getRequest(requestId, player.getUniqueId());
        if (req == null || !req.getReceiver().equals(player.getUniqueId())) {
            plugin.getLocaleManager().send(player, "error_no_active_request");
            SoundUtil.playError(player);
            return;
        }

        Player sender = Bukkit.getPlayer(req.getSender());
        if (sender == null || !sender.isOnline()) {
            plugin.getLocaleManager().send(player, "error_target_offline");
            SoundUtil.playError(player);
            removeRequest(req.getId());
            return;
        }

        // Distance check
        if (player.getWorld() != sender.getWorld() || player.getLocation().distance(sender.getLocation()) > plugin.getConfigManager().getMaxDistance()) {
            plugin.getLocaleManager().send(player, "error_too_far", Map.of("distance", String.valueOf(plugin.getConfigManager().getMaxDistance())));
            plugin.getLocaleManager().send(sender, "error_too_far", Map.of("distance", String.valueOf(plugin.getConfigManager().getMaxDistance())));
            SoundUtil.playError(player);
            SoundUtil.playError(sender);
            removeRequest(req.getId());
            return;
        }

        removeRequest(req.getId());
        SoundUtil.playSuccess(player);
        SoundUtil.playSuccess(sender);

        plugin.getBetLogger().log("Challenge accepted: " + player.getName() + " accepted from " + sender.getName() + " [Req ID: " + req.getId() + "]");

        // Open Bet Mode Selection GUI for both players
        new BetSelectionGUI(sender, player, req.getGameType(), req.getSubMode()).openBoth();
    }

    public void declineRequest(Player player, String requestId) {
        ActivityRequest req = getRequest(requestId, player.getUniqueId());
        if (req == null || !req.getReceiver().equals(player.getUniqueId())) {
            plugin.getLocaleManager().send(player, "error_no_active_request");
            SoundUtil.playError(player);
            return;
        }

        Player sender = Bukkit.getPlayer(req.getSender());
        if (sender != null && sender.isOnline()) {
            plugin.getLocaleManager().send(sender, "request_declined", Map.of("player", player.getName()));
            SoundUtil.playError(sender);
        }

        plugin.getLocaleManager().send(player, "request_declined_self", Map.of("player", sender != null ? sender.getName() : "Unknown"));
        SoundUtil.playClick(player);

        removeRequest(req.getId());
        plugin.getBetLogger().log("Challenge declined: " + player.getName() + " declined " + (sender != null ? sender.getName() : req.getSender()) + " [Req ID: " + req.getId() + "]");
    }

    public void cancelRequest(Player player, String requestId) {
        ActivityRequest req = getRequest(requestId, player.getUniqueId());
        if (req == null || !req.getSender().equals(player.getUniqueId())) {
            plugin.getLocaleManager().send(player, "error_no_active_request");
            SoundUtil.playError(player);
            return;
        }

        Player receiver = Bukkit.getPlayer(req.getReceiver());
        plugin.getLocaleManager().send(player, "request_cancelled", Map.of("player", receiver != null ? receiver.getName() : "Unknown"));
        SoundUtil.playClick(player);

        if (receiver != null && receiver.isOnline()) {
            plugin.getLocaleManager().send(receiver, "request_cancelled", Map.of("player", player.getName()));
            SoundUtil.playError(receiver);
        }

        removeRequest(req.getId());
        plugin.getBetLogger().log("Challenge cancelled: " + player.getName() + " cancelled [Req ID: " + req.getId() + "]");
    }

    public boolean cancelRequestByChat(Player player) {
        String reqId = playerActiveRequest.get(player.getUniqueId());
        if (reqId != null) {
            ActivityRequest req = requestsById.get(reqId);
            if (req != null && req.getSender().equals(player.getUniqueId())) {
                cancelRequest(player, reqId);
                return true;
            }
        }
        return false;
    }

    public void cancelAllForPlayer(UUID uuid) {
        String reqId = playerActiveRequest.get(uuid);
        if (reqId != null) {
            ActivityRequest req = requestsById.get(reqId);
            if (req != null) {
                UUID other = req.getSender().equals(uuid) ? req.getReceiver() : req.getSender();
                Player otherPlayer = Bukkit.getPlayer(other);
                if (otherPlayer != null && otherPlayer.isOnline()) {
                    Player quitting = Bukkit.getPlayer(uuid);
                    plugin.getLocaleManager().send(otherPlayer, "request_cancelled", Map.of("player", quitting != null ? quitting.getName() : "Opponent"));
                }
                removeRequest(req.getId());
            }
        }
    }

    public boolean hasActiveRequest(UUID uuid) {
        String reqId = playerActiveRequest.get(uuid);
        if (reqId == null) return false;
        ActivityRequest req = requestsById.get(reqId);
        if (req == null || req.isExpired()) {
            playerActiveRequest.remove(uuid);
            return false;
        }
        return true;
    }

    private ActivityRequest getRequest(String id, UUID player) {
        if (id != null && requestsById.containsKey(id)) {
            return requestsById.get(id);
        }
        String activeId = playerActiveRequest.get(player);
        if (activeId != null) {
            return requestsById.get(activeId);
        }
        return null;
    }

    private void removeRequest(String id) {
        ActivityRequest req = requestsById.remove(id);
        if (req != null) {
            playerActiveRequest.remove(req.getSender());
            playerActiveRequest.remove(req.getReceiver());
        }
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (ActivityRequest req : requestsById.values()) {
            if (req.isExpired()) {
                Player sender = Bukkit.getPlayer(req.getSender());
                Player receiver = Bukkit.getPlayer(req.getReceiver());

                if (sender != null && sender.isOnline()) {
                    plugin.getLocaleManager().send(sender, "request_expired", Map.of("player", receiver != null ? receiver.getName() : "Unknown"));
                }

                removeRequest(req.getId());
                plugin.getBetLogger().log("Challenge expired: [Req ID: " + req.getId() + "]");
            }
        }
        // Memory leak prevention: purge old cooldown timestamps (> 5 mins)
        cooldowns.entrySet().removeIf(e -> now - e.getValue() > 300_000L);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        requestsById.clear();
        playerActiveRequest.clear();
        cooldowns.clear();
    }
}
