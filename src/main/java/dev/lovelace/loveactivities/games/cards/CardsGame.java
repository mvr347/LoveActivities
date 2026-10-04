package dev.lovelace.loveactivities.games.cards;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.api.GameSession;
import dev.lovelace.loveactivities.api.GameState;
import dev.lovelace.loveactivities.api.GameType;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.gui.TutorialGUI;
import dev.lovelace.loveactivities.manager.SessionManager;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;

public class CardsGame implements GameSession {

    public enum PokerStage {
        PREFLOP("Префлоп (Раздача)"),
        FLOP("Флоп (3 карты)"),
        TURN("Тёрн (4-я карта)"),
        RIVER("Ривер (5-я карта)"),
        SHOWDOWN("Вскрытие карт");

        private final String nameRu;

        PokerStage(String nameRu) {
            this.nameRu = nameRu;
        }

        public String getNameRu() {
            return nameRu;
        }
    }

    private final LoveActivities plugin = LoveActivities.getInstance();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID player1;
    private final UUID player2;
    private long bet;
    private GameState state = GameState.PLAYING;
    private long lastActionTime = System.currentTimeMillis();

    private CardsMode mode = CardsMode.DURAK;

    // Durak state
    private final DurakBoard durakBoard = new DurakBoard();
    private final List<PlayingCard> handP1 = new ArrayList<>();
    private final List<PlayingCard> handP2 = new ArrayList<>();
    private final Set<PlayingCard> knownToOpponentP1 = new HashSet<>();
    private final Set<PlayingCard> knownToOpponentP2 = new HashSet<>();
    private boolean p1Attacking = true;

    // War state
    private int warRound = 1;
    private int warWonP1 = 0;
    private int warWonP2 = 0;
    private PlayingCard warCardP1 = null;
    private PlayingCard warCardP2 = null;
    private String warResultText = "";

    // Poker (Texas Hold'em) state
    private final List<PlayingCard> pokerDeck = new ArrayList<>();
    private final List<PlayingCard> pokerHoleP1 = new ArrayList<>();
    private final List<PlayingCard> pokerHoleP2 = new ArrayList<>();
    private final List<PlayingCard> pokerCommunityCards = new ArrayList<>();
    private PokerStage pokerStage = PokerStage.PREFLOP;
    private boolean pokerP1Turn = true;
    private boolean pokerP1Acted = false;
    private boolean pokerP2Acted = false;
    private long pokerPot = 0L;

    private final Set<UUID> viewingTutorial = new HashSet<>();

    private DurakGUI durakGuiP1;
    private DurakGUI durakGuiP2;
    private WarGUI warGuiP1;
    private WarGUI warGuiP2;
    private TexasHoldemGUI pokerGuiP1;
    private TexasHoldemGUI pokerGuiP2;

    public CardsGame(Player p1, Player p2, long bet) {
        this(p1.getUniqueId(), p2 != null ? p2.getUniqueId() : SessionManager.NPC_UUID, bet, CardsMode.DURAK);
    }

    public CardsGame(Player p1, UUID player2Uuid, long bet) {
        this(p1.getUniqueId(), player2Uuid, bet, CardsMode.DURAK);
    }

    public CardsGame(UUID p1, UUID p2, long bet, CardsMode mode) {
        this.player1 = p1;
        this.player2 = p2 != null ? p2 : SessionManager.NPC_UUID;
        this.bet = bet;
        this.mode = mode != null ? mode : CardsMode.DURAK;

        if (this.mode == CardsMode.DURAK) {
            setupDurak();
        } else if (this.mode == CardsMode.WAR) {
            setupWar();
        } else {
            setupPoker();
        }
    }

    public boolean isNpcMatch() {
        return SessionManager.isNpc(player2);
    }

    private void setupDurak() {
        handP1.clear();
        handP2.clear();
        knownToOpponentP1.clear();
        knownToOpponentP2.clear();

        durakBoard.refillHand(handP1);
        durakBoard.refillHand(handP2);

        PlayingCard.Suit trump = durakBoard.getTrumpSuit();
        int lowestTrumpP1 = 999;
        int lowestTrumpP2 = 999;

        for (PlayingCard c : handP1) {
            if (c.getSuit() == trump && c.getValue() < lowestTrumpP1) lowestTrumpP1 = c.getValue();
        }
        for (PlayingCard c : handP2) {
            if (c.getSuit() == trump && c.getValue() < lowestTrumpP2) lowestTrumpP2 = c.getValue();
        }

        if (lowestTrumpP2 < lowestTrumpP1) {
            p1Attacking = false;
        } else {
            p1Attacking = true;
        }
    }

