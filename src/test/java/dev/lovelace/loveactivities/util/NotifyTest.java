package dev.lovelace.loveactivities.util;

import dev.lovelace.lovecore.api.notify.LoveNotify.Channel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotifyTest {

    @Test
    void actionBarKindRequestsOnlyActionBar() {
        assertEquals(Set.of(Channel.ACTION_BAR), Notify.Gateway.channelsFor(Notify.Kind.ACTION_BAR));
    }

    @Test
    void titleKindFallsBackToActionBarNotChat() {
        assertEquals(Set.of(Channel.TITLE, Channel.ACTION_BAR), Notify.Gateway.channelsFor(Notify.Kind.TITLE));
    }

    @Test
    void eventAndImportantMayUseChat() {
        Set<Channel> expected = Set.of(Channel.ACTION_BAR, Channel.CHAT);
        assertEquals(expected, Notify.Gateway.channelsFor(Notify.Kind.EVENT));
        assertEquals(expected, Notify.Gateway.channelsFor(Notify.Kind.IMPORTANT));
    }
}
