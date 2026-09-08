package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
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

    private final Player challenger;
    private final Player target;
    private final GameType gameType;
    private final String subMode;

    public static class SharedPhysicalState {
        public List<ItemStack> itemsP1 = new ArrayList<>();
        public List<ItemStack> itemsP2 = new ArrayList<>();
        public boolean readyP1 = false;
        public boolean readyP2 = false;
        public int countdown = 3;
        public BukkitTask countdownTask = null;
        public AtomicBoolean gameStarted = new AtomicBoolean(false);
        public AtomicBoolean cancelled = new AtomicBoolean(false);
    }

    private final SharedPhysicalState state;
    private SharedBetReviewGUI opponentView;

    public SharedBetReviewGUI(Player viewer, Player challenger, Player target, GameType gameType, SharedPhysicalState state) {
        this(viewer, challenger, target, gameType, null, state);
    }

    public SharedBetReviewGUI(Player viewer, Player challenger, Player target, GameType gameType, String subMode, SharedPhysicalState state) {
        super(viewer, 54, "<gradient:#FF5E62:#FF9966><bold>Согласование ставок</bold></gradient>");
        this.challenger = challenger;
        this.target = target;
        this.gameType = gameType;
        this.subMode = subMode;
        this.state = state;
    }

    public static void openForBoth(Player p1, Player p2, GameType gameType, SharedPhysicalState state) {
        openForBoth(p1, p2, gameType, null, state);
    }

    public static void openForBoth(Player p1, Player p2, GameType gameType, String subMode, SharedPhysicalState state) {
        SharedBetReviewGUI guiP1 = new SharedBetReviewGUI(p1, p1, p2, gameType, subMode, state);
        SharedBetReviewGUI guiP2 = new SharedBetReviewGUI(p2, p1, p2, gameType, subMode, state);
        guiP1.opponentView = guiP2;
        guiP2.opponentView = guiP1;

        guiP1.open();
        guiP2.open();
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        boolean isP1 = player.getUniqueId().equals(challenger.getUniqueId());
        long valP1 = state.itemsP1.stream().mapToLong(CurrencyUtil::getCoinValue).sum();
        long valP2 = state.itemsP2.stream().mapToLong(CurrencyUtil::getCoinValue).sum();
        long totalPot = valP1 + valP2;

        boolean isCounting = state.countdownTask != null;

        String gName = gameType.getNameRu();
        if ("poker".equalsIgnoreCase(subMode)) gName = "Покер";
        else if ("war".equalsIgnoreCase(subMode)) gName = "Пьяница";
        else if ("durak".equalsIgnoreCase(subMode)) gName = "Дурак";
        else if ("dice_poker".equalsIgnoreCase(subMode)) gName = "Покер на костях";
        else if ("dice_classic".equalsIgnoreCase(subMode)) gName = "Кидание костей";

        // Slot 4: Game & Bank Info
        List<String> headerLore = new ArrayList<>();
        if (totalPot > 0) {
            headerLore.add("<gray>Игрок 1 (" + challenger.getName() + "): <yellow>" + CurrencyUtil.formatCoinsWords(valP1) + "</yellow></gray>");
            headerLore.add("<gray>Игрок 2 (" + target.getName() + "): <yellow>" + CurrencyUtil.formatCoinsWords(valP2) + "</yellow></gray>");
        } else {
            headerLore.add("<gray>Режим: <white>Без ставки</white></gray>");
        }
        headerLore.add("");
        headerLore.add(isCounting ? "<gold><bold>⏳ ИДЁТ ОТСЧЁТ СТАРТА...</bold></gold>" : "<yellow>Оба игрока должны подтвердить готовность</yellow>");

        String titleHeader = totalPot > 0
                ? "<gold><bold>" + gName + " — Общий банк: " + CurrencyUtil.formatCoinsShort(totalPot) + "</bold></gold>"
                : "<gold><bold>" + gName + " — Без ставок</bold></gold>";

        setItem(4, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name(titleHeader)
                .lore(headerLore.toArray(new String[0]))
                .build());

        // Left Side: Player 1 (Challenger)
        setItem(10, ItemBuilder.skull().playerHead(challenger.getUniqueId())
                .name("<gradient:#00C9FF:#92FE9D><bold>" + challenger.getName() + "</bold></gradient>")
                .lore(
                        "<gray>Внесено: <yellow><bold>" + CurrencyUtil.formatCoinsShort(valP1) + "</bold></yellow></gray>",
                        "<gray>Статус: " + (state.readyP1 ? "<green><bold>ГОТОВ ✔</bold></green>" : "<red><bold>НЕ ГОТОВ</bold></red>") + "</gray>"
                )
                .build());

        // P1 Items Preview (Slots 19, 20, 21)
        int[] p1Slots = {19, 20, 21};
        for (int i = 0; i < 3; i++) {
            if (i < state.itemsP1.size()) {
                inventory.setItem(p1Slots[i], state.itemsP1.get(i).clone());
            } else {
                inventory.setItem(p1Slots[i], ItemBuilder.from(Material.AIR).build());
            }
        }

        // Right Side: Player 2 (Target)
        setItem(16, ItemBuilder.skull().playerHead(target.getUniqueId())
                .name("<gradient:#FF9966:#FF5E62><bold>" + target.getName() + "</bold></gradient>")
                .lore(
                        "<gray>Внесено: <yellow><bold>" + CurrencyUtil.formatCoinsShort(valP2) + "</bold></yellow></gray>",
                        "<gray>Статус: " + (state.readyP2 ? "<green><bold>ГОТОВ ✔</bold></green>" : "<red><bold>НЕ ГОТОВ</bold></red>") + "</gray>"
                )
                .build());

        // P2 Items Preview (Slots 23, 24, 25)
        int[] p2Slots = {23, 24, 25};
        for (int i = 0; i < 3; i++) {
            if (i < state.itemsP2.size()) {
                inventory.setItem(p2Slots[i], state.itemsP2.get(i).clone());
            } else {
                inventory.setItem(p2Slots[i], ItemBuilder.from(Material.AIR).build());
            }
        }

        // Center / Action controls
        if (isCounting) {
            String countHead = switch (state.countdown) {
                case 3 -> plugin.getHeadManager().getTexture("ui.countdown_3");
                case 2 -> plugin.getHeadManager().getTexture("ui.countdown_2");
                case 1 -> plugin.getHeadManager().getTexture("ui.countdown_1");
                default -> plugin.getHeadManager().getTexture("ui.coin_stack");
            };

            setItem(31, ItemBuilder.base64Head(countHead)
                    .name("<gold><bold>Старт игры через: " + state.countdown + "...</bold></gold>")
                    .lore("<red>▶ Нажмите, чтобы отменить отсчёт</red>")
                    .build(), click -> {
                cancelCountdown();
                state.readyP1 = false;
                state.readyP2 = false;
                SoundUtil.playClick(player);
                syncViews();
            });
        } else {
            boolean myReady = isP1 ? state.readyP1 : state.readyP2;

            // Slot 29: Edit my bet
            setItem(29, plugin.getHeadManager().createBuilder("ui.all_games_icon")
                    .name("<yellow><bold>✏ Изменить мою ставку</bold></yellow>")
                    .lore("<gray>Вернуться в меню внесения монет</gray>")
                    .build(), click -> editMyBet(isP1));

            // Slot 31: Individual Ready confirmation button
            String readyTex = myReady ? plugin.getHeadManager().getTexture("ui.confirm_ready") : plugin.getHeadManager().getTexture("ui.confirm");
            setItem(31, ItemBuilder.base64Head(readyTex)
                    .name(myReady ? "<green><bold>✔ ВЫ ГОТОВЫ</bold></green>" : "<yellow><bold>ПОДТВЕРДИТЬ СТАВКУ</bold></yellow>")
                    .lore(
                            myReady ? "<gray>Ожидание подтверждения соперника...</gray>" : "<green>▶ Нажмите для готовности к старту</green>",
                            myReady ? "<red>▶ Клик для снятия готовности</red>" : ""
                    )
                    .build(), click -> toggleReady(isP1));

            // Slot 33: Cancel Match
            setItem(33, plugin.getHeadManager().createBuilder("ui.cancel")
                    .name("<red><bold>✖ Отменить игру</bold></red>")
                    .lore("<gray>Вернуть все монеты и закрыть меню</gray>")
                    .build(), click -> cancelAndReturnAll());
        }
    }

    private void toggleReady(boolean forP1) {
        if (forP1) {
            state.readyP1 = !state.readyP1;
        } else {
            state.readyP2 = !state.readyP2;
        }
        SoundUtil.playClick(player);

        if (state.readyP1 && state.readyP2) {
            startCountdown();
        } else {
            cancelCountdown();
        }

        syncViews();
    }

    private void editMyBet(boolean forP1) {
        cancelCountdown();
        state.readyP1 = false;
        state.readyP2 = false;

        Player editor = forP1 ? challenger : target;
        Player other = forP1 ? target : challenger;
        List<ItemStack> currentItems = forP1 ? state.itemsP1 : state.itemsP2;

        setSwitchingInventory(true);
        if (opponentView != null) opponentView.setSwitchingInventory(true);

        // Other player sees WaitingOpponentGUI
        WaitingOpponentGUI waiting = new WaitingOpponentGUI(other, editor, gameType, this::cancelAndReturnAll);
        waiting.open();

        // Editor opens PhysicalDepositGUI
        new PhysicalDepositGUI(editor, other, gameType, currentItems, updatedItems -> {
            if (forP1) {
                state.itemsP1 = updatedItems;
            } else {
                state.itemsP2 = updatedItems;
            }
            waiting.setSwitchingInventory(true);
            openForBoth(challenger, target, gameType, state);
        }, this::cancelAndReturnAll).open();
    }

    private void startCountdown() {
        if (state.countdownTask != null) return;
        state.countdown = 3;

        state.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.cancelled.get() || state.gameStarted.get()) return;

            if (state.countdown > 0) {
                SoundUtil.playCountdownTick(challenger);
                SoundUtil.playCountdownTick(target);
                state.countdown--;
                syncViews();
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

        long valP1 = state.itemsP1.stream().mapToLong(CurrencyUtil::getCoinValue).sum();
        long valP2 = state.itemsP2.stream().mapToLong(CurrencyUtil::getCoinValue).sum();
        long agreedBet = Math.min(valP1, valP2);

        SoundUtil.playCountdownStart(challenger);
        SoundUtil.playCountdownStart(target);

        this.setSwitchingInventory(true);
        if (this.opponentView != null) this.opponentView.setSwitchingInventory(true);

        plugin.getSessionManager().createAndStartSession(challenger, target, gameType, subMode, agreedBet, valP1, valP2);
    }

    public void cancelAndReturnAll() {
        if (!state.cancelled.compareAndSet(false, true)) return;
        cancelCountdown();

        // Return P1 items
        for (ItemStack item : state.itemsP1) {
            if (challenger.isOnline()) {
                Map<Integer, ItemStack> leftover = challenger.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) challenger.getWorld().dropItemNaturally(challenger.getLocation(), drop);
            } else {
                challenger.getWorld().dropItemNaturally(challenger.getLocation(), item);
            }
        }
        state.itemsP1.clear();

        // Return P2 items
        for (ItemStack item : state.itemsP2) {
            if (target.isOnline()) {
                Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) target.getWorld().dropItemNaturally(target.getLocation(), drop);
            } else {
                target.getWorld().dropItemNaturally(target.getLocation(), item);
            }
        }
        state.itemsP2.clear();

        if (challenger.isOnline()) {
            challenger.closeInventory();
            plugin.getLocaleManager().send(challenger, "request_cancelled", Map.of("player", target.getName()));
        }
        if (target.isOnline()) {
            target.closeInventory();
            plugin.getLocaleManager().send(target, "request_cancelled", Map.of("player", challenger.getName()));
        }
    }

    private void syncViews() {
        this.initializeItems();
        if (this.opponentView != null) {
            this.opponentView.initializeItems();
        }
    }

    @Override
    public void handleClose() {
        if (!state.gameStarted.get() && !state.cancelled.get() && !isSwitchingInventory()) {
            cancelAndReturnAll();
        }
    }
}