    private void setupWar() {
        List<PlayingCard> fullDeck = new ArrayList<>();
        for (PlayingCard.Suit s : PlayingCard.Suit.values()) {
            for (PlayingCard.Rank r : PlayingCard.Rank.values()) {
                fullDeck.add(new PlayingCard(s, r));
            }
        }
        Collections.shuffle(fullDeck);

        for (int i = 0; i < 18 && !fullDeck.isEmpty(); i++) handP1.add(fullDeck.remove(0));
        for (int i = 0; i < 18 && !fullDeck.isEmpty(); i++) handP2.add(fullDeck.remove(0));
    }

    private void setupPoker() {
        pokerDeck.clear();
        for (PlayingCard.Suit s : PlayingCard.Suit.values()) {
            for (PlayingCard.Rank r : PlayingCard.Rank.values()) {
                pokerDeck.add(new PlayingCard(s, r));
            }
        }
        Collections.shuffle(pokerDeck);

        pokerHoleP1.clear();
        pokerHoleP2.clear();
        pokerCommunityCards.clear();

        // 2 hole cards each
        if (pokerDeck.size() >= 4) {
            pokerHoleP1.add(pokerDeck.remove(0));
            pokerHoleP1.add(pokerDeck.remove(0));
            pokerHoleP2.add(pokerDeck.remove(0));
            pokerHoleP2.add(pokerDeck.remove(0));
        }

        pokerStage = PokerStage.PREFLOP;
        pokerP1Turn = true;
        pokerP1Acted = false;
        pokerP2Acted = false;
        pokerPot = bet > 0 ? bet * 2 : 0L;
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public GameType getGameType() {
        return GameType.CARDS;
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
        Player p2 = isNpcMatch() ? null : Bukkit.getPlayer(player2);

        if (p1 != null && p1.isOnline()) {
            if (mode == CardsMode.DURAK) {
                this.durakGuiP1 = new DurakGUI(p1, this);
                this.durakGuiP1.open();
            } else if (mode == CardsMode.WAR) {
                this.warGuiP1 = new WarGUI(p1, this);
                this.warGuiP1.open();
            } else {
                this.pokerGuiP1 = new TexasHoldemGUI(p1, this);
                this.pokerGuiP1.open();
            }
        }
        if (p2 != null && p2.isOnline()) {
            if (mode == CardsMode.DURAK) {
                this.durakGuiP2 = new DurakGUI(p2, this);
                this.durakGuiP2.open();
            } else if (mode == CardsMode.WAR) {
                this.warGuiP2 = new WarGUI(p2, this);
                this.warGuiP2.open();
            } else {
                this.pokerGuiP2 = new TexasHoldemGUI(p2, this);
                this.pokerGuiP2.open();
            }
        }

        if (isNpcMatch() && mode == CardsMode.DURAK && !p1Attacking) {
            triggerBotDurakAction();
        } else if (isNpcMatch() && mode == CardsMode.POKER && !pokerP1Turn) {
            triggerBotPokerAction();
        }
    }

    public CardsMode getMode() {
        return mode;
    }

    public DurakBoard getDurakBoard() {
        return durakBoard;
    }

    public List<PlayingCard> getHandP1() {
        return handP1;
    }

    public List<PlayingCard> getHandP2() {
        return handP2;
    }

    public Set<PlayingCard> getKnownToOpponentP1() {
        return Collections.unmodifiableSet(knownToOpponentP1);
    }

    public Set<PlayingCard> getKnownToOpponentP2() {
        return Collections.unmodifiableSet(knownToOpponentP2);
    }

    public boolean isP1Attacking() {
        return p1Attacking;
    }

    public int getWarRound() {
        return warRound;
    }

    public int getWarWonP1() {
        return warWonP1;
    }

    public int getWarWonP2() {
        return warWonP2;
    }

    public PlayingCard getWarCardP1() {
        return warCardP1;
    }

    public PlayingCard getWarCardP2() {
        return warCardP2;
    }

    public String getWarResultText() {
        return warResultText;
    }

    public List<PlayingCard> getPokerHoleP1() {
        return pokerHoleP1;
    }

    public List<PlayingCard> getPokerHoleP2() {
        return pokerHoleP2;
    }

    public List<PlayingCard> getPokerCommunityCards() {
        return pokerCommunityCards;
    }

    public long getPokerPot() {
        return pokerPot;
    }

    public String getPokerStageName() {
        return pokerStage.getNameRu();
    }

    public boolean isPokerShowdown() {
        return pokerStage == PokerStage.SHOWDOWN;
    }

    public boolean isPokerPlayerTurn(Player player) {
        if (state != GameState.PLAYING || pokerStage == PokerStage.SHOWDOWN) return false;
        boolean isP1 = player.getUniqueId().equals(player1);
        return isP1 ? pokerP1Turn : !pokerP1Turn;
    }

    public int getUndefendedCount() {
        int count = 0;
        for (DurakBoard.TablePair p : durakBoard.getTable()) {
            if (p.defense() == null) count++;
        }
        return count;
    }

    // --- Durak Actions ---
    public synchronized void actionDurakPlayCard(Player player, int cardIndex) {
        if (state != GameState.PLAYING) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        List<PlayingCard> hand = isP1 ? handP1 : handP2;
        List<PlayingCard> oppHand = isP1 ? handP2 : handP1;

        if (cardIndex < 0 || cardIndex >= hand.size()) return;

        PlayingCard card = hand.get(cardIndex);
        boolean isMyAttack = isP1 == p1Attacking;

        if (isMyAttack) {
            if (durakBoard.attack(card, oppHand.size() + getUndefendedCount())) {
                hand.remove(cardIndex);
                if (isP1) {
                    knownToOpponentP1.remove(card);
                } else {
                    knownToOpponentP2.remove(card);
                }
                SoundUtil.playCardDraw(player);
                syncViews();

                if (isNpcMatch() && isP1) {
                    triggerBotDurakAction();
                }
            } else {
                SoundUtil.playError(player);
            }
        } else {
            List<DurakBoard.TablePair> table = durakBoard.getTable();
            int undefendedIdx = -1;
            for (int i = 0; i < table.size(); i++) {
                if (table.get(i).defense() == null) {
                    undefendedIdx = i;
                    break;
                }
            }

            if (undefendedIdx != -1 && durakBoard.defend(undefendedIdx, card)) {
                hand.remove(cardIndex);
                if (isP1) {
                    knownToOpponentP1.remove(card);
                } else {
                    knownToOpponentP2.remove(card);
                }
                SoundUtil.playCardDraw(player);
                syncViews();

                if (isNpcMatch() && isP1 && !p1Attacking) {
                    triggerBotDurakAction();
                }
            } else {
                SoundUtil.playError(player);
            }
        }

        checkDurakWinCondition();
    }

    public synchronized void actionDurakBito(Player player) {
        if (state != GameState.PLAYING) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1 != p1Attacking || !durakBoard.isAllDefended()) {
            SoundUtil.playError(player);
            return;
        }

        durakBoard.bito();
        List<PlayingCard> drawnAttacker = durakBoard.refillHand(p1Attacking ? handP1 : handP2);
        List<PlayingCard> drawnDefender = durakBoard.refillHand(p1Attacking ? handP2 : handP1);

        PlayingCard trump = durakBoard.getTrumpCard();
        if (trump != null) {
            if (drawnAttacker.contains(trump)) {
                (p1Attacking ? knownToOpponentP1 : knownToOpponentP2).add(trump);
            }
            if (drawnDefender.contains(trump)) {
                (p1Attacking ? knownToOpponentP2 : knownToOpponentP1).add(trump);
            }
        }
        knownToOpponentP1.retainAll(handP1);
        knownToOpponentP2.retainAll(handP2);

        p1Attacking = !p1Attacking;
        SoundUtil.playSuccess(player);
        syncViews();

        checkDurakWinCondition();

        if (isNpcMatch() && !p1Attacking) {
            triggerBotDurakAction();
        }
    }

