package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SharedBetReviewGUI extends AbstractGUI {

    public static final int[] SLOTS_P1 = {19, 20, 21};
    public static final int[] SLOTS_P2 = {23, 24, 25};
    public static final int SLOT_P1_READY = 29;
    public static final int SLOT_P2_READY = 33;
    public static final int SLOT_STATUS = 31;
    public static final int SLOT_CANCEL = 49;

    private final Player challenger;
    private final Player target;
    private final GameType gameType;
    private final String subMode;

    public static class SharedPhysicalState {
        public boolean readyP1 = false;
        public boolean readyP2 = false;
        public int countdown = 3;
        public BukkitTask countdownTask = null;
        public final AtomicBoolean gameStarted = new AtomicBoolean(false);
        public final AtomicBoolean cancelled = new AtomicBoolean(false);
    }

    private final SharedPhysicalState state;

    public SharedBetReviewGUI(Player challenger, Player target, GameType gameType, String subMode) {
        super(challenger, 54, "<gradient:#FF5E62:#FF9966>Комната ставок</gradient>");
        this.challenger = challenger;
        this.target = target;
        this.gameType = gameType;
        this.subMode = subMode;
        this.state = new SharedPhysicalState();
    }

    public static void openForBoth(Player p1, Player p2, GameType gameType) {
        openForBoth(p1, p2, gameType, null);
    }

    public static void openForBoth(Player p1, Player p2, GameType gameType, String subMode) {
        SharedBetReviewGUI sharedGui = new SharedBetReviewGUI(p1, p2, gameType, subMode);
        sharedGui.initializeItems();

        p1.openInventory(sharedGui.getInventory());
        p2.openInventory(sharedGui.getInventory());
    }

    public Player getChallenger() {
        return challenger;
    }

    public Player getTarget() {
        return target;
    }

    public SharedPhysicalState getState() {
        return state;
    }

    public boolean isP1DepositSlot(int slot) {
        return slot == 19 || slot == 20 || slot == 21;
    }

    public boolean isP2DepositSlot(int slot) {
        return slot == 23 || slot == 24 || slot == 25;
    }

    public boolean isDepositSlot(int slot) {
        return isP1DepositSlot(slot) || isP2DepositSlot(slot);
    }

    @Override
    public void initializeItems() {
        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        ItemStack divider = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();

        for (int i = 0; i < 54; i++) {
            if (isDepositSlot(i)) {
                // Deposit slots are interactive; leave untouched if already set or empty
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, null);
                }
            } else if (i == 13 || i == 22) {
                inventory.setItem(i, divider);
            } else {
                inventory.setItem(i, border);
            }
        }

        // Labels above deposit slots
        setItem(11, plugin.getHeadManager().createBuilder("ui.coin_stack")
                .name("<aqua>Слоты ставки: " + challenger.getName() + "</aqua>")
                .lore("<gray>Внесите монеты в 3 слота ниже</gray>")
                .build());

        setItem(15, plugin.getHeadManager().createBuilder("ui.coin_stack")
                .name("<gold>Слоты ставки: " + target.getName() + "</gold>")
                .lore("<gray>Внесите монеты в 3 слота ниже</gray>")
                .build());

        // Cancel button in footer
        setItem(SLOT_CANCEL, plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red>Отказаться от матча</red>")
                .lore(
                        "<gray>Вернуть все внесённые монеты</gray>",
                        "<gray>и отменить игру для обоих игроков.</gray>",
                        "",
                        "<red>▶ Нажмите для отмены</red>"
                )
                .build(), click -> cancelAndReturnAll());

        updateStatusDisplays();
    }

    public long calculateP1Value() {
        long sum = 0L;
        for (int slot : SLOTS_P1) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                sum += CurrencyUtil.getCoinValue(item);
            }
        }
        return sum;
    }

    public long calculateP2Value() {
        long sum = 0L;
        for (int slot : SLOTS_P2) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                sum += CurrencyUtil.getCoinValue(item);
            }
        }
        return sum;
    }

    public void updateStatusDisplays() {
        long valP1 = calculateP1Value();
        long valP2 = calculateP2Value();
        long totalPot = valP1 + valP2;
        boolean isCounting = state.countdownTask != null;
        boolean betsEqual = valP1 == valP2;

        String gName = gameType.getNameRu();
        if ("poker".equalsIgnoreCase(subMode)) gName = "Покер";
        else if ("war".equalsIgnoreCase(subMode)) gName = "Пьяница";
        else if ("durak".equalsIgnoreCase(subMode)) gName = "Дурак";
        else if ("dice_poker".equalsIgnoreCase(subMode)) gName = "Покер на костях";
        else if ("dice_classic".equalsIgnoreCase(subMode)) gName = "Кидание костей";

        // Slot 0: Challenger Head
        setItem(0, ItemBuilder.skull().playerHead(challenger.getUniqueId())
                .name("<aqua>" + challenger.getName() + "</aqua>")
                .lore(
                        "<gray>Внесено: <yellow>" + CurrencyUtil.formatCoinsShort(valP1) + "</yellow></gray>",
                        "<gray>Статус: " + (state.readyP1 ? "<green>ГОТОВ ✔</green>" : "<red>НЕ ГОТОВ</red>") + "</gray>"
                )
                .build());

        // Slot 8: Target Head
        setItem(8, ItemBuilder.skull().playerHead(target.getUniqueId())
                .name("<gold>" + target.getName() + "</gold>")
                .lore(
                        "<gray>Внесено: <yellow>" + CurrencyUtil.formatCoinsShort(valP2) + "</yellow></gray>",
                        "<gray>Статус: " + (state.readyP2 ? "<green>ГОТОВ ✔</green>" : "<red>НЕ ГОТОВ</red>") + "</gray>"
                )
                .build());

        // Slot 4: Game & Pot Info
        List<String> headerLore = new ArrayList<>();
        headerLore.add("<gray>Игрок 1 (" + challenger.getName() + "): <yellow>" + CurrencyUtil.formatCoinsWords(valP1) + "</yellow></gray>");
        headerLore.add("<gray>Игрок 2 (" + target.getName() + "): <yellow>" + CurrencyUtil.formatCoinsWords(valP2) + "</yellow></gray>");
        headerLore.add("");
        if (isCounting) {
            headerLore.add("<gold>⏳ ИДЁТ ОТСЧЁТ СТАРТА...</gold>");
        } else if (!betsEqual) {
            headerLore.add("<red>⚠ Ставки игроков должны быть равны!</red>");
        } else if (state.readyP1 && !state.readyP2) {
            headerLore.add("<yellow>Ожидание готовности " + target.getName() + "...</yellow>");
        } else if (!state.readyP1 && state.readyP2) {
            headerLore.add("<yellow>Ожидание готовности " + challenger.getName() + "...</yellow>");
        } else {
            headerLore.add("<yellow>Оба игрока должны подтвердить готовность</yellow>");
        }

        if (totalPot > 0) {
            headerLore.addAll(dev.lovelace.loveactivities.util.BetLore.lines(totalPot));
        }
        String titleHeader = totalPot > 0
                ? "<gold>" + gName + " — Ставки</gold>"
                : "<gold>" + gName + " — Без ставок</gold>";

        setItem(4, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name(titleHeader)
                .lore(headerLore.toArray(new String[0]))
                .build());

        // Slot 29: P1 Ready Button
        String p1ReadyTex = state.readyP1 ? "ui.confirm_ready" : "ui.confirm";
        List<String> p1Lore = new ArrayList<>();
        if (!state.readyP1) {
            p1Lore.add("<gray>Нажмите, когда выставите ставку.</gray>");
            if (!betsEqual) {
                p1Lore.add("<red>⚠ Ставки должны совпадать!</red>");
            } else {
                p1Lore.add("<yellow>▶ Нажмите для подтверждения готовности</yellow>");
            }
        } else {
            p1Lore.add("<gray>Ставка подтверждена.</gray>");
            p1Lore.add("<gray>Ожидание соперника...</gray>");
            p1Lore.add("<red>▶ Клик для снятия готовности</red>");
        }

        setItem(SLOT_P1_READY, plugin.getHeadManager().createBuilder(p1ReadyTex)
                .name(state.readyP1 ? "<green>Готов ✔</green>" : "<yellow>Не готов</yellow>")
                .lore(p1Lore.toArray(new String[0]))
                .build());

        // Slot 33: P2 Ready Button
        String p2ReadyTex = state.readyP2 ? "ui.confirm_ready" : "ui.confirm";
        List<String> p2Lore = new ArrayList<>();
        if (!state.readyP2) {
            p2Lore.add("<gray>Нажмите, когда выставите ставку.</gray>");
            if (!betsEqual) {
                p2Lore.add("<red>⚠ Ставки должны совпадать!</red>");
            } else {
                p2Lore.add("<yellow>▶ Нажмите для подтверждения готовности</yellow>");
            }
        } else {
            p2Lore.add("<gray>Ставка подтверждена.</gray>");
            p2Lore.add("<gray>Ожидание соперника...</gray>");
            p2Lore.add("<red>▶ Клик для снятия готовности</red>");
        }

        setItem(SLOT_P2_READY, plugin.getHeadManager().createBuilder(p2ReadyTex)
                .name(state.readyP2 ? "<green>Готов ✔</green>" : "<yellow>Не готов</yellow>")
                .lore(p2Lore.toArray(new String[0]))
                .build());

        // Slot 31: Center Status / Countdown Button
        if (isCounting) {
            String countHead = switch (state.countdown) {
                case 3 -> plugin.getHeadManager().getTexture("ui.countdown_3");
                case 2 -> plugin.getHeadManager().getTexture("ui.countdown_2");
                case 1 -> plugin.getHeadManager().getTexture("ui.countdown_1");
                default -> plugin.getHeadManager().getTexture("ui.coin_stack");
            };

            setItem(SLOT_STATUS, ItemBuilder.base64Head(countHead)
                    .name("<gold>Старт игры через: " + state.countdown + "...</gold>")
                    .lore("<red>▶ Нажмите, чтобы отменить отсчёт</red>")
                    .build());
        } else {
            setItem(SLOT_STATUS, plugin.getHeadManager().createBuilder("ui.all_games_icon")
                    .name("<yellow>Согласование ставок</yellow>")
                    .lore(
                            "<gray>Ставка P1: <yellow>" + CurrencyUtil.formatCoinsShort(valP1) + "</yellow></gray>",
                            "<gray>Ставка P2: <yellow>" + CurrencyUtil.formatCoinsShort(valP2) + "</yellow></gray>",
                            betsEqual ? "<green>✔ Ставки равны</green>" : "<red>✘ Ставки не равны</red>"
                    )
                    .build());
        }
    }

    public void onP1SlotsChanged() {
        if (state.readyP1) {
            state.readyP1 = false;
        }
        cancelCountdown();
        updateStatusDisplays();
    }

    public void onP2SlotsChanged() {
        if (state.readyP2) {
            state.readyP2 = false;
        }
        cancelCountdown();
        updateStatusDisplays();
    }

    public void toggleReadyP1() {
        long valP1 = calculateP1Value();
        long valP2 = calculateP2Value();

        if (!state.readyP1 && valP1 != valP2) {
            SoundUtil.playError(challenger);
            challenger.sendMessage(TextUtil.parse("<red>Ставки игроков должны быть равны! (" +
                    CurrencyUtil.formatCoinsShort(valP1) + " ≠ " + CurrencyUtil.formatCoinsShort(valP2) + ")</red>"));
            return;
        }

        state.readyP1 = !state.readyP1;
        SoundUtil.playClick(challenger);

        if (state.readyP1 && state.readyP2) {
            startCountdown();
        } else {
            cancelCountdown();
        }

        updateStatusDisplays();
    }

    public void toggleReadyP2() {
        long valP1 = calculateP1Value();
        long valP2 = calculateP2Value();

        if (!state.readyP2 && valP1 != valP2) {
            SoundUtil.playError(target);
            target.sendMessage(TextUtil.parse("<red>Ставки игроков должны быть равны! (" +
                    CurrencyUtil.formatCoinsShort(valP2) + " ≠ " + CurrencyUtil.formatCoinsShort(valP1) + ")</red>"));
            return;
        }

        state.readyP2 = !state.readyP2;
        SoundUtil.playClick(target);

        if (state.readyP1 && state.readyP2) {
            startCountdown();
        } else {
            cancelCountdown();
        }

        updateStatusDisplays();
    }

    public void cancelCountdownByUser(Player who) {
        cancelCountdown();
        state.readyP1 = false;
        state.readyP2 = false;
        SoundUtil.playClick(who);
        updateStatusDisplays();
    }

    private void startCountdown() {
        if (state.countdownTask != null) return;
        state.countdown = 3;

        state.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.cancelled.get() || state.gameStarted.get()) return;

            if (state.countdown > 0) {
                if (challenger.isOnline()) SoundUtil.playCountdownTick(challenger);
                if (target.isOnline()) SoundUtil.playCountdownTick(target);
                state.countdown--;
                updateStatusDisplays();
            } else {
                if (state.countdownTask != null) {
                    state.countdownTask.cancel();
                    state.countdownTask = null;
                }
                executeGameStart();
            }
        }, 20L, 20L);
    }

    private void cancelCountdown() {
        if (state.countdownTask != null) {
            state.countdownTask.cancel();
            state.countdownTask = null;
            state.countdown = 3;
        }
    }

    private void executeGameStart() {
        if (!state.gameStarted.compareAndSet(false, true)) return;

        long valP1 = calculateP1Value();
        long valP2 = calculateP2Value();
        long agreedBet = Math.min(valP1, valP2);

        // Clear deposit slots so coins are not refunded on close
        for (int slot : SLOTS_P1) {
            inventory.setItem(slot, null);
        }
        for (int slot : SLOTS_P2) {
            inventory.setItem(slot, null);
        }

        if (challenger.isOnline()) SoundUtil.playCountdownStart(challenger);
        if (target.isOnline()) SoundUtil.playCountdownStart(target);

        setSwitchingInventory(true);

        challenger.closeInventory();
        target.closeInventory();

        plugin.getSessionManager().createAndStartSession(challenger, target, gameType, subMode, agreedBet, valP1, valP2);
    }

    public void cancelAndReturnAll() {
        if (!state.cancelled.compareAndSet(false, true)) return;
        cancelCountdown();

        // 1. Return P1 items
        for (int slot : SLOTS_P1) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                inventory.setItem(slot, null);
                if (challenger.isOnline()) {
                    Map<Integer, ItemStack> leftover = challenger.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) challenger.getWorld().dropItemNaturally(challenger.getLocation(), drop);
                } else {
                    challenger.getWorld().dropItemNaturally(challenger.getLocation(), item);
                }
            }
        }

        // 2. Return P2 items
        for (int slot : SLOTS_P2) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                inventory.setItem(slot, null);
                if (target.isOnline()) {
                    Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) target.getWorld().dropItemNaturally(target.getLocation(), drop);
                } else {
                    target.getWorld().dropItemNaturally(target.getLocation(), item);
                }
            }
        }

        // 3. Close and notify
        if (challenger.isOnline()) {
            challenger.closeInventory();
            plugin.getLocaleManager().send(challenger, "request_cancelled", Map.of("player", target.getName()));
        }
        if (target.isOnline()) {
            target.closeInventory();
            plugin.getLocaleManager().send(target, "request_cancelled", Map.of("player", challenger.getName()));
        }
    }

    @Override
    public void handleClose() {
        if (!state.gameStarted.get() && !state.cancelled.get() && !isSwitchingInventory()) {
            cancelAndReturnAll();
        }
    }
}
