package dev.lovelace.loveactivities.manager;

import dev.lovelace.loveactivities.api.GameType;

import java.util.*;

public class NpcActivityConfig {

    private final UUID entityUuid;
    private String customName;
    private GameType gameType;
    private long defaultBet;
    private long maxBet;
    private boolean playsBets;
    private long refusedUntil;
    private int refusalCooldownMinutes;
    private boolean allowFreePlay;
    private double acceptChance;
    private final Map<String, List<String>> dialogues = new HashMap<>();

    public NpcActivityConfig(UUID entityUuid, String customName, GameType gameType, long defaultBet) {
        this(entityUuid, customName, gameType, defaultBet, 0L, true);
    }

    public NpcActivityConfig(UUID entityUuid, String customName, GameType gameType, long defaultBet, long maxBet, boolean playsBets) {
        this.entityUuid = entityUuid;
        this.customName = dev.lovelace.loveactivities.util.NpcNames.normalize(customName);
        this.gameType = gameType != null ? gameType : GameType.BLACKJACK;
        this.defaultBet = Math.max(0L, defaultBet);
        this.maxBet = Math.max(0L, maxBet);
        this.playsBets = playsBets;
        this.refusedUntil = 0L;
        this.refusalCooldownMinutes = 5;
        this.allowFreePlay = true;
        this.acceptChance = 0.85;
        initDefaultDialogues();
    }

    private void initDefaultDialogues() {
        dialogues.put("greetings", List.of(
                "Приветствую, путник! Сыграем партию?",
                "Ищешь достойного соперника? Давай проверим твою удачу!",
                "Ха, надеюсь, у тебя припасена пара лишних монет!",
                "Давно никто не бросал мне вызов. Я в деле!",
                "Садись за стол, посмотрим, из какого теста ты сделан.",
                "О, свежая кровь! Доставай монеты, правила просты.",
                "Азарт в крови? Прекрасно, не заставляй меня ждать!",
                "Хороший день для хорошей игры. Начнём!"
        ));

        dialogues.put("refuse", List.of(
                "Не сейчас, друг. У меня полно других забот.",
                "С тобой играть? Сначала заслужи репутацию в этих краях!",
                "Я сейчас не в настроении бросать кости. Приходи позже.",
                "Твои карманы слишком пусты для игры со мной.",
                "Сегодня не мой день для азартных игр. Зайди в другой раз.",
                "У меня важное дело, не до развлечений.",
                "Твои ставки меня не впечатляют, найди кого-нибудь попроще.",
                "Отойди, путник, я жду другого человека."
        ));

        dialogues.put("ingame", List.of(
                "Хм... Интересный ход, но я предвидел это!",
                "Посмотрим, как ты справишься со следующим раундом!",
                "Кажется, сегодня удача благоволит мне!",
                "Неплохо держишься, но победа будет за мной.",
                "Главное в игре — сохранять хладнокровие.",
                "Ты играешь смело, уважаю. Но рисковать опасно!",
                "Колода и кости не прощают ошибок, будь начеку.",
                "Чувствуешь напряжение? Это и есть настоящий азарт!"
        ));

        dialogues.put("win", List.of(
                "Ха-ха! Легкая победа. Приходи, когда потренируешься!",
                "Опыт всегда берет верх над азартом. Спасибо за игру!",
                "Не расстраивайся, сегодня просто не твой день!",
                "Монеты остаются у мастера. Возвращайся с реваншем!",
                "Красивая партия, но победителем ухожу я.",
                "Удача любит подготовленных! До встречи за столом.",
                "Не вешай нос, в следующий раз повезёт больше!"
        ));

        dialogues.put("loss", List.of(
                "Чёрт... Ты действительно мастер своего дела. Забирай выигрыш!",
                "Невероятно! Ты обыграл меня... В следующий раз я отыграюсь!",
                "Признаю своё поражение. Достойная игра!",
                "Удача была на твоей стороне. Заслуженная победа!",
                "Эх, просчитался... Забирай свой куш, ты заслужил.",
                "Сильный ход, я не ожидал такого поворота. Снимаю шляпу!",
                "Мои поздравления. Но в следующий раз победа будет за мной!"
        ));

        dialogues.put("draw", List.of(
                "Ничья! Мы стоим друг друга, отличная партия.",
                "Силы равны. Ни победителей, ни побежденных.",
                "Разошлись миром. Сыграем ещё разок?",
                "Абсолютное равенство. Редкий и достойный исход!",
                "Ничья! Кажется, нам обоим стоит повторить эту дуэль."
        ));
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = dev.lovelace.loveactivities.util.NpcNames.normalize(customName);
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public long getDefaultBet() {
        return defaultBet;
    }

    public void setDefaultBet(long defaultBet) {
        this.defaultBet = defaultBet;
    }

    public long getMaxBet() {
        return maxBet;
    }

    public void setMaxBet(long maxBet) {
        this.maxBet = Math.max(0L, maxBet);
    }

    public boolean isPlaysBets() {
        return playsBets;
    }

    public void setPlaysBets(boolean playsBets) {
        this.playsBets = playsBets;
    }

    public long getRefusedUntil() {
        return refusedUntil;
    }

    public void setRefusedUntil(long refusedUntil) {
        this.refusedUntil = refusedUntil;
    }

    public boolean isRefusing() {
        return System.currentTimeMillis() < refusedUntil;
    }

    public int getRefusalCooldownMinutes() {
        return refusalCooldownMinutes;
    }

    public void setRefusalCooldownMinutes(int refusalCooldownMinutes) {
        this.refusalCooldownMinutes = Math.max(1, refusalCooldownMinutes);
    }

    public boolean isAllowFreePlay() {
        return allowFreePlay;
    }

    public void setAllowFreePlay(boolean allowFreePlay) {
        this.allowFreePlay = allowFreePlay;
    }

    public double getAcceptChance() {
        return acceptChance;
    }

    public void setAcceptChance(double acceptChance) {
        this.acceptChance = acceptChance;
    }

    public Map<String, List<String>> getDialogues() {
        return dialogues;
    }

    public String getRandomDialogue(String category) {
        List<String> list = dialogues.get(category);
        if (list != null && !list.isEmpty()) {
            return list.get(new Random().nextInt(list.size()));
        }
        return dev.lovelace.loveactivities.LoveActivities.getInstance().getNpcManager().getDefaultDialogue(category);
    }
}