    public synchronized void actionDurakTake(Player player) {
        if (state != GameState.PLAYING) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1 == p1Attacking || durakBoard.getTable().isEmpty()) {
            SoundUtil.playError(player);
            return;
        }

        List<PlayingCard> taken = durakBoard.take(isP1 ? handP1 : handP2);
        if (isP1) {
            knownToOpponentP1.addAll(taken);
        } else {
            knownToOpponentP2.addAll(taken);
        }

        List<PlayingCard> drawnAttacker = durakBoard.refillHand(p1Attacking ? handP1 : handP2);
        PlayingCard trump = durakBoard.getTrumpCard();
        if (trump != null && drawnAttacker.contains(trump)) {
            (p1Attacking ? knownToOpponentP1 : knownToOpponentP2).add(trump);
        }
        knownToOpponentP1.retainAll(handP1);
        knownToOpponentP2.retainAll(handP2);

        SoundUtil.playClick(player);
        syncViews();

        checkDurakWinCondition();

        if (isNpcMatch() && !p1Attacking) {
            triggerBotDurakAction();
        }
    }

    private void triggerBotDurakAction() {
        if (!isNpcMatch() || state != GameState.PLAYING) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;

            if (!p1Attacking) {
                if (durakBoard.getTable().isEmpty()) {
                    int bestIdx = findBestBotAttackCard();
                    if (bestIdx != -1) {
                        PlayingCard card = handP2.remove(bestIdx);
                        knownToOpponentP2.remove(card);
                        durakBoard.attack(card, handP1.size() + getUndefendedCount());
                        Player p1 = Bukkit.getPlayer(player1);
                        if (p1 != null) SoundUtil.playCardDraw(p1);
                        syncViews();
                    }
                } else if (durakBoard.isAllDefended()) {
                    int tossIdx = findBestBotTossCard();
                    if (tossIdx != -1 && durakBoard.getTable().size() < 6 && durakBoard.getTable().size() < handP1.size()) {
                        PlayingCard card = handP2.remove(tossIdx);
                        knownToOpponentP2.remove(card);
                        durakBoard.attack(card, handP1.size() + getUndefendedCount());
                        Player p1 = Bukkit.getPlayer(player1);
                        if (p1 != null) SoundUtil.playCardDraw(p1);
                        syncViews();
                    } else {
                        durakBoard.bito();
                        List<PlayingCard> drawnBot = durakBoard.refillHand(handP2);
                        List<PlayingCard> drawnPlayer = durakBoard.refillHand(handP1);
                        PlayingCard trump = durakBoard.getTrumpCard();
                        if (trump != null) {
                            if (drawnBot.contains(trump)) knownToOpponentP2.add(trump);
                            if (drawnPlayer.contains(trump)) knownToOpponentP1.add(trump);
                        }
                        knownToOpponentP1.retainAll(handP1);
                        knownToOpponentP2.retainAll(handP2);

                        p1Attacking = true;
                        Player p1 = Bukkit.getPlayer(player1);
                        if (p1 != null) SoundUtil.playSuccess(p1);
                        syncViews();
                    }
                }
            } else {
                List<DurakBoard.TablePair> table = durakBoard.getTable();
                int undefendedIdx = -1;
                for (int i = 0; i < table.size(); i++) {
                    if (table.get(i).defense() == null) {
                        undefendedIdx = i;
                        break;
                    }
                }

                if (undefendedIdx != -1) {
                    PlayingCard attackCard = table.get(undefendedIdx).attack();
                    int defCardIdx = findBestBotDefendCard(attackCard);

                    if (defCardIdx != -1) {
                        PlayingCard card = handP2.remove(defCardIdx);
                        knownToOpponentP2.remove(card);
                        durakBoard.defend(undefendedIdx, card);
                        Player p1 = Bukkit.getPlayer(player1);
                        if (p1 != null) SoundUtil.playCardDraw(p1);
                        syncViews();
                    } else {
                        List<PlayingCard> taken = durakBoard.take(handP2);
                        knownToOpponentP2.addAll(taken);
                        List<PlayingCard> drawnPlayer = durakBoard.refillHand(handP1);
                        PlayingCard trump = durakBoard.getTrumpCard();
                        if (trump != null && drawnPlayer.contains(trump)) {
                            knownToOpponentP1.add(trump);
                        }
                        knownToOpponentP1.retainAll(handP1);
                        knownToOpponentP2.retainAll(handP2);

                        Player p1 = Bukkit.getPlayer(player1);
                        if (p1 != null) SoundUtil.playClick(p1);
                        syncViews();
                    }
                }
            }

            checkDurakWinCondition();
        }, 20L);
    }

    private int findBestBotAttackCard() {
        int bestIdx = -1;
        int lowestVal = 999;
        PlayingCard.Suit trump = durakBoard.getTrumpSuit();

        for (int i = 0; i < handP2.size(); i++) {
            PlayingCard c = handP2.get(i);
            int score = (c.getSuit() == trump ? 100 : 0) + c.getValue();
            if (score < lowestVal) {
                lowestVal = score;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private int findBestBotTossCard() {
        int bestIdx = -1;
        int lowestVal = 999;
        PlayingCard.Suit trump = durakBoard.getTrumpSuit();

        for (int i = 0; i < handP2.size(); i++) {
            PlayingCard c = handP2.get(i);
            if (durakBoard.canAttackWith(c, handP1.size() + getUndefendedCount())) {
                int score = (c.getSuit() == trump ? 100 : 0) + c.getValue();
                if (score < lowestVal) {
                    lowestVal = score;
                    bestIdx = i;
                }
            }
        }
        return bestIdx;
    }

    private int findBestBotDefendCard(PlayingCard attack) {
        int bestIdx = -1;
        int lowestVal = 999;
        PlayingCard.Suit trump = durakBoard.getTrumpSuit();

        for (int i = 0; i < handP2.size(); i++) {
            PlayingCard c = handP2.get(i);
            if (c.canBeat(attack, trump)) {
                int score = (c.getSuit() == trump ? 100 : 0) + c.getValue();
                if (score < lowestVal) {
                    lowestVal = score;
                    bestIdx = i;
                }
            }
        }
        return bestIdx;
    }

    private void checkDurakWinCondition() {
        if (durakBoard.getDeckRemaining() == 0) {
            if (handP1.isEmpty() && handP2.isEmpty()) {
                endWithDraw();
                return;
            }

            // If there are still undefended attack cards on table, defender must have the chance to defend or take!
            if (durakBoard.getUndefendedCount() > 0) {
                boolean isP1Defender = !p1Attacking;
                List<PlayingCard> defHand = isP1Defender ? handP1 : handP2;
                if (defHand.isEmpty()) {
                    // Defender has no cards in hand to beat the attack -> attacker wins
                    endWithWinner(p1Attacking ? player1 : player2);
                }
                return;
            }

            if (handP1.isEmpty() && !handP2.isEmpty()) {
                endWithWinner(player1);
            } else if (handP2.isEmpty() && !handP1.isEmpty()) {
                endWithWinner(player2);
            }
        }
    }

    // --- Texas Hold'em Actions ---
    public synchronized void actionPokerCheck(Player player) {
        if (!isPokerPlayerTurn(player)) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1) pokerP1Acted = true; else pokerP2Acted = true;

        player.sendActionBar(TextUtil.parse("<gray>Вы выбрали: Чек / Пропуск хода</gray>"));
        SoundUtil.playClick(player);

        if (pokerP1Acted && pokerP2Acted) {
            advancePokerStage();
        } else {
            pokerP1Turn = !pokerP1Turn;
            syncViews();
            if (isNpcMatch() && !pokerP1Turn) {
                triggerBotPokerAction();
            }
        }
    }

    public synchronized void actionPokerRaise(Player player) {
        if (!isPokerPlayerTurn(player)) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        long addition = Math.max(plugin.getConfigManager().getPokerMinRaise(), pokerPot / 10);
        pokerPot += addition;

        if (isP1) {
            pokerP1Acted = true;
            pokerP2Acted = false;
        } else {
            pokerP2Acted = true;
            pokerP1Acted = false;
        }

        player.sendActionBar(TextUtil.parse("<gold><bold>▲ Вы повысили ставки на +" + addition + " монет (Ставки: " + pokerPot + ")</bold></gold>"));
        player.sendMessage(TextUtil.parse("<gold>▲ Вы повысили ставки на <yellow>+" + addition + "</yellow> монет! (Текущие ставки: <yellow>" + pokerPot + "</yellow>)</gold>"));
        SoundUtil.playSuccess(player);

        Player opp = Bukkit.getPlayer(getOpponent(player.getUniqueId()));
        if (opp != null && !isNpcMatch()) {
            opp.sendMessage(TextUtil.parse("<gold>Соперник повысил ставки на <yellow>+" + addition + "</yellow> монет! (Ставки: <yellow>" + pokerPot + "</yellow>)</gold>"));
            opp.sendActionBar(TextUtil.parse("<gold>▲ Соперник повысил ставку (Ставки: " + pokerPot + ")</gold>"));
            SoundUtil.playChallenge(opp);
        }

        pokerP1Turn = !pokerP1Turn;
        syncViews();

        if (isNpcMatch() && !pokerP1Turn) {
            triggerBotPokerAction();
        }
    }

    public synchronized void actionPokerFold(Player player) {
        if (state != GameState.PLAYING) return;
        UUID loser = player.getUniqueId();
        UUID winner = getOpponent(loser);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null) plugin.getLocaleManager().send(p1, "autolose_surrender", Map.of("player", player.getName(), "game", "Покер"));
        if (p2 != null && !isNpcMatch()) plugin.getLocaleManager().send(p2, "autolose_surrender", Map.of("player", player.getName(), "game", "Покер"));

        endWithWinner(winner);
    }

    private void advancePokerStage() {
        pokerP1Acted = false;
        pokerP2Acted = false;
        pokerP1Turn = true;

        switch (pokerStage) {
            case PREFLOP -> {
                // Deal Flop (3 cards)
                if (pokerDeck.size() >= 3) {
                    pokerCommunityCards.add(pokerDeck.remove(0));
                    pokerCommunityCards.add(pokerDeck.remove(0));
                    pokerCommunityCards.add(pokerDeck.remove(0));
                }
                pokerStage = PokerStage.FLOP;
                syncViews();
            }
            case FLOP -> {
                // Deal Turn (1 card)
                if (!pokerDeck.isEmpty()) {
                    pokerCommunityCards.add(pokerDeck.remove(0));
                }
                pokerStage = PokerStage.TURN;
                syncViews();
            }
            case TURN -> {
                // Deal River (1 card)
                if (!pokerDeck.isEmpty()) {
                    pokerCommunityCards.add(pokerDeck.remove(0));
                }
                pokerStage = PokerStage.RIVER;
                syncViews();
            }
            case RIVER -> {
                // Showdown!
                pokerStage = PokerStage.SHOWDOWN;
                syncViews();
                evaluatePokerShowdown();
            }
            case SHOWDOWN -> {}
        }
    }

    private void evaluatePokerShowdown() {
        List<PlayingCard> allP1 = new ArrayList<>(pokerHoleP1);
        allP1.addAll(pokerCommunityCards);

        List<PlayingCard> allP2 = new ArrayList<>(pokerHoleP2);
        allP2.addAll(pokerCommunityCards);

        PokerHandEvaluator.PokerScore score1 = PokerHandEvaluator.evaluate7Cards(allP1);
        PokerHandEvaluator.PokerScore score2 = PokerHandEvaluator.evaluate7Cards(allP2);

        int cmp = score1.compareTo(score2);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        String p1Desc = score1.description();
        String p2Desc = score2.description();
        String p2Name = isNpcMatch() ? SessionManager.NPC_NAME : (p2 != null ? p2.getName() : "Игрок 2");

        if (p1 != null) {
            p1.sendMessage(TextUtil.parse("<yellow><bold>--- ВСКРЫТИЕ КАРТ (ШОУДАУН) ---</bold></yellow>"));
            p1.sendMessage(TextUtil.parse("<green>Ваша комбинация:</green> <white>" + p1Desc + "</white>"));
            p1.sendMessage(TextUtil.parse("<yellow>Комбинация " + p2Name + ":</yellow> <white>" + p2Desc + "</white>"));
            if (cmp > 0) {
                p1.sendActionBar(TextUtil.parse("<green><bold>🏆 Ваша комбинация побеждает: " + p1Desc + "!</bold></green>"));
            } else if (cmp < 0) {
                p1.sendActionBar(TextUtil.parse("<red><bold>☠ " + p2Name + " побеждает с: " + p2Desc + "</bold></red>"));
            } else {
                p1.sendActionBar(TextUtil.parse("<yellow><bold>🤝 Ничья комбинаций: " + p1Desc + "</bold></yellow>"));
            }
        }
        if (p2 != null && !isNpcMatch()) {
            p2.sendMessage(TextUtil.parse("<yellow><bold>--- ВСКРЫТИЕ КАРТ (ШОУДАУН) ---</bold></yellow>"));
            p2.sendMessage(TextUtil.parse("<green>Ваша комбинация:</green> <white>" + p2Desc + "</white>"));
            p2.sendMessage(TextUtil.parse("<yellow>Комбинация соперника:</yellow> <white>" + p1Desc + "</white>"));
            if (cmp < 0) {
                p2.sendActionBar(TextUtil.parse("<green><bold>🏆 Ваша комбинация побеждает: " + p2Desc + "!</bold></green>"));
            } else if (cmp > 0) {
                p2.sendActionBar(TextUtil.parse("<red><bold>☠ Соперник побеждает с: " + p1Desc + "</bold></red>"));
            } else {
                p2.sendActionBar(TextUtil.parse("<yellow><bold>🤝 Ничья комбинаций: " + p2Desc + "</bold></yellow>"));
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING) return;
            if (cmp > 0) {
                endWithWinner(player1);
            } else if (cmp < 0) {
                endWithWinner(player2);
            } else {
                endWithDraw();
            }
        }, 50L); // 2.5-second showdown reveal pause
    }

    private void triggerBotPokerAction() {
        if (!isNpcMatch() || state != GameState.PLAYING) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.PLAYING || pokerP1Turn) return;

            List<PlayingCard> allBot = new ArrayList<>(pokerHoleP2);
            allBot.addAll(pokerCommunityCards);
            PokerHandEvaluator.PokerScore botScore = PokerHandEvaluator.evaluate7Cards(allBot);

            // Bot decides action
            if (botScore.rank().getScore() >= 4 && Math.random() < 0.40) {
                // Raise if strong
                pokerPot += Math.max(plugin.getConfigManager().getPokerMinRaise(), pokerPot / 10);
                pokerP2Acted = true;
                pokerP1Acted = false;
                pokerP1Turn = true;
                Player p1 = Bukkit.getPlayer(player1);
                if (p1 != null) SoundUtil.playChallenge(p1);
            } else {
                // Check / Call
                pokerP2Acted = true;
                if (pokerP1Acted) {
                    advancePokerStage();
                    return;
                } else {
                    pokerP1Turn = true;
                }
            }
            syncViews();
        }, 20L);
    }

    // --- War Actions ---
    public synchronized void actionWarFlip(Player player) {
        if (state != GameState.PLAYING) return;
        updateLastActionTime();

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1 && warCardP1 == null && !handP1.isEmpty()) {
            warCardP1 = handP1.remove(0);
            SoundUtil.playCardDraw(player);

            if (isNpcMatch() && !handP2.isEmpty()) {
                warCardP2 = handP2.remove(0);
            }
        } else if (!isP1 && warCardP2 == null && !handP2.isEmpty()) {
            warCardP2 = handP2.remove(0);
            SoundUtil.playCardDraw(player);
        }

        syncViews();

        if (warCardP1 != null && warCardP2 != null) {
            if (warCardP1.getValue() > warCardP2.getValue()) {
                warWonP1 += 2;
                warResultText = "Победа P1!";
            } else if (warCardP2.getValue() > warCardP1.getValue()) {
                warWonP2 += 2;
                warResultText = "Победа P2!";
            } else {
                warWonP1 += 1;
                warWonP2 += 1;
                warResultText = "Ничья (Война)!";
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state != GameState.PLAYING) return;

                warCardP1 = null;
                warCardP2 = null;
                warResultText = "";
                warRound++;

                if (handP1.isEmpty() || handP2.isEmpty()) {
                    if (warWonP1 > warWonP2) endWithWinner(player1);
                    else if (warWonP2 > warWonP1) endWithWinner(player2);
                    else endWithDraw();
                } else {
                    syncViews();
                }
            }, 35L);
        }
    }

    public void resign(Player player) {
        if (state != GameState.PLAYING) return;
        UUID loser = player.getUniqueId();
        UUID winner = getOpponent(loser);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        if (p1 != null) plugin.getLocaleManager().send(p1, "autolose_surrender", Map.of("player", player.getName(), "game", "Карты"));
        if (p2 != null && !isNpcMatch()) plugin.getLocaleManager().send(p2, "autolose_surrender", Map.of("player", player.getName(), "game", "Карты"));

        endWithWinner(winner);
    }

    public void openTutorial(Player player) {
        if (state != GameState.PLAYING) return;
        viewingTutorial.add(player.getUniqueId());

        boolean isP1 = player.getUniqueId().equals(player1);
        if (isP1 && durakGuiP1 != null) durakGuiP1.setSwitchingInventory(true);
        if (!isP1 && durakGuiP2 != null) durakGuiP2.setSwitchingInventory(true);
        if (isP1 && warGuiP1 != null) warGuiP1.setSwitchingInventory(true);
        if (!isP1 && warGuiP2 != null) warGuiP2.setSwitchingInventory(true);
        if (isP1 && pokerGuiP1 != null) pokerGuiP1.setSwitchingInventory(true);
        if (!isP1 && pokerGuiP2 != null) pokerGuiP2.setSwitchingInventory(true);

        List<List<String>> pages = (mode == CardsMode.POKER) ? List.of(
                List.of(
                        "<yellow><bold>Техасский Холдем (Покер на двоих)</bold></yellow>",
                        "<gray>Каждый игрок получает по 2 карманные карты.</gray>",
                        "<gray>На столе открываются 5 общих карт в 3 этапа:</gray>",
                        "<yellow>• Флоп:</yellow> <gray>первые 3 общие карты</gray>",
                        "<yellow>• Тёрн:</yellow> <gray>4-я общая карта</gray>",
                        "<yellow>• Ривер:</yellow> <gray>5-я общая карта</gray>",
                        "<green>• Вскрытие (Шоудаун):</green> <gray>сравнение лучших 5-карточных рук.</gray>"
                ),
                List.of(
                        "<yellow><bold>Старшинство комбинаций Покера</bold></yellow>",
                        "<gray>1. <gold>Роял-Флеш</gold> (10-J-Q-K-A одной масти)</gray>",
                        "<gray>2. <gold>Стрит-Флеш</gold> (5 карт подряд одной масти)</gray>",
                        "<gray>3. <gold>Каре</gold> (4 карты одного ранга)</gray>",
                        "<gray>4. <gold>Фулл-Хаус</gold> (3 + 2 карты)</gray>",
                        "<gray>5. <yellow>Флеш</yellow> (5 карт одной масти)</gray>",
                        "<gray>6. <yellow>Стрит</yellow> (5 карт подряд)</gray>",
                        "<gray>7. <yellow>Сет / Тройка</yellow> (3 карты одного ранга)</gray>",
                        "<gray>8. <yellow>Две пары</yellow> | 9. <yellow>Пара</yellow> | 10. <gray>Старшая карта</gray>"
                )
        ) : List.of(
                List.of(
                        "<yellow><bold>Основы Дурака (подкидного)</bold></yellow>",
                        "<gray>Колода из 36 карт (от шестёрок до тузов).</gray>",
                        "<gray>В начале определяется козырная масть (козырь бьёт любую карту не-козыря).</gray>",
                        "<gray>Игрокам раздаётся по 6 карт. Атакующий ходит с карты в руке,</gray>",
                        "<gray>защищающийся обязан побить её старшей картой той же масти или козырем.</gray>"
                ),
                List.of(
                        "<yellow><bold>Подкидывание и завершение</bold></yellow>",
                        "<gray>Атакующий может подкидывать любые карты, достоинство которых</gray>",
                        "<gray>уже присутствует на столе.</gray>",
                        "<green>• Бито:</green> <gray>если защищающийся отбил все карты, кон уходит в сброс,</gray>",
                        "<gray>и защитник становится новым атакующим.</gray>",
                        "<red>• Взять:</red> <gray>если защитник не может отбиться, он забирает все карты со стола.</gray>",
                        "",
                        "<yellow>Побеждает игрок, который первым избавится от всех карт!</yellow>"
                )
        );

        new TutorialGUI(player, GameType.CARDS, pages, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                viewingTutorial.remove(player.getUniqueId());
                lastActionTime = System.currentTimeMillis();
                if (state == GameState.PLAYING) {
                    if (mode == CardsMode.DURAK) {
                        if (isP1 && durakGuiP1 != null) {
                            durakGuiP1.setSwitchingInventory(true);
                            durakGuiP1.open();
                        } else if (!isP1 && durakGuiP2 != null) {
                            durakGuiP2.setSwitchingInventory(true);
                            durakGuiP2.open();
                        }
                    } else if (mode == CardsMode.WAR) {
                        if (isP1 && warGuiP1 != null) {
                            warGuiP1.setSwitchingInventory(true);
                            warGuiP1.open();
                        } else if (!isP1 && warGuiP2 != null) {
                            warGuiP2.setSwitchingInventory(true);
                            warGuiP2.open();
                        }
                    } else {
                        if (isP1 && pokerGuiP1 != null) {
                            pokerGuiP1.setSwitchingInventory(true);
                            pokerGuiP1.open();
                        } else if (!isP1 && pokerGuiP2 != null) {
                            pokerGuiP2.setSwitchingInventory(true);
                            pokerGuiP2.open();
                        }
                    }
                }
            }, 1L);
        }).open();
    }

    private void syncViews() {
        if (durakGuiP1 != null && !viewingTutorial.contains(player1)) durakGuiP1.initializeItems();
        if (durakGuiP2 != null && !viewingTutorial.contains(player2)) durakGuiP2.initializeItems();
        if (warGuiP1 != null && !viewingTutorial.contains(player1)) warGuiP1.initializeItems();
        if (warGuiP2 != null && !viewingTutorial.contains(player2)) warGuiP2.initializeItems();
        if (pokerGuiP1 != null && !viewingTutorial.contains(player1)) pokerGuiP1.initializeItems();
        if (pokerGuiP2 != null && !viewingTutorial.contains(player2)) pokerGuiP2.initializeItems();
    }

    @Override
    public void onPlayerClick(Player player, int slot, ClickType clickType) {}

    private final long sessionStartTime = System.currentTimeMillis();

    public void reopenGui(Player player) {
        boolean isP1 = player.getUniqueId().equals(player1);
        if (mode == CardsMode.DURAK) {
            DurakGUI gui = isP1 ? durakGuiP1 : durakGuiP2;
            if (gui != null) { gui.setSwitchingInventory(true); gui.open(); }
        } else if (mode == CardsMode.WAR) {
            WarGUI gui = isP1 ? warGuiP1 : warGuiP2;
            if (gui != null) { gui.setSwitchingInventory(true); gui.open(); }
        } else {
            TexasHoldemGUI gui = isP1 ? pokerGuiP1 : pokerGuiP2;
            if (gui != null) { gui.setSwitchingInventory(true); gui.open(); }
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
        if (durakGuiP1 != null) {
            durakGuiP1.setSwitchingInventory(true);
            durakGuiP1.setClosed(true);
            Player p = durakGuiP1.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
        if (durakGuiP2 != null) {
            durakGuiP2.setSwitchingInventory(true);
            durakGuiP2.setClosed(true);
            Player p = durakGuiP2.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
        if (warGuiP1 != null) {
            warGuiP1.setSwitchingInventory(true);
            warGuiP1.setClosed(true);
            Player p = warGuiP1.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
        if (warGuiP2 != null) {
            warGuiP2.setSwitchingInventory(true);
            warGuiP2.setClosed(true);
            Player p = warGuiP2.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
        if (pokerGuiP1 != null) {
            pokerGuiP1.setSwitchingInventory(true);
            pokerGuiP1.setClosed(true);
            Player p = pokerGuiP1.getPlayer();
            if (p != null && p.isOnline()) p.closeInventory();
        }
        if (pokerGuiP2 != null) {
            pokerGuiP2.setSwitchingInventory(true);
            pokerGuiP2.setClosed(true);
            Player p = pokerGuiP2.getPlayer();
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
            UUID afkPlayer;
            if (mode == CardsMode.DURAK) {
                afkPlayer = p1Attacking ? player1 : player2;
            } else if (mode == CardsMode.POKER) {
                afkPlayer = pokerP1Turn ? player1 : player2;
            } else {
                afkPlayer = warCardP1 == null ? player1 : player2;
            }
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
