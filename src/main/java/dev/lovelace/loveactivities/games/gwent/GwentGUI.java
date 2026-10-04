package dev.lovelace.loveactivities.games.gwent;

import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.util.Hints;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GwentGUI extends AbstractGUI {

    private final GwentGame game;
    private int handPage = 0;

    public GwentGUI(Player player, GwentGame game) {
        super(player, 54, "<gradient:#FF5E62:#FF9966>Гвинт (Minecraft Edition)</gradient>");
        this.game = game;
    }

    /** Header control buttons (gui_gen v2.1): opponent, weather, pass, tutorial, surrender in slots 2-7, centred. */
    private static final int[] CONTROL_SLOTS = {2, 3, 4, 6, 7};
    /** Board clusters inside the 7 content columns of a work row: melee 2, ranged 3, siege 2 (side walls stay empty). */
    private static final int[] MELEE_COLS = {1, 2};
    private static final int[] RANGED_COLS = {3, 4, 5};
    private static final int[] SIEGE_COLS = {6, 7};
    private static final int OPP_ROW = 18;
    private static final int MY_ROW = 27;
    private static final int HAND_ROW = 36;
    private static final int HAND_PER_PAGE = 7;

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        // Header: rows 0-1 are glass (Row1 never holds anything), the footer is one glass row.
        for (int i = 0; i <= 17; i++) inventory.setItem(i, glass);
        for (int i = 45; i <= 53; i++) inventory.setItem(i, glass);

        boolean isP1 = player.getUniqueId().equals(game.getPlayer1());
        Player opp = Bukkit.getPlayer(game.getOpponent(player.getUniqueId()));
        String oppName = opp != null ? opp.getName() : "Соперник";

        List<GwentCard> oppMelee = isP1 ? game.getMeleeP2() : game.getMeleeP1();
        List<GwentCard> oppRanged = isP1 ? game.getRangedP2() : game.getRangedP1();
        List<GwentCard> oppSiege = isP1 ? game.getSiegeP2() : game.getSiegeP1();
        List<GwentCard> myMelee = isP1 ? game.getMeleeP1() : game.getMeleeP2();
        List<GwentCard> myRanged = isP1 ? game.getRangedP1() : game.getRangedP2();
        List<GwentCard> mySiege = isP1 ? game.getSiegeP1() : game.getSiegeP2();

        int myTotalPower = game.calculateTotalPower(isP1);
        int oppTotalPower = game.calculateTotalPower(!isP1);
        boolean myTurn = game.isPlayerTurn(player);
        boolean myPassed = isP1 ? game.isP1Passed() : game.isP2Passed();
        boolean oppPassed = isP1 ? game.isP2Passed() : game.isP1Passed();
        int myRounds = isP1 ? game.getRoundsWonP1() : game.getRoundsWonP2();
        int oppRounds = isP1 ? game.getRoundsWonP2() : game.getRoundsWonP1();
        int myHandSize = (isP1 ? game.getHandP1() : game.getHandP2()).size();
        int oppHandSize = (isP1 ? game.getHandP2() : game.getHandP1()).size();

        // The round and the AFK countdown live in the heads: no separate round button.
        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);
        String roundLine = "<dark_gray>▪</dark_gray> <gray>Раунд: <white>" + game.getCurrentRound() + "/3</white></gray>";
        String bankLine = game.getBet() > 0
                ? "<dark_gray>▪</dark_gray> <gray>Банк: <gold>" + (game.getBet() * 2) + " " + plugin.getLoveCoreBridge().currencyName() + "</gold></gray>"
                : "<dark_gray>▪</dark_gray> <gray>Режим: <white>без ставки</white></gray>";

        // Slot 0: you (always first)
        String turnLine = myPassed ? "<red>Вы спасовали</red>" : (myTurn ? "<green>Ваш ход</green> <dark_gray>(" + remaining + "с)</dark_gray>" : "<yellow>Ход соперника</yellow>");
        setItem(0, ItemBuilder.skull().playerHead(player.getUniqueId())
                .name("<green><bold>Вы</bold></green> <dark_gray>•</dark_gray> <white>" + myTotalPower + "</white>")
                .lore(
                        roundLine,
                        "<dark_gray>▪</dark_gray> <gray>Раундов выиграно: <gold>" + myRounds + "/2</gold></gray>",
                        "<dark_gray>▪</dark_gray> <gray>Карт в руке: <aqua>" + myHandSize + "</aqua></gray>",
                        "<dark_gray>▪</dark_gray> <gray>Ближний <white>" + game.calculateRowPower(myMelee, isP1)
                                + "</white> · Дальний <white>" + game.calculateRowPower(myRanged, isP1)
                                + "</white> · Осада <white>" + game.calculateRowPower(mySiege, isP1) + "</white></gray>",
                        bankLine,
                        "",
                        turnLine
                )
                .build());

        // Controls, in the order: opponent, weather, pass, tutorial, surrender
        setItem(CONTROL_SLOTS[0], ItemBuilder.skull().playerHead(opp != null ? opp.getUniqueId() : null)
                .name("<red><bold>" + oppName + "</bold></red> <dark_gray>•</dark_gray> <white>" + oppTotalPower + "</white>")
                .lore(
                        roundLine,
                        "<dark_gray>▪</dark_gray> <gray>Раундов выиграно: <gold>" + oppRounds + "/2</gold></gray>",
                        "<dark_gray>▪</dark_gray> <gray>Карт в руке: <aqua>" + oppHandSize + "</aqua></gray>",
                        "<dark_gray>▪</dark_gray> <gray>Ближний <white>" + game.calculateRowPower(oppMelee, !isP1)
                                + "</white> · Дальний <white>" + game.calculateRowPower(oppRanged, !isP1)
                                + "</white> · Осада <white>" + game.calculateRowPower(oppSiege, !isP1) + "</white></gray>",
                        "",
                        oppPassed ? "<red>Спасовал</red>" : (myTurn ? "<gray>В игре</gray>" : "<yellow>Думает...</yellow>")
                )
                .build());

        String weatherName = "<green>Ясно</green>";
        String weatherKey = "gwent.weather_clear";
        if (game.isFrost()) { weatherName = "<aqua>Мороз — ближний ряд = 1</aqua>"; weatherKey = "gwent.weather_frost"; }
        else if (game.isFog()) { weatherName = "<gray>Туман — дальний ряд = 1</gray>"; weatherKey = "gwent.weather_fog"; }
        else if (game.isRain()) { weatherName = "<blue>Ливень — осадный ряд = 1</blue>"; weatherKey = "gwent.weather_rain"; }
        setItem(CONTROL_SLOTS[1], plugin.getHeadManager().createBuilder(weatherKey)
                .name("<yellow><bold>Погода</bold></yellow>")
                .lore(
                        "<dark_gray>▪</dark_gray> <gray>Сейчас: " + weatherName + "</gray>",
                        "",
                        "<gray>Мороз — ближний, Туман — дальний, Ливень — осадный.</gray>",
                        "<gray>Героев погода не затрагивает.</gray>"
                )
                .build());

        setItem(CONTROL_SLOTS[2], plugin.getHeadManager().createBuilder("gwent.pass_round")
                .name("<red><bold>Пас</bold></red>")
                .lore(
                        "<gray>Завершить участие в этом раунде.</gray>",
                        "",
                        myTurn ? Hints.act("ЛКМ", "пасовать") : "<gray>Ждите своего хода...</gray>"
                )
                .build(), click -> {
            if (myTurn) {
                game.actionPass(player);
            } else {
                SoundUtil.playError(player);
            }
        });

        setItem(CONTROL_SLOTS[3], plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<yellow><bold>Обучение</bold></yellow>")
                .lore("<gray>Правила карт и рядов.</gray>", "", Hints.act("ЛКМ", "открыть"))
                .build(), click -> game.openTutorial(player));

        setItem(CONTROL_SLOTS[4], plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<dark_red><bold>Сдаться</bold></dark_red>")
                .lore("<gray>Признать поражение.</gray>", "", Hints.act("ЛКМ", "сдаться"))
                .build(), click -> game.resign(player));

        // Board: opponent on 18-26, you on 27-35 (side walls stay empty)
        renderRow(oppMelee, rowSlots(OPP_ROW, MELEE_COLS), false, "Враг: ближний бой");
        renderRow(oppRanged, rowSlots(OPP_ROW, RANGED_COLS), false, "Враг: дальний бой");
        renderRow(oppSiege, rowSlots(OPP_ROW, SIEGE_COLS), false, "Враг: осада");
        renderRow(myMelee, rowSlots(MY_ROW, MELEE_COLS), true, "Ваш ближний бой");
        renderRow(myRanged, rowSlots(MY_ROW, RANGED_COLS), true, "Ваш дальний бой");
        renderRow(mySiege, rowSlots(MY_ROW, SIEGE_COLS), true, "Ваша осада");

        // Hand on 37-43, page arrows on the walls 36 and 44 (gui_gen rule 6)
        List<GwentCard> myHand = isP1 ? game.getHandP1() : game.getHandP2();
        int totalPages = Math.max(1, (int) Math.ceil((double) myHand.size() / HAND_PER_PAGE));
        if (handPage >= totalPages) handPage = totalPages - 1;
        if (handPage < 0) handPage = 0;

        if (handPage > 0) {
            setItem(HAND_ROW, plugin.getHeadManager().createBuilder("ui.arrow_left")
                    .name("<yellow><bold>← Назад (" + handPage + "/" + totalPages + ")</bold></yellow>")
                    .lore("<gray>Предыдущая страница руки.</gray>")
                    .build(), click -> {
                handPage--;
                SoundUtil.playClick(player);
                initializeItems();
            });
        }
        if (handPage < totalPages - 1) {
            setItem(HAND_ROW + 8, plugin.getHeadManager().createBuilder("ui.arrow_right")
                    .name("<yellow><bold>Далее (" + (handPage + 2) + "/" + totalPages + ") →</bold></yellow>")
                    .lore("<gray>Следующая страница руки.</gray>")
                    .build(), click -> {
                handPage++;
                SoundUtil.playClick(player);
                initializeItems();
            });
        }

        int startIndex = handPage * HAND_PER_PAGE;
        for (int i = 0; i < HAND_PER_PAGE; i++) {
            int cardIdx = startIndex + i;
            int slot = HAND_ROW + 1 + i;
            if (cardIdx >= myHand.size()) continue;
            GwentCard card = myHand.get(cardIdx);
            String tex = plugin.getHeadManager().getTexture(card.getTextureKey());
            if (tex.isEmpty()) tex = plugin.getHeadManager().getTexture("gwent.card_hero");

            String powerTag = card.isUnit() ? (card.isHero() ? " <gold>[" + card.getBaseStrength() + " ★]</gold>" : " <yellow>[" + card.getBaseStrength() + "]</yellow>") : " <aqua>[Особая]</aqua>";

            ItemBuilder cardBuilder = ItemBuilder.base64Head(tex)
                    .name((card.isHero() ? "<gold><bold>" : "<yellow><bold>") + card.getName() + (card.isHero() ? "</bold></gold>" : "</bold></yellow>") + powerTag)
                    .lore(
                            "<dark_gray>▪</dark_gray> <gray>Ряд: <white>" + card.getRow().getNameRu() + "</white></gray>",
                            card.getAbility() != GwentCard.Ability.NONE ? "<dark_gray>▪</dark_gray> <gold>" + card.getAbility().getDescRu() + "</gold>" : "<dark_gray>▪</dark_gray> <gray>Обычный отряд</gray>",
                            "",
                            myTurn ? Hints.act("ЛКМ", "разыграть") : "<red>Сейчас не ваш ход</red>"
                    );
            if (card.isHero()) cardBuilder.glow(true);

            setItem(slot, cardBuilder.build(), click -> {
                if (myTurn) {
                    game.actionPlayCard(player, cardIdx);
                } else {
                    SoundUtil.playError(player);
                }
            });
        }
    }

    private static int[] rowSlots(int rowStart, int[] cols) {
        int[] slots = new int[cols.length];
        for (int i = 0; i < cols.length; i++) slots[i] = rowStart + cols[i];
        return slots;
    }

    private void renderRow(List<GwentCard> cards, int[] slots, boolean isMyRow, String label) {
        if (cards == null) return;
        int maxSlots = slots.length;
        for (int i = 0; i < maxSlots; i++) {
            int slot = slots[i];
            if (i < cards.size()) {
                GwentCard c = cards.get(i);
                String tex = plugin.getHeadManager().getTexture(c.getTextureKey());
                if (tex.isEmpty()) tex = plugin.getHeadManager().getTexture("gwent.card_hero");

                int calcPower = game.getCalculatedCardPower(c, isMyRow);
                int basePower = c.getBaseStrength();
                String powerDisplay;
                if (c.isHero()) {
                    powerDisplay = "<gold>[" + calcPower + " ★]</gold>";
                } else if (calcPower > basePower) {
                    powerDisplay = "<green>[" + calcPower + " ↑]</green>";
                } else if (calcPower < basePower) {
                    powerDisplay = "<red>[" + calcPower + " ↓]</red>";
                } else {
                    powerDisplay = "<yellow>[" + calcPower + "]</yellow>";
                }

                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Базовая сила: <yellow>" + basePower + "</yellow></gray>"));
                lore.add(TextUtil.parse("<gray>Текущая сила: " + powerDisplay + "</gray>"));
                lore.add(TextUtil.parse("<gray>Ряд: <white>" + label + "</white></gray>"));
                if (c.getAbility() != GwentCard.Ability.NONE) {
                    lore.add(TextUtil.parse("<gold>Способность: " + c.getAbility().getDescRu() + "</gold>"));
                }
                if (i == maxSlots - 1 && cards.size() > maxSlots) {
                    lore.add(Component.empty());
                    lore.add(TextUtil.parse("<yellow>+ ещё " + (cards.size() - maxSlots) + " карт в ряду (всего " + cards.size() + ")</yellow>"));
                }

                ItemBuilder b = ItemBuilder.base64Head(tex)
                        .name((c.isHero() ? "<gold><bold>" : "<yellow>") + c.getName() + (c.isHero() ? "</bold></gold> " : "</yellow> ") + powerDisplay)
                        .lore(lore);

                if (c.isHero() || calcPower > basePower) {
                    b.glow(true);
                }

                setItem(slot, b.build());
            } else {
                setItem(slot, ItemBuilder.from(Material.AIR).build());
            }
        }
    }

    @Override
    public void handleClose() {
        if (!isSwitchingInventory()) {
            game.onPlayerClose(player);
        }
    }
}
