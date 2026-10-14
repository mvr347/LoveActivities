package dev.lovelace.loveactivities.util;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.notify.LoveNotify;
import dev.lovelace.lovecore.api.notify.LoveNotify.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Thin wrapper over LoveCore's {@code LoveNotify}. Event-style notices go through it so a player's
 * {@code /lovenotify} choice is respected and LoveCore can fall back to chat for long or important text.
 * Without LoveCore the message is sent directly, exactly as before the wrapper existed.
 *
 * <p>Command replies, errors, lists, clickable text and the turn-timer HUD stay on direct sends.</p>
 */
public final class Notify {

    /** What kind of notice is being shown; decides which channels are requested. */
    public enum Kind { ACTION_BAR, TITLE, EVENT, IMPORTANT }

    private Notify() {
    }

    /** Short status line, action bar preferred. */
    public static void actionBar(Player player, Component message) {
        send(player, message, Kind.ACTION_BAR);
    }

    /** Big on-screen notice; degrades to the action bar when the title channel is off. */
    public static void title(Player player, Component message) {
        send(player, message, Kind.TITLE);
    }

    /** Short event message that used to be a plain chat line. */
    public static void event(Player player, Component message) {
        send(player, message, Kind.EVENT);
    }

    /** Critical notice, always duplicated into chat by LoveCore. */
    public static void important(Player player, Component message) {
        send(player, message, Kind.IMPORTANT);
    }

    private static void send(Player player, Component message, Kind kind) {
        if (player == null || !player.isOnline() || message == null) return;
        try {
            // LoveCore.service is looked up on every call: a neighbour may register it later.
            if (Gateway.notify(player, message, kind)) return;
        } catch (NoClassDefFoundError missingLoveCore) {
            // LoveCore is only a soft dependency: fall through to the direct send
        }
        direct(player, message, kind);
    }

    private static void direct(Player player, Component message, Kind kind) {
        switch (kind) {
            case ACTION_BAR -> player.sendActionBar(message);
            case TITLE -> player.showTitle(Title.title(message, Component.empty()));
            case EVENT, IMPORTANT -> player.sendMessage(message);
        }
    }

    /**
     * Isolated so that {@code LoveNotify} is only resolved when this class is first touched
     * inside the try/catch above, never when {@link Notify} itself is verified.
     */
    static final class Gateway {
        private Gateway() {
        }

        static boolean notify(Player player, Component message, Kind kind) {
            var service = LoveCore.service(LoveNotify.class);
            if (service.isEmpty()) return false;
            service.get().notify(player, message, channelsFor(kind), kind == Kind.IMPORTANT);
            return true;
        }

        /**
         * Channels requested per kind. The title falls back to the action bar (not chat) when the
         * player turned titles off; chat is added by LoveCore itself when needed.
         */
        static Set<Channel> channelsFor(Kind kind) {
            return switch (kind) {
                case ACTION_BAR -> Set.of(Channel.ACTION_BAR);
                case TITLE -> Set.of(Channel.TITLE, Channel.ACTION_BAR);
                case EVENT, IMPORTANT -> Set.of(Channel.ACTION_BAR, Channel.CHAT);
            };
        }
    }
}
