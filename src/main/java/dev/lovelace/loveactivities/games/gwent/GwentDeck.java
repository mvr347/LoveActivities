package dev.lovelace.loveactivities.games.gwent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GwentDeck {

    public static List<GwentCard> createStandardDeck() {
        List<GwentCard> deck = new ArrayList<>();

        // Heroes (Golden units)
        deck.add(new GwentCard("Стив", 15, GwentCard.Row.MELEE, GwentCard.Ability.HERO, "gwent.card_steve"));
        deck.add(new GwentCard("Дракон Края", 15, GwentCard.Row.RANGED, GwentCard.Ability.HERO, "gwent.card_dragon"));
        deck.add(new GwentCard("Варден", 12, GwentCard.Row.MELEE, GwentCard.Ability.HERO, "gwent.card_warden"));

        // Spies
        deck.add(new GwentCard("Эндермен", 4, GwentCard.Row.MELEE, GwentCard.Ability.SPY, "gwent.card_enderman"));
        deck.add(new GwentCard("Житель-торговец", 0, GwentCard.Row.MELEE, GwentCard.Ability.SPY, "gwent.card_villager"));

        // Medics
        deck.add(new GwentCard("Железный голем", 7, GwentCard.Row.MELEE, GwentCard.Ability.MEDIC, "gwent.card_iron_golem"));
        deck.add(new GwentCard("Ведьма", 5, GwentCard.Row.RANGED, GwentCard.Ability.MEDIC, "gwent.card_witch"));

        // Melee Units
        deck.add(new GwentCard("Незеритовый рыцарь", 8, GwentCard.Row.MELEE, GwentCard.Ability.NONE, "gwent.card_netherite"));
        deck.add(new GwentCard("Зомби", 4, GwentCard.Row.MELEE, GwentCard.Ability.NONE, "gwent.card_zombie"));
        deck.add(new GwentCard("Зомби", 4, GwentCard.Row.MELEE, GwentCard.Ability.NONE, "gwent.card_zombie"));
        deck.add(new GwentCard("Крипер", 6, GwentCard.Row.MELEE, GwentCard.Ability.SCORCH, "gwent.card_creeper"));

        // Ranged Units
        deck.add(new GwentCard("Скелет", 6, GwentCard.Row.RANGED, GwentCard.Ability.NONE, "gwent.card_skeleton"));
        deck.add(new GwentCard("Разбойник", 5, GwentCard.Row.RANGED, GwentCard.Ability.NONE, "gwent.card_pillager"));
        deck.add(new GwentCard("Разбойник", 5, GwentCard.Row.RANGED, GwentCard.Ability.NONE, "gwent.card_pillager"));

        // Siege Units
        deck.add(new GwentCard("Гаст", 8, GwentCard.Row.SIEGE, GwentCard.Ability.NONE, "gwent.card_ghast"));
        deck.add(new GwentCard("Иссушитель", 10, GwentCard.Row.SIEGE, GwentCard.Ability.NONE, "gwent.card_wither"));
        deck.add(new GwentCard("Ифрит", 6, GwentCard.Row.SIEGE, GwentCard.Ability.NONE, "gwent.card_blaze"));

        // Specials & Weather
        deck.add(new GwentCard("Маяк", 0, GwentCard.Row.SPECIAL, GwentCard.Ability.HORN, "gwent.card_beacon"));
        deck.add(new GwentCard("ТНТ", 0, GwentCard.Row.SPECIAL, GwentCard.Ability.SCORCH, "gwent.card_tnt"));
        deck.add(new GwentCard("Тотем", 0, GwentCard.Row.SPECIAL, GwentCard.Ability.DECOY, "gwent.card_totem"));
        deck.add(new GwentCard("Снегопад (Мороз)", 0, GwentCard.Row.SPECIAL, GwentCard.Ability.FROST, "gwent.weather_frost"));
        deck.add(new GwentCard("Грозовой туман", 0, GwentCard.Row.SPECIAL, GwentCard.Ability.FOG, "gwent.weather_fog"));
        deck.add(new GwentCard("Кислотный ливень", 0, GwentCard.Row.SPECIAL, GwentCard.Ability.RAIN, "gwent.weather_rain"));
        deck.add(new GwentCard("Ясный день", 0, GwentCard.Row.SPECIAL, GwentCard.Ability.CLEAR, "gwent.weather_clear"));

        Collections.shuffle(deck);
        return deck;
    }
}
