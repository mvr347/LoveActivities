package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.manager.NpcActivityConfig;
import dev.lovelace.loveactivities.util.BetInput;
import dev.lovelace.loveactivities.util.BetLore;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.Hints;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.MenuLayout;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Confirmation against a bot: a 9-slot menu (gui_gen header row). Slot 0 describes the game, the buttons are
 * centred in slots 2-7: <pre>[ description ] [ stake ] [ accept ] [ decline ]</pre>
 * Stake button: <b>Shift</b> switches the coin, <b>left click</b> adds one, <b>right click</b> takes one away;
 * the most the stake can reach is the player's balance / the NPC's limit (there is no "maximum" button).
 * An NPC that plays without bets has no stake button at all.
 */
public class NpcConfirmationGUI extends AbstractGUI {

    private final NpcActivityConfig npcConfig;
    private final BetInput input;

    public NpcConfirmationGUI(Player player, NpcActivityConfig npcConfig) {
        super(player, MenuLayout.SIZE, "<white>Игра с </white>" + npcConfig.getCustomName());
        this.npcConfig = npcConfig;
        long balance = plugin.getLoveCoreBridge().getBalance(player);
        long limit = npcConfig.isPlaysBets()
                ? (npcConfig.getMaxBet() > 0 ? Math.min(npcConfig.getMaxBet(), balance) : balance)
                : 0L;
        this.input = new BetInput(CurrencyUtil.coinValuesAscending(), limit, npcConfig.isPlaysBets() ? npcConfig.getDefaultBet() : 0L);
        // Start on the iron coin: copper would need hundreds of clicks on the 1/100/2000/20000 scale.
        this.input.startAtUnit(100);
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < MenuLayout.SIZE; i++) inventory.setItem(i, glass);

        String iconKey = npcConfig.getGameType() != null ? npcConfig.getGameType().getIconKey() : "chess";
        String gameName = npcConfig.getGameType() != null ? npcConfig.getGameType().getNameRu() : "Мини-игра";
        long balance = plugin.getLoveCoreBridge().getBalance(player);

        List<String> infoLore = new ArrayList<>();
        infoLore.add("<dark_gray>▪</dark_gray> <gray>Соперник: </gray>" + npcConfig.getCustomName());
        if (npcConfig.isPlaysBets()) {
            infoLore.add("<dark_gray>▪</dark_gray> <gray>Ваш баланс: </gray>" + CurrencyUtil.formatCoinsShort(balance));
        } else {
            infoLore.add("<dark_gray>▪</dark_gray> <gray>Ставок нет — дружеская игра.</gray>");
        }
        setItem(0, plugin.getHeadManager().createBuilder("game_icons." + iconKey)
                .name("<yellow>Партия: " + gameName + "</yellow>")
                .lore(infoLore.toArray(new String[0]))
                .build());

        // Buttons shifted +1 to the right: 3 buttons -> [3, 5, 7], 2 buttons -> [4, 6]
        int count = npcConfig.isPlaysBets() ? 3 : 2;
        int[] baseSlots = MenuLayout.controlSlots(count);
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = Math.min(MenuLayout.SIZE - 2, baseSlots[i] + 1);
        }
        int next = 0;

        if (npcConfig.isPlaysBets()) {
            List<String> betLore = new ArrayList<>();
            betLore.addAll(BetLore.lines(input.bet()));
            betLore.add("<dark_gray>▪</dark_gray> <gray>Номинал: </gray>" + CurrencyUtil.coinGlyphForValue(input.activeUnit()));
            betLore.add("<dark_gray>▪</dark_gray> <gray>Ваш баланс: </gray>" + CurrencyUtil.formatCoinsShort(balance));
            betLore.add("<dark_gray>▪</dark_gray> <gray>Макс. ставка: </gray>" + CurrencyUtil.formatCoinsShort(input.max()));
            betLore.add("");
            betLore.add(Hints.join(Hints.act("Shift", "сменить"), Hints.op("ЛКМ", "+"), Hints.op("ПКМ", "\u2212"), Hints.act("СКМ", "макс")));
            setItem(slots[next++], plugin.getHeadManager().createBuilder("ui.coin_stack")
                    .name("<gold>Ставка</gold>")
                    .lore(betLore.toArray(new String[0]))
                    .build(), this::clickBet);
        }

        List<String> startLore = new ArrayList<>();
        if (input.bet() > 0) {
            startLore.add("<gray>Ставка будет удержана до конца партии.</gray>");
        } else {
            startLore.add("<gray>Дружеская игра без ставки.</gray>");
        }
        startLore.add("");
        startLore.add(Hints.act("ЛКМ", "принять"));
        setItem(slots[next++], plugin.getHeadManager().createBuilder("ui.confirm_ready")
                .name("<green>Принять</green>")
                .lore(startLore.toArray(new String[0]))
                .build(), click -> start());

        setItem(slots[next], plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red>Отказаться</red>")
                .lore(Hints.act("ЛКМ", "закрыть меню"))
                .build(), click -> {
            SoundUtil.playClick(player);
            player.closeInventory();
        });
    }

    private void clickBet(ClickType click) {
        boolean changed;
        if (click == ClickType.MIDDLE || (click.isShiftClick() && click.isRightClick())) {
            changed = input.setMax();
        } else if (click.isShiftClick()) {
            input.cycle();
            changed = true;
        } else if (click.isRightClick()) {
            changed = input.subtract();
        } else if (click.isLeftClick()) {
            changed = input.add();
        } else {
            return;
        }
        if (changed) SoundUtil.playClick(player);
        else SoundUtil.playError(player);
        initializeItems();
    }

    private void start() {
        long bet = input.bet();
        if (plugin.getSessionManager().isInGame(player.getUniqueId())) {
            player.closeInventory();
            SoundUtil.playError(player);
            plugin.getNpcManager().speak(player, npcConfig.getCustomName(), "Ты уже участвуешь в игре!");
            return;
        }

        if (bet > 0 && !plugin.getLoveCoreBridge().hasBalance(player, bet)) {
            SoundUtil.playError(player);
            plugin.getLocaleManager().send(player, "insufficient_funds", Map.of(
                    "cost", String.valueOf(bet),
                    "currency", plugin.getLoveCoreBridge().currencyName()
            ));
            return;
        }

        setSwitchingInventory(true);
        SoundUtil.playClick(player);
        // Cleanly close inventory and start session on next tick to avoid window packet races
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            plugin.getSessionManager().startNpcSession(player, npcConfig.getGameType(), bet, npcConfig.getCustomName(), npcConfig);
        }, 2L);
    }
}
