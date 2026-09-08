package dev.lovelace.loveactivities.games.gwent;

import dev.lovelace.loveactivities.LoveActivities;
import org.bukkit.inventory.ItemStack;

public class GwentCard {

    public enum Row {
        MELEE("Ближний бой (Мечи)", "melee_row"),
        RANGED("Дальний бой (Луки)", "ranged_row"),
        SIEGE("Осадный ряд (Орудия)", "siege_row"),
        SPECIAL("Особая карта", "special_row");

        private final String nameRu;
        private final String textureKey;

        Row(String nameRu, String textureKey) {
            this.nameRu = nameRu;
            this.textureKey = textureKey;
        }

        public String getNameRu() {
            return nameRu;
        }

        public String getTextureKey() {
            return textureKey;
        }
    }

    public enum Ability {
        NONE("Нет", "Обычный отряд"),
        HERO("Герой", "Иммунитет к погоде, казни и усилениям"),
        SPY("Шпион", "Играется на поле врага, берёт 2 карты из колоды"),
        MEDIC("Медик", "Воскрешает случайный отряд из сброса"),
        SCORCH("Казнь", "Уничтожает сильнейшие карты на поле боя"),
        HORN("Маяк (Рог)", "Удваивает силу отрядов в ряду"),
        DECOY("Тотем (Чучело)", "Возвращает не-геройский отряд с поля в руку"),
        FROST("Снегопад (Мороз)", "Сила рукопашных не-героев = 1"),
        FOG("Грозовой туман", "Сила дальнобойных не-героев = 1"),
        RAIN("Кислотный ливень", "Сила осадных не-героев = 1"),
        CLEAR("Ясный день", "Снимает все погодные эффекты");

        private final String nameRu;
        private final String descRu;

        Ability(String nameRu, String descRu) {
            this.nameRu = nameRu;
            this.descRu = descRu;
        }

        public String getNameRu() {
            return nameRu;
        }

        public String getDescRu() {
            return descRu;
        }
    }

    private final String name;
    private final int baseStrength;
    private final Row row;
    private final Ability ability;
    private final String textureKey;

    public GwentCard(String name, int baseStrength, Row row, Ability ability, String textureKey) {
        this.name = name;
        this.baseStrength = baseStrength;
        this.row = row;
        this.ability = ability;
        this.textureKey = textureKey;
    }

    public String getName() {
        return name;
    }

    public int getBaseStrength() {
        return baseStrength;
    }

    public Row getRow() {
        return row;
    }

    public Ability getAbility() {
        return ability;
    }

    public String getTextureKey() {
        return textureKey;
    }

    public boolean isHero() {
        return ability == Ability.HERO;
    }

    public boolean isUnit() {
        return row != Row.SPECIAL;
    }
}
