package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class BettingRoomGUI extends AbstractGUI {

    private final Player p1;
    private final Player p2;
    private final GameType gameType;
    private final String subMode;

    private static class SharedBetState {
        long betP1 = 0L;
        long betP2 = 0L;
        boolean readyP1 = false;
        boolean readyP2 = false;
        int countdown = 3;
        BukkitTask countdownTask = null;
        AtomicBoolean gameStarted = new AtomicBoolean(false);
        AtomicBoolean cancelled = new AtomicBoolean(false);
    }

    private final SharedBetState state;
    private BettingRoomGUI opponentView;

    public BettingRoomGUI(Player p1, Player p2, GameType gameType) {
        this(p1, p2, gameType, null, new SharedBetState());
    }

    public BettingRoomGUI(Player p1, Player p2, GameType gameType, String subMode) {
        this(p1, p2, gameType, subMode, new SharedBetState());
    }

    private BettingRoomGUI(Player p1, Player p2, GameType gameType, String subMode, SharedBetState state) {
        super(p1, 54, "<gradient:#FF5E62:#FF9966>Комната ставок</gradient>");
        this.p1 = p1;
        this.p2 = p2;
        this.gameType = gameType;
        this.subMode = subMode;
        this.state = state;
    }

    public void openBoth() {
        if (dev.lovelace.loveactivities.integration.VesuvioBridge.isHighRisk(p1)) {
            p1.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    "<red>[Активности]</red> <gray>Действие заблокировано: у вас зафиксирован высокий риск античита!</gray>"));
            return;
        }
        if (dev.lovelace.loveactivities.integration.VesuvioBridge.isHighRisk(p2)) {
            p1.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    "<red>[Активности]</red> <gray>Игрок <white>" + p2.getName() + "</white> имеет высокий риск античита и не может делать ставки!</gray>"));
            return;
        }
        this.opponentView = new BettingRoomGUI(p2, p1, gameType, subMode, state);
        this.opponentView.opponentView = this;
        this.open();
        this.opponentView.open();
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();
        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 54; i++) inventory.setItem(i, border);

        setItem(4, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name("<yellow>" + gameType.getNameRu() + "</yellow>")
                .lore("<gray>Выбранная игра</gray>").build());

        setItem(13, plugin.getHeadManager().createBuilder("ui.info")
                .name("<gold>Внесение монет</gold>")
                .lore(
                        "<gray>Нажимайте на слоты монет, чтобы внести ставку.</gray>",
                        "<gray>Списание после подтверждения обоих.</gray>",
                        "<yellow>Мин: <gold>" + plugin.getConfigManager().getMinBet() + "</gold> | Макс: <gold>" + plugin.getConfigManager().getMaxBet(gameType) + "</gold></yellow>"
                ).build());

        if (state.countdownTask != null) {
            String countHead = switch (state.countdown) {
                case 3 -> plugin.getHeadManager().getTexture("ui.countdown_3");
                case 2 -> plugin.getHeadManager().getTexture("ui.countdown_2");
                case 1 -> plugin.getHeadManager().getTexture("ui.countdown_1");
                default -> plugin.getHeadManager().getTexture("ui.coin_stack");
            };
            setItem(31, ItemBuilder.base64Head(countHead)
                    .name("<gold>Старт игры через: " + state.countdown + "...</gold>").build());
        } else {
            long total = state.betP1 + state.betP2;
            if (total > 0) {
                setItem(31, plugin.getHeadManager().createBuilder("ui.coin_stack")
                        .name("<gold>Банк игры</gold>")
                        .lore("<gray>Текущий банк: <gold>" + total + " " + plugin.getLoveCoreBridge().currencyName() + "</gold></gray>").build());
            } else {
                setItem(31, plugin.getHeadManager().createBuilder("ui.without_bets")
                        .name("<green>Игра без ставок</green>")
                        .lore("<gray>Дружеская игра на интерес</gray>").build());
            }
        }

        setItem(49, plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red>Выйти / Отменить</red>")
                .lore("<gray>Закрывает комнату и отменяет игру</gray>")
                .build(), click -> cancelBetting());

        long p1Balance = plugin.getLoveCoreBridge().getBalance(p1);
        setItem(10, ItemBuilder.skull().playerHead(p1.getUniqueId())
                .name("<gradient:#00C9FF:#92FE9D>" + p1.getName() + "</gradient>")
                .lore(
                        "<gray>Баланс: <gold>" + p1Balance + " " + plugin.getLoveCoreBridge().currencyName() + "</gold></gray>",
                        "<gray>Ставка: <yellow>" + state.betP1 + " " + plugin.getLoveCoreBridge().currencyName() + "</yellow></gray>",
                        "<gray>Статус: " + (state.readyP1 ? "<green>ГОТОВ ✔</green>" : "<red>НЕ ГОТОВ</red>") + "</gray>"
                ).build());

        long p2Balance = plugin.getLoveCoreBridge().getBalance(p2);
        setItem(16, ItemBuilder.skull().playerHead(p2.getUniqueId())
                .name("<gradient:#FF9966:#FF5E62>" + p2.getName() + "</gradient>")
                .lore(
                        "<gray>Баланс: <gold>" + p2Balance + " " + plugin.getLoveCoreBridge().currencyName() + "</gold></gray>",
                        "<gray>Ставка: <yellow>" + state.betP2 + " " + plugin.getLoveCoreBridge().currencyName() + "</yellow></gray>",
                        "<gray>Статус: " + (state.readyP2 ? "<green>ГОТОВ ✔</green>" : "<red>НЕ ГОТОВ</red>") + "</gray>"
                ).build());

        boolean isP1 = player.getUniqueId().equals(p1.getUniqueId());
        setupThreeCoinSlots(isP1, true, 19, 20, 21, 28, 29, 30);
        setupThreeCoinSlots(!isP1, false, 23, 24, 25, 32, 33, 34);
    }

    private void setupThreeCoinSlots(boolean canControl, boolean forP1, int s10, int s100, int s500, int sMax, int sReset, int sReady) {
        boolean isReady = forP1 ? state.readyP1 : state.readyP2;
        setItem(s10, plugin.getHeadManager().createBuilder("ui.plus_10")
                .name("<yellow>+10 " + plugin.getLoveCoreBridge().currencyName() + "</yellow>")
                .lore("<gray>Добавить 10</gray>").build(), canControl ? click -> addBet(forP1, 10L) : null);
        setItem(s100, plugin.getHeadManager().createBuilder("ui.plus_100")
                .name("<yellow>+100 " + plugin.getLoveCoreBridge().currencyName() + "</yellow>")
                .lore("<gray>Добавить 100</gray>").build(), canControl ? click -> addBet(forP1, 100L) : null);
        setItem(s500, plugin.getHeadManager().createBuilder("ui.plus_500")
                .name("<yellow>+500 " + plugin.getLoveCoreBridge().currencyName() + "</yellow>")
                .lore("<gray>Добавить 500</gray>").build(), canControl ? click -> addBet(forP1, 500L) : null);
        setItem(sMax, plugin.getHeadManager().createBuilder("ui.all_in")
                .name("<gold>Максимальная ставка</gold>")
                .lore("<gray>Поставить всё доступное</gray>").build(), canControl ? click -> setMaxBet(forP1) : null);
        setItem(sReset, plugin.getHeadManager().createBuilder("ui.reset")
                .name("<red>Сбросить ставку</red>").build(), canControl ? click -> resetBet(forP1) : null);
        String readyHead = isReady ? plugin.getHeadManager().getTexture("ui.confirm_ready") : plugin.getHeadManager().getTexture("ui.confirm");
        setItem(sReady, ItemBuilder.base64Head(readyHead)
                .name(isReady ? "<green>✔ ВЫ ГОТОВЫ</green>" : "<yellow>ПОДТВЕРДИТЬ СТАВКУ</yellow>")
                .lore("<gray>Статус: " + (isReady ? "<green>Ожидание соперника</green>" : "<red>Нажмите для готовности</red>") + "</gray>")
                .build(), canControl ? click -> toggleReady(forP1) : null);
    }

    private void addBet(boolean forP1, long amount) {
        cancelCountdown();
        Player targetPlayer = forP1 ? p1 : p2;
        long balance = plugin.getLoveCoreBridge().getBalance(targetPlayer);
        long maxBet = plugin.getConfigManager().getMaxBet(gameType);
        if (forP1) {
            long newBet = state.betP1 + amount;
            if (newBet > balance) { plugin.getLocaleManager().send(targetPlayer, "bet_not_enough_money", Map.of("balance", String.valueOf(balance))); SoundUtil.playError(targetPlayer); return; }
            if (newBet > maxBet) { plugin.getLocaleManager().send(targetPlayer, "bet_max_exceeded", Map.of("max", String.valueOf(maxBet), "currency", plugin.getLoveCoreBridge().currencyName())); SoundUtil.playError(targetPlayer); return; }
            state.betP1 = newBet; state.readyP1 = false;
        } else {
            long newBet = state.betP2 + amount;
            if (newBet > balance) { plugin.getLocaleManager().send(targetPlayer, "bet_not_enough_money", Map.of("balance", String.valueOf(balance))); SoundUtil.playError(targetPlayer); return; }
            if (newBet > maxBet) { plugin.getLocaleManager().send(targetPlayer, "bet_max_exceeded", Map.of("max", String.valueOf(maxBet), "currency", plugin.getLoveCoreBridge().currencyName())); SoundUtil.playError(targetPlayer); return; }
            state.betP2 = newBet; state.readyP2 = false;
        }
        SoundUtil.playClick(targetPlayer);
        syncViews();
    }

    private void setMaxBet(boolean forP1) {
        cancelCountdown();
        Player targetPlayer = forP1 ? p1 : p2;
        long target = Math.min(plugin.getLoveCoreBridge().getBalance(targetPlayer), plugin.getConfigManager().getMaxBet(gameType));
        if (forP1) { state.betP1 = target; state.readyP1 = false; } else { state.betP2 = target; state.readyP2 = false; }
        SoundUtil.playClick(targetPlayer);
        syncViews();
    }

    private void resetBet(boolean forP1) {
        cancelCountdown();
        Player targetPlayer = forP1 ? p1 : p2;
        if (forP1) { state.betP1 = 0L; state.readyP1 = false; } else { state.betP2 = 0L; state.readyP2 = false; }
        SoundUtil.playClick(targetPlayer);
        syncViews();
    }

    private void toggleReady(boolean forP1) {
        Player targetPlayer = forP1 ? p1 : p2;
        long currentBet = forP1 ? state.betP1 : state.betP2;
        long balance = plugin.getLoveCoreBridge().getBalance(targetPlayer);
        if (currentBet > balance) { plugin.getLocaleManager().send(targetPlayer, "bet_not_enough_money", Map.of("balance", String.valueOf(balance))); SoundUtil.playError(targetPlayer); return; }
        if (forP1) state.readyP1 = !state.readyP1; else state.readyP2 = !state.readyP2;
        SoundUtil.playSuccess(targetPlayer);
        if (state.readyP1 && state.readyP2) startCountdown(); else cancelCountdown();
        syncViews();
    }

    private void startCountdown() {
        if (state.countdownTask != null) return;
        state.countdown = plugin.getConfigManager().getBetCountdownSeconds();
        state.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.cancelled.get() || state.gameStarted.get()) return;
            if (state.countdown > 0) {
                SoundUtil.playCountdownTick(p1); SoundUtil.playCountdownTick(p2);
                state.countdown--;
                syncViews();
            } else {
                if (state.countdownTask != null) { state.countdownTask.cancel(); state.countdownTask = null; }
                executeGameStart();
            }
        }, 20L, 20L);
    }

    private void cancelCountdown() {
        if (state.countdownTask != null) {
            state.countdownTask.cancel();
            state.countdownTask = null;
            state.countdown = plugin.getConfigManager().getBetCountdownSeconds();
        }
    }

    private void executeGameStart() {
        if (!state.gameStarted.compareAndSet(false, true)) return;
        if (!plugin.getLoveCoreBridge().hasBalance(p1, state.betP1) || !plugin.getLoveCoreBridge().hasBalance(p2, state.betP2)) {
            plugin.getLocaleManager().send(p1, "bet_escrow_failed");
            plugin.getLocaleManager().send(p2, "bet_escrow_failed");
            cancelBetting();
            return;
        }
        if (!plugin.getLoveCoreBridge().charge(p1, state.betP1) && state.betP1 > 0) {
            plugin.getLocaleManager().send(p1, "bet_escrow_failed");
            cancelBetting();
            return;
        }
        if (!plugin.getLoveCoreBridge().charge(p2, state.betP2) && state.betP2 > 0) {
            plugin.getLoveCoreBridge().give(p1, state.betP1);
            plugin.getLocaleManager().send(p2, "bet_escrow_failed");
            cancelBetting();
            return;
        }
        long agreedBet = Math.min(state.betP1, state.betP2);
        SoundUtil.playCountdownStart(p1);
        SoundUtil.playCountdownStart(p2);
        this.setSwitchingInventory(true);
        if (this.opponentView != null) this.opponentView.setSwitchingInventory(true);
        plugin.getSessionManager().createAndStartSession(p1, p2, gameType, subMode, agreedBet, state.betP1, state.betP2);
    }

    private void cancelBetting() {
        if (!state.cancelled.compareAndSet(false, true)) return;
        cancelCountdown();
        if (p1.isOnline()) { p1.closeInventory(); plugin.getLocaleManager().send(p1, "request_cancelled", Map.of("player", p2.getName())); }
        if (p2.isOnline()) { p2.closeInventory(); plugin.getLocaleManager().send(p2, "request_cancelled", Map.of("player", p1.getName())); }
    }

    private void syncViews() {
        this.initializeItems();
        if (this.opponentView != null) this.opponentView.initializeItems();
    }

    @Override
    public void handleClose() {
        if (!state.gameStarted.get() && !state.cancelled.get() && !isSwitchingInventory()) cancelBetting();
    }
}
