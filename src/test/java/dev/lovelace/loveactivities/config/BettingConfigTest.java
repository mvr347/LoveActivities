package dev.lovelace.loveactivities.config;

import dev.lovelace.lovecore.api.economy.MoneyParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Every money key of config.yml must parse: a typo must fail the build, not silently change a bet limit. */
class BettingConfigTest {

    private static long money(YamlConfiguration cfg, String path) {
        Object raw = cfg.get(path);
        assertNotNull(raw, path);
        if (raw instanceof Number n) return n.longValue();
        return MoneyParser.parse(String.valueOf(raw), MoneyParser.STANDARD);
    }

    @Test
    void bettingKeysParseAndAreConsistent() throws Exception {
        YamlConfiguration cfg;
        try (Reader r = new InputStreamReader(getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            cfg = YamlConfiguration.loadConfiguration(r);
        }
        long min = money(cfg, "betting.min_bet");
        long max = money(cfg, "betting.max_bet_global");
        assertTrue(min >= 1 && min < max);
        long small = money(cfg, "betting.npc_bet_step_small");
        long large = money(cfg, "betting.npc_bet_step_large");
        assertTrue(small >= 1 && small < large);
        assertTrue(money(cfg, "betting.poker_min_raise") >= 1);
        for (Object o : cfg.getList("betting.quick_amounts")) {
            long v = o instanceof Number n ? n.longValue() : MoneyParser.parse(String.valueOf(o), MoneyParser.STANDARD);
            assertTrue(v > 0 && v <= max);
        }
        for (String game : cfg.getConfigurationSection("betting.limits").getKeys(false)) {
            assertTrue(money(cfg, "betting.limits." + game) >= min, game);
        }
    }
}
