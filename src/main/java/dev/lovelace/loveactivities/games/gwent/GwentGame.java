package dev.lovelace.loveactivities.games.gwent;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.gui.TutorialGUI;
import dev.lovelace.loveactivities.manager.SessionManager;
import dev.lovelace.loveactivities.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class GwentGame implements GameSession {

    private final LoveActivities plugin = LoveActivities.getInstance();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID player1;
    private final UUID player2;
    private long bet;
    private GameState state = GameState.PLAYING;
    private long lastActionTime = System.currentTimeMillis();

    private final List<GwentCard> deckP1 = GwentDeck.createStandardDeck();
    private final List<GwentCard> deckP2 = GwentDeck.createStandardDeck();

    private final List<GwentCard> handP1 = new ArrayList<>();
    private final List<GwentCard> handP2 = new ArrayList<>();

    private final List<GwentCard> graveyardP1 = new ArrayList<>();
    private final List<GwentCard> graveyardP2 = new ArrayList<>();

    // Battlefield
    private final List<GwentCard> meleeP1 = new ArrayList<>();
    private final List<GwentCard> rangedP1 = new ArrayList<>();
    private final List<GwentCard> siegeP1 = new ArrayList<>();

    private final List<GwentCard> meleeP2 = new ArrayList<>();
    private final List<GwentCard> rangedP2 = new ArrayList<>();
    private final List<GwentCard> siegeP2 = new ArrayList<>();

    // Weather
    private boolean frost = false;
    private boolean fog = false;
    private boolean rain = false;

    // Horns
    private boolean hornMeleeP1 = false, hornRangedP1 = false, hornSiegeP1 = false;
    private boolean hornMeleeP2 = false, hornRangedP2 = false, hornSiegeP2 = false;

    // Rounds & Passing
    private int currentRound = 1;
    private int roundsWonP1 = 0;
    private int roundsWonP2 = 0;
    private boolean p1Passed = false;
    private boolean p2Passed = false;
    private boolean isPlayer1Turn = true;
    /** Who opened the current round: a draw hands the opening to the other player. */
    private boolean roundStarterP1 = true;
    private final Set<UUID> viewingTutorial = new HashSet<>();

    private GwentGUI guiP1;
    private GwentGUI guiP2;

    public GwentGame(Player p1, Player p2, long bet) {
        this.player1 = p1.getUniqueId();
        this.player2 = p2 != null ? p2.getUniqueId() : SessionManager.NPC_UUID;
        this.bet = bet;

        // Deal 10 cards initially
        for (int i = 0; i < 10 && !deckP1.isEmpty(); i++) {
            handP1.add(deckP1.remove(0));
        }
        for (int i = 0; i < 10 && !deckP2.isEmpty(); i++) {
            handP2.add(deckP2.remove(0));
        }
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public GameType getGameType() {
        return GameType.GWENT;
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
        Player p2 = Bukkit.getPlayer(player2);

        if (p1 != null && p1.isOnline()) {
            this.guiP1 = new GwentGUI(p1, this);
            this.guiP1.open();
        }
        if (p2 != null && p2.isOnline() && !player2.equals(SessionManager.NPC_UUID)) {
            this.guiP2 = new GwentGUI(p2, this);
            this.guiP2.open();
        }
    }

    public List<GwentCard> getHandP1() {
        return handP1;
    }

    public List<GwentCard> getHandP2() {
        return handP2;
    }

    public List<GwentCard> getMeleeP1() {
        return meleeP1;
    }

    public List<GwentCard> getRangedP1() {
        return rangedP1;
    }

    public List<GwentCard> getSiegeP1() {
        return siegeP1;
    }

    public List<GwentCard> getMeleeP2() {
        return meleeP2;
    }

    public List<GwentCard> getRangedP2() {
        return rangedP2;
    }

    public List<GwentCard> getSiegeP2() {
        return siegeP2;
    }

    public boolean isFrost() {
        return frost;
    }

    public boolean isFog() {
        return fog;
    }

    public boolean isRain() {
        return rain;
    }

    public boolean hasHornMelee(boolean isP1) {
        return isP1 ? hornMeleeP1 : hornMeleeP2;
    }

    public boolean hasHornRanged(boolean isP1) {
        return isP1 ? hornRangedP1 : hornRangedP2;
    }

    public boolean hasHornSiege(boolean isP1) {
        return isP1 ? hornSiegeP1 : hornSiegeP2;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getRoundsWonP1() {
        return roundsWonP1;
    }

    public int getRoundsWonP2() {
        return roundsWonP2;
    }

    public boolean isP1Passed() {
        return p1Passed;
    }

    public boolean isP2Passed() {
        return p2Passed;
    }

    public boolean isPlayerTurn(Player player) {
        if (state != GameState.PLAYING) return false;
        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1) {
            return isPlayer1Turn && !p1Passed;
        } else {
            return !isPlayer1Turn && !p2Passed;
        }
    }

    public int getCalculatedCardPower(GwentCard card, boolean isP1) {
        if (!card.isUnit()) return 0;
        if (card.isHero()) return card.getBaseStrength();

        int power = card.getBaseStrength();
        boolean affectedByWeather = switch (card.getRow()) {
            case MELEE -> frost;
            case RANGED -> fog;
            case SIEGE -> rain;
            default -> false;
        };

        if (affectedByWeather) {
            power = 1;
        }

        boolean hasHorn = switch (card.getRow()) {
            case MELEE -> isP1 ? hornMeleeP1 : hornMeleeP2;
            case RANGED -> isP1 ? hornRangedP1 : hornRangedP2;
            case SIEGE -> isP1 ? hornSiegeP1 : hornSiegeP2;
            default -> false;
        };

        if (hasHorn) {
            power *= 2;
        }

        return power;
    }

    public int calculateRowPower(List<GwentCard> cards, boolean isP1) {
        if (cards == null) return 0;
        int total = 0;
        for (GwentCard c : cards) {
            total += getCalculatedCardPower(c, isP1);
        }
        return total;
    }

    public int calculateTotalPower(boolean isP1) {
        int total = 0;
        List<GwentCard> melee = isP1 ? meleeP1 : meleeP2;
        List<GwentCard> ranged = isP1 ? rangedP1 : rangedP2;
        List<GwentCard> siege = isP1 ? siegeP1 : siegeP2;

        total += calculateRowPower(melee, isP1);
        total += calculateRowPower(ranged, isP1);
        total += calculateRowPower(siege, isP1);

        return total;
    }

    public synchronized void actionPlayCard(Player player, int cardIndex) {
        if (state != GameState.PLAYING) return;
        boolean isP1 = player != null && player.getUniqueId().equals(player1);
        if (player != null && !isPlayerTurn(player)) return;
        if (player == null && (isPlayer1Turn || p2Passed)) return;
        updateLastActionTime();

        List<GwentCard> hand = isP1 ? handP1 : handP2;

        if (cardIndex < 0 || cardIndex >= hand.size()) return;
        GwentCard card = hand.remove(cardIndex);

        if (player != null) SoundUtil.playCardDraw(player);

        switch (card.getAbility()) {
            case SPY -> {
                if (isP1) meleeP2.add(card);
                else meleeP1.add(card);
                List<GwentCard> deck = isP1 ? deckP1 : deckP2;
                for (int i = 0; i < 2 && !deck.isEmpty(); i++) {
                    hand.add(deck.remove(0));
                }
            }
            case MEDIC -> {
                placeUnit(card, isP1);
                List<GwentCard> grave = isP1 ? graveyardP1 : graveyardP2;
                if (!grave.isEmpty()) {
                    GwentCard res = grave.remove(grave.size() - 1);
                    if (res.isUnit()) {
                        placeUnit(res, isP1);
                    }
                }
            }
            case SCORCH -> {
                if (card.isUnit()) placeUnit(card, isP1);
                applyScorch();
            }
            case HORN -> {
                if (card.getRow() == GwentCard.Row.MELEE) {
                    if (isP1) hornMeleeP1 = true; else hornMeleeP2 = true;
                } else if (card.getRow() == GwentCard.Row.RANGED) {
                    if (isP1) hornRangedP1 = true; else hornRangedP2 = true;
                } else if (card.getRow() == GwentCard.Row.SIEGE) {
                    if (isP1) hornSiegeP1 = true; else hornSiegeP2 = true;
                }
            }
            case DECOY -> {
                List<GwentCard> melee = isP1 ? meleeP1 : meleeP2;
                List<GwentCard> ranged = isP1 ? rangedP1 : rangedP2;
                List<GwentCard> siege = isP1 ? siegeP1 : siegeP2;
                GwentCard returned = null;

                if (!melee.isEmpty()) returned = melee.remove(melee.size() - 1);
                else if (!ranged.isEmpty()) returned = ranged.remove(ranged.size() - 1);
                else if (!siege.isEmpty()) returned = siege.remove(siege.size() - 1);

                if (returned != null && !returned.isHero()) {
                    hand.add(returned);
                }
            }
            case FROST -> frost = true;
            case FOG -> fog = true;
            case RAIN -> rain = true;
            case CLEAR -> {
                frost = false;
                fog = false;
                rain = false;
            }
            default -> {
                if (card.isUnit()) {
                    placeUnit(card, isP1);
                }
            }
        }

        // Auto-pass if hand empty
        if (hand.isEmpty()) {
            if (isP1) p1Passed = true;
            else p2Passed = true;
        }

        if (p1Passed && p2Passed) {
            evaluateRound();
        } else {
            switchTurn();
        }
    }

    private void placeUnit(GwentCard card, boolean isP1) {
        switch (card.getRow()) {
            case MELEE -> (isP1 ? meleeP1 : meleeP2).add(card);
            case RANGED -> (isP1 ? rangedP1 : rangedP2).add(card);
            case SIEGE -> (isP1 ? siegeP1 : siegeP2).add(card);
            default -> {}
        }
    }

    private void applyScorch() {
        int maxPower = -1;
        List<List<GwentCard>> allRows = List.of(meleeP1, rangedP1, siegeP1, meleeP2, rangedP2, siegeP2);

        for (List<GwentCard> row : allRows) {
            for (GwentCard c : row) {
                if (!c.isHero()) {
                    maxPower = Math.max(maxPower, c.getBaseStrength());
                }
            }
        }

        if (maxPower > 0) {
            final int targetPower = maxPower;
            for (List<GwentCard> row : allRows) {
                row.removeIf(c -> !c.isHero() && c.getBaseStrength() == targetPower);
            }
        }
    }

    public synchronized void actionPass(Player player) {
        if (state != GameState.PLAYING) return;
        boolean isP1 = player != null && player.getUniqueId().equals(player1);
        if (player != null && !isPlayerTurn(player)) return;
        if (player == null && (isPlayer1Turn || p2Passed)) return;
        updateLastActionTime();

        if (isP1) {
            p1Passed = true;
        } else {
            p2Passed = true;
        }

        if (player != null) SoundUtil.playClick(player);

        if (p1Passed && p2Passed) {
            evaluateRound();
        } else {
            switchTurn();
        }
    }

    private void switchTurn() {
        if (isPlayer1Turn) {
            if (!p2Passed) isPlayer1Turn = false;
        } else {
            if (!p1Passed) isPlayer1Turn = true;
        }

        syncViews();

        if (dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2) && !isPlayer1Turn && !p2Passed) {
            triggerBotGwentTurn();
        }
    }

    private void triggerBotGwentTurn() {
        if (!dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2) || state != GameState.PLAYING) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (this) {
                if (state != GameState.PLAYING || isPlayer1Turn || p2Passed) return;

                // The human is reading the tutorial: the bot waits instead of playing a whole round meanwhile.
                if (!viewingTutorial.isEmpty()) {
                    triggerBotGwentTurn();
                    return;
                }

                if (handP2.isEmpty()) {
                    botPass();
                    return;
                }

                int p1Power = calculateTotalPower(true);
                int p2Power = calculateTotalPower(false);

                // Human passed and the bot is already ahead: nothing more to gain.
                if (p1Passed && p2Power > p1Power) {
                    botPass();
                    return;
                }
                // Comfortably ahead and not richer in cards: keep the cards for the next round.
                if (!p1Passed && p2Power >= p1Power + 15 && handP2.size() <= handP1.size()) {
                    botPass();
                    return;
                }
                // Round 1 is lost anyway and the bot is poorer in cards: concede it and save the hand.
                if (!p1Passed && currentRound == 1 && p1Power >= p2Power + 20 && handP2.size() + 1 < handP1.size()) {
                    botPass();
                    return;
                }

                int chosenIdx = chooseBotCard(p1Power, p2Power);
                if (chosenIdx < 0) {
                    // Only cards that would hurt: pass instead of throwing them away.
                    botPass();
                    return;
                }
                actionPlayCard(null, chosenIdx);
            }
        }, 20L);
    }

    private void botPass() {
        updateLastActionTime();
        p2Passed = true;
        if (p1Passed) evaluateRound();
        else switchTurn();
    }

    /** Best card of the bot's hand by a simple value estimate; -1 if every card would do harm. */
    private int chooseBotCard(int p1Power, int p2Power) {
        int best = -1;
        double bestScore = -1;
        for (int i = 0; i < handP2.size(); i++) {
            double score = scoreBotCard(handP2.get(i));
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        // A harmful pick is still better than losing the round by passing early when the bot is behind.
        if (best < 0 && p1Passed && p2Power <= p1Power && !handP2.isEmpty()) {
            for (int i = 0; i < handP2.size(); i++) {
                if (handP2.get(i).isUnit()) return i;
            }
        }
        return best;
    }

    /** Positive = worth playing, negative = harmful or wasted. */
    private double scoreBotCard(GwentCard card) {
        return switch (card.getAbility()) {
            case SPY -> deckP2.size() >= 2 ? 6 : 2;
            case MEDIC -> card.getBaseStrength() + (graveyardP2.stream().anyMatch(GwentCard::isUnit) ? 5 : 0);
            case SCORCH -> {
                int max = -1;
                for (List<GwentCard> row : List.of(meleeP1, rangedP1, siegeP1, meleeP2, rangedP2, siegeP2)) {
                    for (GwentCard c : row) if (!c.isHero()) max = Math.max(max, c.getBaseStrength());
                }
                if (max < 0) yield -1;
                int enemy = 0, own = 0;
                for (List<GwentCard> row : List.of(meleeP1, rangedP1, siegeP1)) {
                    for (GwentCard c : row) if (!c.isHero() && c.getBaseStrength() == max) enemy += c.getBaseStrength();
                }
                for (List<GwentCard> row : List.of(meleeP2, rangedP2, siegeP2)) {
                    for (GwentCard c : row) if (!c.isHero() && c.getBaseStrength() == max) own += c.getBaseStrength();
                }
                int gain = enemy - own + (card.isUnit() ? card.getBaseStrength() : 0);
                yield gain >= 6 ? gain : -1;
            }
            case HORN -> {
                boolean already = switch (card.getRow()) {
                    case MELEE -> hornMeleeP2;
                    case RANGED -> hornRangedP2;
                    case SIEGE -> hornSiegeP2;
                    default -> true;
                };
                int rowPower = switch (card.getRow()) {
                    case MELEE -> calculateRowPower(meleeP2, false);
                    case RANGED -> calculateRowPower(rangedP2, false);
                    case SIEGE -> calculateRowPower(siegeP2, false);
                    default -> 0;
                };
                yield already || rowPower < 8 ? -1 : rowPower;
            }
            case DECOY -> -1;
            case FROST, FOG, RAIN -> {
                List<GwentCard> enemyRow = switch (card.getAbility()) {
                    case FROST -> meleeP1;
                    case FOG -> rangedP1;
                    default -> siegeP1;
                };
                List<GwentCard> ownRow = switch (card.getAbility()) {
                    case FROST -> meleeP2;
                    case FOG -> rangedP2;
                    default -> siegeP2;
                };
                boolean active = switch (card.getAbility()) {
                    case FROST -> frost;
                    case FOG -> fog;
                    default -> rain;
                };
                int enemyLoss = 0, ownLoss = 0;
                for (GwentCard c : enemyRow) if (!c.isHero()) enemyLoss += getCalculatedCardPower(c, true) - 1;
                for (GwentCard c : ownRow) if (!c.isHero()) ownLoss += getCalculatedCardPower(c, false) - 1;
                int gain = enemyLoss - ownLoss;
                yield active || gain < 6 ? -1 : gain;
            }
            case CLEAR -> {
                int ownLoss = 0, enemyLoss = 0;
                if (frost) { ownLoss += rowLoss(meleeP2, false); enemyLoss += rowLoss(meleeP1, true); }
                if (fog) { ownLoss += rowLoss(rangedP2, false); enemyLoss += rowLoss(rangedP1, true); }
                if (rain) { ownLoss += rowLoss(siegeP2, false); enemyLoss += rowLoss(siegeP1, true); }
                int gain = ownLoss - enemyLoss;
                yield gain >= 6 ? gain : -1;
            }
            default -> card.isUnit() ? card.getBaseStrength() : -1;
        };
    }

    /** Power a row loses to weather right now (base strength vs. 1 per non-hero card). */
    private int rowLoss(List<GwentCard> row, boolean isP1) {
        int loss = 0;
        for (GwentCard c : row) if (!c.isHero()) loss += c.getBaseStrength() - 1;
        return loss;
    }

    private void evaluateRound() {
        int p1Power = calculateTotalPower(true);
        int p2Power = calculateTotalPower(false);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);

        if (p1Power > p2Power) {
            roundsWonP1++;
            if (p1 != null) SoundUtil.playSuccess(p1);
            if (p2 != null) SoundUtil.playLoss(p2);
        } else if (p2Power > p1Power) {
            roundsWonP2++;
            if (p2 != null) SoundUtil.playSuccess(p2);
            if (p1 != null) SoundUtil.playLoss(p1);
        } else {
            roundsWonP1++;
            roundsWonP2++;
            if (p1 != null) SoundUtil.playDraw(p1);
            if (p2 != null) SoundUtil.playDraw(p2);
        }

        if (roundsWonP1 >= 2 && roundsWonP2 < 2) {
            endWithWinner(player1);
            return;
        } else if (roundsWonP2 >= 2 && roundsWonP1 < 2) {
            endWithWinner(player2);
            return;
        } else if (roundsWonP1 >= 2 && roundsWonP2 >= 2) {
            endWithDraw();
            return;
        }

        if (currentRound >= 3) {
            if (roundsWonP1 > roundsWonP2) endWithWinner(player1);
            else if (roundsWonP2 > roundsWonP1) endWithWinner(player2);
            else endWithDraw();
            return;
        }

        // Clear battlefield to graveyards
        graveyardP1.addAll(meleeP1); meleeP1.clear();
        graveyardP1.addAll(rangedP1); rangedP1.clear();
        graveyardP1.addAll(siegeP1); siegeP1.clear();

        graveyardP2.addAll(meleeP2); meleeP2.clear();
        graveyardP2.addAll(rangedP2); rangedP2.clear();
        graveyardP2.addAll(siegeP2); siegeP2.clear();

        // Reset weather & horns & pass
        frost = false;
        fog = false;
        rain = false;
        hornMeleeP1 = hornRangedP1 = hornSiegeP1 = false;
        hornMeleeP2 = hornRangedP2 = hornSiegeP2 = false;

        p1Passed = false;
        p2Passed = false;
        currentRound++;

        // The winner of the finished round opens the next one; after a draw the starter alternates.
        // (Before this the turn simply stayed with whoever ended the round, so a round could open on the bot's
        // turn with nothing scheduled - the bot never moved and the AFK timer then lost the game for it.)
        boolean p1StartsNext = p1Power > p2Power || (p1Power == p2Power && !roundStarterP1);
        roundStarterP1 = p1StartsNext;
        isPlayer1Turn = p1StartsNext;

        // Draw new cards for new round (2 cards for R2, 1 card for R3)
        int drawCount = (currentRound == 2) ? 2 : 1;
        for (int i = 0; i < drawCount && !deckP1.isEmpty(); i++) handP1.add(deckP1.remove(0));
        for (int i = 0; i < drawCount && !deckP2.isEmpty(); i++) handP2.add(deckP2.remove(0));

        // If both players have empty hands and decks, finish
        if (handP1.isEmpty() && handP2.isEmpty()) {
            if (roundsWonP1 > roundsWonP2) endWithWinner(player1);
            else if (roundsWonP2 > roundsWonP1) endWithWinner(player2);
            else endWithDraw();
            return;
        }

        // A starter with an empty hand has nothing to play: he passes at once, the other side continues.
        if (isPlayer1Turn && handP1.isEmpty()) {
            p1Passed = true;
            isPlayer1Turn = false;
        } else if (!isPlayer1Turn && handP2.isEmpty()) {
            p2Passed = true;
            isPlayer1Turn = true;
        }
        updateLastActionTime();
        syncViews();

        if (dev.lovelace.loveactivities.manager.SessionManager.isNpc(player2) && !isPlayer1Turn && !p2Passed) {
            triggerBotGwentTurn();
        }
    }

    public void resign(Player player) {
        if (state != GameState.PLAYING) return;
        UUID loser = player.getUniqueId();
        UUID winner = getOpponent(loser);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null) plugin.getLocaleManager().send(p1, "autolose_surrender", Map.of("player", player.getName(), "game", "Гвинт"));
        if (p2 != null) plugin.getLocaleManager().send(p2, "autolose_surrender", Map.of("player", player.getName(), "game", "Гвинт"));

        endWithWinner(winner);
    }

    public void openTutorial(Player player) {
        if (state != GameState.PLAYING) return;
        viewingTutorial.add(player.getUniqueId());

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1 && guiP1 != null) guiP1.setSwitchingInventory(true);
        if (!isP1 && guiP2 != null) guiP2.setSwitchingInventory(true);

        List<List<String>> pages = List.of(
                List.of(
                        "<yellow><bold>Гвинт (Minecraft Edition)</bold></yellow>",
                        "<gray>Матч длится до 2 побед в раундах (Best of 3).</gray>",
                        "<gray>Игроки по очереди разыгрывают по 1 карте на поле боя.</gray>",
                        "<gray>Побеждает игрок с наибольшей суммарной силой рядов.</gray>",
                        "",
                        "<white>Боевые ряды:</white>",
                        "<yellow>• Ближний бой</yellow> (Стив, Голем, Зомби)",
                        "<yellow>• Дальний бой</yellow> (Дракон, Скелет, Ведьма)",
                        "<yellow>• Осадный ряд</yellow> (Иссушитель, Гаст, Ифрит)"
                ),
                List.of(
                        "<yellow><bold>Способности карт</bold></yellow>",
                        "<gold>• Герои (Стив, Дракон, Варден):</gold> <gray>иммунитет к погоде и казни.</gray>",
                        "<gold>• Шпионы (Эндермен, Торговец):</gold> <gray>дают силу врагу, но берут 2 карты.</gray>",
                        "<gold>• Медики (Голем, Ведьма):</gold> <gray>воскрешают отряд из сброса.</gray>",
                        "<gold>• Казнь (Крипер, TNT):</gold> <gray>уничтожает сильнейшие карты.</gray>",
                        "<gold>• Погода (Снегопад, Туман, Ливень):</gold> <gray>снижает силу ряда до 1.</gray>",
                        "",
                        "<red>Пас:</red> <gray>если вы спасовали, то больше не можете ходить в этом раунде.</gray>"
                )
        );

        new TutorialGUI(player, GameType.GWENT, pages, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                viewingTutorial.remove(player.getUniqueId());
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

    private final long sessionStartTime = System.currentTimeMillis();

    public void reopenGui(Player player) {
        GwentGUI gui = player.getUniqueId().equals(player1) ? guiP1 : guiP2;
        if (gui != null) {
            gui.setSwitchingInventory(true);
            gui.open();
        }
    }

    @Override
    public void onPlayerClose(Player player) {
        if (state != GameState.PLAYING || (viewingTutorial != null && viewingTutorial.contains(player.getUniqueId()))) {
            return;
        }
        if (System.currentTimeMillis() - sessionStartTime < 2000L) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == GameState.PLAYING && player.isOnline()) {
                    if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof AbstractGUI)) {
                        reopenGui(player);
                    }
                }
            }, 2L);
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
        }, 15L);
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
