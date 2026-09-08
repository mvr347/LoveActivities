package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;

import java.util.List;

public class TutorialRegistry {

    public static List<List<String>> getTutorialPages(GameType gameType) {
        return switch (gameType) {
            case BLACKJACK -> getBlackjackPages();
            case DICE -> getDicePokerPages();
            case GWENT -> getGwentPages();
            case CARDS -> getDurakPages();
            case RPS -> getRpsPages();
            case CHESS -> getChessPages();
        };
    }

    public static List<List<String>> getTutorialPages(String key) {
        if (key == null) return getDurakPages();
        String lower = key.toLowerCase();

        return switch (lower) {
            case "poker", "texas", "texasholdem", "holdem" -> getPokerPages();
            case "durak", "cards_durak" -> getDurakPages();
            case "war", "cards_war", "drunkard" -> getWarPages();
            case "dice_poker", "yahtzee", "poker_dice" -> getDicePokerPages();
            case "dice_classic", "classic_dice", "2d6" -> getDiceClassicPages();
            case "dice" -> getDicePokerPages();
            case "blackjack", "21", "bj" -> getBlackjackPages();
            case "gwent" -> getGwentPages();
            case "rps", "knb" -> getRpsPages();
            case "chess", "minichess", "шахматы", "минишахматы" -> getChessPages();
            default -> {
                GameType gt = GameType.fromString(key);
                yield gt != null ? getTutorialPages(gt) : getDurakPages();
            }
        };
    }

    public static GameType getGameTypeByKey(String key) {
        if (key == null) return GameType.CARDS;
        String lower = key.toLowerCase();
        if (lower.contains("chess") || lower.contains("шахматы")) return GameType.CHESS;
        if (lower.contains("poker") || lower.contains("durak") || lower.contains("war") || lower.contains("card")) return GameType.CARDS;
        if (lower.contains("dice") || lower.contains("кости")) return GameType.DICE;
        if (lower.contains("black") || lower.contains("21") || lower.contains("блэк")) return GameType.BLACKJACK;
        if (lower.contains("gwent") || lower.contains("гвинт")) return GameType.GWENT;
        if (lower.contains("rps") || lower.contains("кнб")) return GameType.RPS;
        return GameType.CARDS;
    }

    public static List<List<String>> getChessPages() {
        return List.of(
                List.of(
                        "<yellow><bold>Мини-шахматы (6x7)</bold></yellow>",
                        "<gray>Динамичная версия шахмат на расширенной доске 6x7 клеток.</gray>",
                        "<gray>У каждого игрока: Король, Ферзь, 2 Слона, Конь, 2 Ладьи и 7 Пешек.</gray>",
                        "",
                        "<white>Цель игры:</white>",
                        "<green>• Захватить Короля противника или поставить ему Мат!</green>",
                        "<yellow>• Пешка, дошедшая до противоположного края, становится Ферзем!</yellow>"
                ),
                List.of(
                        "<yellow><bold>Как ходить фигурами</bold></yellow>",
                        "<gray>1. Нажмите на свою фигуру, чтобы выбрать её.</gray>",
                        "<gray>2. На доске подсветятся доступные ходы:</gray>",
                        "<green>• Зелёные плитки</green> <gray>— свободные клетки для перемещения.</gray>",
                        "<red>• Красные плитки</red> <gray>— вражеские фигуры для взятия.</gray>",
                        "<gray>3. Нажмите на подсвеченную клетку для совершения хода.</gray>"
                )
        );
    }

    public static List<List<String>> getBlackjackPages() {
        return List.of(
                List.of(
                        "<yellow><bold>Основы Блэкджека (21)</bold></yellow>",
                        "<gray>Цель игры — набрать сумму очков как можно ближе к 21,</gray>",
                        "<gray>но не превысить это число (иначе сразу перебор и проигрыш).</gray>",
                        "",
                        "<white>Номиналы карт:</white>",
                        "<gray>• Карты 2–10 дают соответствующее число очков (2–10).</gray>",
                        "<gray>• Валет, Дама, Король дают по 10 очков.</gray>",
                        "<gray>• Туз даёт 11 очков (или 1 очко, если с 11 получается перебор).</gray>"
                ),
                List.of(
                        "<yellow><bold>Действия игрока</bold></yellow>",
                        "<green>▶ Взять карту (+1)</green> <gray>— добрать ещё одну карту из колоды.</gray>",
                        "<yellow>▶ Хватит</yellow> <gray>— зафиксировать свои очки и передать ход сопернику.</gray>",
                        "<gold>▶ Удвоить ставку (x2)</gold> <gray>— удвоить ставку, взять ровно 1 карту</gray>",
                        "<gray>и автоматически завершить свой ход.</gray>",
                        "",
                        "<yellow>Побеждает игрок с наибольшим числом очков ≤ 21!</yellow>"
                )
        );
    }

    public static List<List<String>> getDiceClassicPages() {
        return List.of(
                List.of(
                        "<yellow><bold>Кидание костей (Классика 2D6)</bold></yellow>",
                        "<gray>Каждый игрок бросает по 2 кубика.</gray>",
                        "<gray>Суммируются выпавшие значения на гранях (от 2 до 12).</gray>",
                        "",
                        "<white>Правила победы:</white>",
                        "<green>• Побеждает игрок, у которого сумма очков больше!</green>",
                        "<yellow>• При равенстве суммы очков объявляется ничья и возврат ставок.</yellow>"
                )
        );
    }

    public static List<List<String>> getDicePokerPages() {
        return List.of(
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
    }

    public static List<List<String>> getPokerPages() {
        return List.of(
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
        );
    }

    public static List<List<String>> getDurakPages() {
        return List.of(
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
    }

    public static List<List<String>> getWarPages() {
        return List.of(
                List.of(
                        "<yellow><bold>Карточная игра: Пьяница (Война)</bold></yellow>",
                        "<gray>Колода делится поровну между двумя игроками.</gray>",
                        "<gray>Каждый раунд игроки одновременно открывают верхнюю карту.</gray>",
                        "<gray>Игрок с более старшей картой забирает обе карты в свой победный банк.</gray>",
                        "",
                        "<yellow>• При равных картах объявляется «Война» (ничья раунда).</yellow>",
                        "<green>• Побеждает игрок, набравший наибольшее количество карт!</green>"
                )
        );
    }

    public static List<List<String>> getGwentPages() {
        return List.of(
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
    }

    public static List<List<String>> getRpsPages() {
        return List.of(
                List.of(
                        "<yellow><bold>Камень, Ножницы, Бумага (КНБ)</bold></yellow>",
                        "<gray>Классическая игра на удачу и интуицию.</gray>",
                        "<gray>Матч длится до 2 побед в раундах (Best of 3).</gray>",
                        "",
                        "<white>Правила:</white>",
                        "<green>• Камень бьёт Ножницы</green>",
                        "<green>• Ножницы режут Бумагу</green>",
                        "<green>• Бумага накрывает Камень</green>",
                        "<yellow>• При одинаковом выборе — ничья в раунде.</yellow>"
                )
        );
    }
}
