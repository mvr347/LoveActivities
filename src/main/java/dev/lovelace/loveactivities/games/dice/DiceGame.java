package dev.lovelace.loveactivities.games.dice;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.gui.TutorialGUI;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DiceGame implements GameSession {

    private final LoveActivities plugin = LoveActivities.getInstance();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID player1;
    private final UUID player2;
    private long bet;
    private GameState state = GameState.PLAYING;
    private long lastActionTime = System.currentTimeMillis();

    private DiceMode mode = DiceMode.POKER;

    private int[] diceP1;
    private int[] diceP2;
    private final boolean[] holdP1 = new boolean[5];
    private final boolean[] holdP2 = new boolean[5];

    private int rerollsLeftP1 = 1;
    private int rerollsLeftP2 = 1;
    private boolean isPlayer1Turn = true;
    private boolean p1Finished = false;
    private boolean p2Finished = false;
    private boolean p1Rolling = false;
    private boolean p2Rolling = false;
    private final Set<UUID> viewingTutorial = new HashSet<>();

    private DiceGUI guiP1;
    private DiceGUI guiP2;

    public DiceGame(Player p1, Player p2, long bet) {
        this(p1 != null ? p1.getUniqueId() : null, p2 != null ? p2.getUniqueId() : null, bet, DiceMode.CLASSIC);
    }

    public DiceGame(Player p1, Player p2, long bet, DiceMode mode) {
        this(p1 != null ? p1.getUniqueId() : null, p2 != null ? p2.getUniqueId() : null, bet, mode);
    }

    public DiceGame(UUID p1, UUID p2, long bet) {
        this(p1, p2, bet, DiceMode.CLASSIC);
    }

    public DiceGame(UUID p1, UUID p2, long bet, DiceMode mode) {
        this.player1 = p1;
        this.player2 = p2;
        this.bet = bet;
        this.mode = mode != null ? mode : DiceMode.CLASSIC;

        this.diceP1 = new int[this.mode.getDiceCount()];
        this.diceP2 = new int[this.mode.getDiceCount()];
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public GameType getGameType() {
        return GameType.DICE;
    }

    @Override
    public UUID getPlayer1() {
        return player1;
    }

    @Override
    public UUID getPlayer2() {
        return player2;
    }

    @Override
    public long getBet() {
        return bet;
    }

    @Override
    public void setBet(long bet) {
        this.bet = bet;
    }

    @Override
    public GameState getState() {
        return state;
    }

    @Override
    public void setState(GameState state) {
        this.state = state;
    }

    @Override
    public void startGame() {
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = (player2 != null && !dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2)) ? Bukkit.getPlayer(player2) : null;

        if (p1 != null && p1.isOnline()) {
            this.guiP1 = new DiceGUI(p1, this);
            this.guiP1.open();
        }
        if (p2 != null && p2.isOnline()) {
            this.guiP2 = new DiceGUI(p2, this);
            this.guiP2.open();
        }
    }

    public DiceMode getMode() {
        return mode;
    }

    public int[] getDiceP1() {
        return diceP1;
    }

    public int[] getDiceP2() {
        return diceP2;
    }

    public int getP1RerollsLeft() {
        return rerollsLeftP1;
    }

    public int getP2RerollsLeft() {
        return rerollsLeftP2;
    }

    public boolean isP1Hold(int i) {
        return i >= 0 && i < holdP1.length && holdP1[i];
    }

    public boolean isP2Hold(int i) {
        return i >= 0 && i < holdP2.length && holdP2[i];
    }

    public void toggleP1Hold(int i) {
        if (i >= 0 && i < holdP1.length) holdP1[i] = !holdP1[i];
    }

    public void toggleP2Hold(int i) {
        if (i >= 0 && i < holdP2.length) holdP2[i] = !holdP2[i];
    }

    public boolean isPlayer1Turn() {
        return isPlayer1Turn;
    }

    public boolean isP1Finished() {
        return p1Finished;
    }

    public boolean isP2Finished() {
        return p2Finished;
    }

    public boolean isP1Rolling() {
        return p1Rolling;
    }

    public boolean isP2Rolling() {
        return p2Rolling;
    }

    public String getP1ComboName() {
        if (diceP1 == null || diceP1[0] == 0) return "Ожидание броска";
        if (mode == DiceMode.POKER) {
            return DiceCombination.evaluate(diceP1).getType().getNameRu();
        } else {
            int sum = Arrays.stream(diceP1).sum();
            return "Сумма: " + sum;
        }
    }

    public String getP2ComboName() {
        if (diceP2 == null || diceP2[0] == 0) return "Ожидание броска";
        if (mode == DiceMode.POKER) {
            return DiceCombination.evaluate(diceP2).getType().getNameRu();
        } else {
            int sum = Arrays.stream(diceP2).sum();
            return "Сумма: " + sum;
        }
    }

    public boolean isPlayerTurn(Player player) {
        if (state != GameState.PLAYING || p1Rolling || p2Rolling) return false;
        if (isPlayer1Turn) {
            return player.getUniqueId().equals(player1) && !p1Finished;
        } else {
            return player.getUniqueId().equals(player2) && !p2Finished;
        }
    }

    public synchronized void actionRoll(Player player) {
        if (!isPlayerTurn(player)) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1) {
            if (rerollsLeftP1 <= 0) {
                actionStand(player);
                return;
            }
            rerollsLeftP1--;
            p1Rolling = true;
        } else {
            if (rerollsLeftP2 <= 0) {
                actionStand(player);
                return;
            }
            rerollsLeftP2--;
            p2Rolling = true;
        }

        SoundUtil.playDiceRoll(player);
        syncViews();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            if (isP1) {
                for (int i = 0; i < mode.getDiceCount(); i++) {
                    if (!holdP1[i] || diceP1[i] == 0) {
                        diceP1[i] = rnd.nextInt(1, 7);
                    }
                }
                p1Rolling = false;
                SoundUtil.playSuccess(player);
                syncViews();

                if (mode != DiceMode.POKER || rerollsLeftP1 == 0) {
                    actionStand(player);
                }
            } else {
                for (int i = 0; i < mode.getDiceCount(); i++) {
                    if (!holdP2[i] || diceP2[i] == 0) {
                        diceP2[i] = rnd.nextInt(1, 7);
                    }
                }
                p2Rolling = false;
                SoundUtil.playSuccess(player);
                syncViews();

                if (mode != DiceMode.POKER || rerollsLeftP2 == 0) {
                    actionStand(player);
                }
            }
        }, 15L);
    }

    public synchronized void actionStand(Player player) {
        if (!isPlayerTurn(player)) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1) {
            if (diceP1[0] == 0) {
                actionRoll(player);
                return;
            }
            p1Finished = true;
            isPlayer1Turn = false;
            SoundUtil.playClick(player);
            syncViews();

            if (dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2)) {
                triggerBotDiceTurn();
            } else {
                Player p2 = Bukkit.getPlayer(player2);
                if (p2 != null && p2.isOnline()) {
                    SoundUtil.playChallenge(p2);
                }
            }
        } else {
            if (diceP2[0] == 0) {
                actionRoll(player);
                return;
            }
            p2Finished = true;
            SoundUtil.playClick(player);
            syncViews();
            evaluateShowdown();
        }
    }

    private void triggerBotDiceTurn() {
        if (!dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2) || state != GameState.PLAYING) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < mode.getDiceCount(); i++) {
                diceP2[i] = rnd.nextInt(1, 7);
            }
            p2Finished = true;
            syncViews();
            Player p1 = Bukkit.getPlayer(player1);
            if (p1 != null && p1.isOnline()) SoundUtil.playDiceRoll(p1);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state != GameState.PLAYING) return;
                evaluateShowdown();
            }, 25L);
        }, 20L);
    }

    private void evaluateShowdown() {
        if (mode == DiceMode.POKER) {
            DiceCombination comboP1 = DiceCombination.evaluate(diceP1);
            DiceCombination comboP2 = DiceCombination.evaluate(diceP2);

            int cmp = comboP1.compareTo(comboP2);
            if (cmp > 0) {
                endWithWinner(player1);
            } else if (cmp < 0) {
                endWithWinner(player2);
            } else {
                endWithDraw();
            }
        } else {
            int sum1 = Arrays.stream(diceP1).sum();
            int sum2 = Arrays.stream(diceP2).sum();

            if (sum1 > sum2) {
                endWithWinner(player1);
            } else if (sum2 > sum1) {
                endWithWinner(player2);
            } else {
                endWithDraw();
            }
        }
    }

    public void resign(Player player) {
        if (state != GameState.PLAYING) return;
        UUID loser = player.getUniqueId();
        UUID winner = getOpponent(loser);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null) plugin.getLocaleManager().send(p1, "autolose_surrender", Map.of("player", player.getName(), "game", "Кости"));
        if (p2 != null) plugin.getLocaleManager().send(p2, "autolose_surrender", Map.of("player", player.getName(), "game", "Кости"));

        endWithWinner(winner);
    }

    public void openTutorial(Player player) {
        if (state != GameState.PLAYING) return;
        viewingTutorial.add(player.getUniqueId());

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1 && guiP1 != null) guiP1.setSwitchingInventory(true);
        if (!isP1 && guiP2 != null) guiP2.setSwitchingInventory(true);

        List<List<String>> pages = (mode == DiceMode.CLASSIC) ? List.of(
                List.of(
                        "<yellow><bold>Кидание костей (Классика 2D6)</bold></yellow>",
                        "<gray>Каждый игрок бросает по 2 кубика.</gray>",
                        "<gray>Суммируются выпавшие значения на гранях (от 2 до 12).</gray>",
                        "",
                        "<white>Правила победы:</white>",
                        "<green>• Побеждает игрок, у которого сумма очков больше!</green>",
                        "<yellow>• При равенстве суммы очков объявляется ничья и возврат ставок.</yellow>"
                )
        ) : List.of(
                List.of(
                        "<yellow><bold>Покер на костях (5 кубиков)</bold></yellow>",
                        "<gray>Каждый игрок бросает 5 кубиков.</gray>",
                        "<gray>После первого броска можно зафиксировать нужные кости</gray>",
                        "<gray>и сделать 1 переброс оставшихся для сбора комбинации.</gray>",
                        "",
                        "<white>Старшие комбинации:</white>",
                        "<gray>1. <gold>Покер</gold> (5 одинаковых)</gray>",
                        "<gray>2. <gold>Каре</gold> (4 одинаковых)</gray>",
                        "<gray>3. <gold>Фулл-Хаус</gold> (3 + 2 одинаковых)</gray>",
                        "<gray>4. <gold>Большой стрит</gold> (2-3-4-5-6)</gray>"
                ),
                List.of(
                        "<yellow><bold>Младшие комбинации</bold></yellow>",
                        "<gray>5. <yellow>Малый стрит</yellow> (1-2-3-4-5)</gray>",
                        "<gray>6. <yellow>Сет / Тройка</yellow> (3 одинаковых)</gray>",
                        "<gray>7. <yellow>Две пары</yellow> (2 + 2)</gray>",
                        "<gray>8. <yellow>Пара</yellow> (2 одинаковых)</gray>",
                        "<gray>9. <yellow>Старшая кость</yellow></gray>",
                        "",
                        "<green>Побеждает игрок с более сильной комбинацией!</green>"
                )
        );

        new TutorialGUI(player, GameType.DICE, pages, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                viewingTutorial.remove(player.getUniqueId());
                lastActionTime = System.currentTimeMillis();
                if (state == GameState.PLAYING) {
                    if (isP1 && guiP1 != null) {
                        guiP1.setSwitchingInventory(true);
                        guiP1.open();
                    } else if (!isP1 && guiP2 != null) {
                        guiP2.setSwitchingInventory(true);
                        guiP2.open();
                    }
                }
            }, 1L);
        }).open();
    }

    private void syncViews() {
        if (guiP1 != null && !viewingTutorial.contains(player1)) guiP1.initializeItems();
        if (guiP2 != null && !viewingTutorial.contains(player2)) guiP2.initializeItems();
    }

    @Override
    public void onPlayerClick(Player player, int slot, ClickType clickType) {}

    @Override
    public void onPlayerClose(Player player) {
        if (state != GameState.PLAYING || (viewingTutorial != null && viewingTutorial.contains(player.getUniqueId()))) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;
            if (viewingTutorial != null && viewingTutorial.contains(player.getUniqueId())) return;
            if (!player.isOnline()) {
                autoLose(player.getUniqueId(), "autolose_disconnect");
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractGUI) {
                return;
            }
            this.state = GameState.FINISHED;
            autoLose(player.getUniqueId(), "autolose_gui_close");
        }, 3L);
    }

    private void closeAllGuis() {
        if (guiP1 != null) {
            guiP1.setSwitchingInventory(true);
            guiP1.setClosed(true);
            Player p = guiP1.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
        if (guiP2 != null) {
            guiP2.setSwitchingInventory(true);
            guiP2.setClosed(true);
            Player p = guiP2.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
    }

    @Override
    public void autoLose(UUID loser, String reasonKey) {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.FINISHED;
        closeAllGuis();
        plugin.getSessionManager().handleAutoLose(this, loser, reasonKey);
    }

    @Override
    public void endWithWinner(UUID winner) {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.FINISHED;
        closeAllGuis();
        plugin.getSessionManager().handleWinner(this, winner);
    }

    @Override
    public void endWithDraw() {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.FINISHED;
        closeAllGuis();
        plugin.getSessionManager().handleDraw(this);
    }

    @Override
    public void cancelAndRefund(String reasonKey) {
        if (state == GameState.CANCELLED) return;
        this.state = GameState.CANCELLED;
        closeAllGuis();
        plugin.getSessionManager().handleCancelAndRefund(this, reasonKey);
    }

    @Override
    public void tickTurnTimer() {
        if (state != GameState.PLAYING) return;
        if (!viewingTutorial.isEmpty()) {
            lastActionTime = System.currentTimeMillis();
            return;
        }
        long elapsed = (System.currentTimeMillis() - lastActionTime) / 1000L;
        if (elapsed > plugin.getConfigManager().getAfkTurnTimeoutSeconds()) {
            UUID afkPlayer = isPlayer1Turn ? player1 : player2;
            autoLose(afkPlayer, "autolose_afk");
        }
    }

    @Override
    public long getLastActionTime() {
        return lastActionTime;
    }

    @Override
    public void updateLastActionTime() {
        this.lastActionTime = System.currentTimeMillis();
    }

    @Override
    public boolean containsPlayer(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    @Override
    public UUID getOpponent(UUID uuid) {
        return player1.equals(uuid) ? player2 : player1;
    }
}
