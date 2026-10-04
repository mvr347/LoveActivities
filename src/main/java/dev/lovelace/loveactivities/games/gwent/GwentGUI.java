package dev.lovelace.loveactivities.games.gwent;

import dev.lovelace.loveactivities.gui.AbstractGUI;
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
        super(player, 54, "<gradient:#FF5E62:#FF9966>Гвинт (Minecraft Edition)</gradient> <dark_gray>[Р" + game.getCurrentRound() + "]</dark_gray>");
        this.game = game;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        // Header (Row 0) & Row 1 (Header 2nd row per gui-gen-5)
        for (int i = 0; i <= 17; i++) {
            inventory.setItem(i, glass);
        }

        // Row 4 separator (Slots 36-44)
        for (int i = 36; i <= 44; i++) {
            inventory.setItem(i, glass);
        }

        // Hand row (Row 5: 45-53)
        for (int i = 45; i <= 53; i++) {
            inventory.setItem(i, glass);
        }

        boolean isP1 = player.getUniqueId().equals(game.getPlayer1());
        Player opp = Bukkit.getPlayer(game.getOpponent(player.getUniqueId()));

        int myTotalPower = game.calculateTotalPower(isP1);
        int oppTotalPower = game.calculateTotalPower(!isP1);
        boolean myTurn = game.isPlayerTurn(player);

        // Slot 0: the game itself - score, strength and status of both sides, bank and the AFK timer
        int myRounds = isP1 ? game.getRoundsWonP1() : game.getRoundsWonP2();
        int oppRounds = isP1 ? game.getRoundsWonP2() : game.getRoundsWonP1();
        int myHandSize = (isP1 ? game.getHandP1() : game.getHandP2()).size();
        int oppHandSize = (isP1 ? game.getHandP2() : game.getHandP1()).size();
        boolean myPassed = isP1 ? game.isP1Passed() : game.isP2Passed();
        boolean oppPassed = isP1 ? game.isP2Passed() : game.isP1Passed();
        String oppName = opp != null ? opp.getName() : "Соперник";

        long elapsed = (System.currentTimeMillis() - game.getLastActionTime()) / 1000L;
        long remaining = Math.max(0L, plugin.getConfigManager().getAfkTurnTimeoutSeconds() - elapsed);
        String timerColor = remaining > 15 ? "<green>" : (remaining > 5 ? "<yellow>" : "<red>");

        List<String> gameLore = new ArrayList<>();
        gameLore.add("<gray>Раунд: <white>" + game.getCurrentRound() + "/3</white></gray>");
        gameLore.add("");
        gameLore.add("<green><bold>Вы</bold></green> <gray>— сила <white>" + myTotalPower + "</white>, раундов <gold>" + myRounds
                + "/2</gold>, карт <aqua>" + myHandSize + "</aqua>" + (myPassed ? " <red>(пас)</red>" : "") + "</gray>");
        gameLore.add("<red><bold>" + oppName + "</bold></red> <gray>— сила <white>" + oppTotalPower + "</white>, раундов <gold>" + oppRounds
                + "/2</gold>, карт <aqua>" + oppHandSize + "</aqua>" + (oppPassed ? " <red>(пас)</red>" : "") + "</gray>");
        gameLore.add("");
        gameLore.add(game.getBet() > 0
                ? "<gray>Банк: <gold>" + (game.getBet() * 2) + " " + plugin.getLoveCoreBridge().currencyName() + "</gold></gray>"
                : "<gray>Режим: <white>без ставки</white></gray>");
        gameLore.add("");
        gameLore.add(myPassed ? "<red>Вы спасовали</red>"
                : (myTurn ? "<green>Ваш ход</green> <dark_gray>•</dark_gray> " + timerColor + remaining + "с</gray>"
                        : "<yellow>Ход соперника...</yellow>"));

        setItem(0, plugin.getHeadManager().createBuilder("game_icons.gwent")
                .name("<gradient:#00C9FF:#92FE9D><bold>Гвинт</bold></gradient> <dark_gray>•</dark_gray> <green>" + myTotalPower
                        + "</green> <gray>:</gray> <red>" + oppTotalPower + "</red>")
                .lore(gameLore.toArray(new String[0]))
                .build());

        // Slot 4 (top centre): Active Weather
        String weatherName = "<green>Ясно</green>";
        String weatherKey = "gwent.weather_clear";
        if (game.isFrost()) { weatherName = "<aqua>Мороз (Ближний ряд = 1)</aqua>"; weatherKey = "gwent.weather_frost"; }
        else if (game.isFog()) { weatherName = "<gray>Туман (Дальний ряд = 1)</gray>"; weatherKey = "gwent.weather_fog"; }
        else if (game.isRain()) { weatherName = "<blue>Ливень (Осадный ряд = 1)</blue>"; weatherKey = "gwent.weather_rain"; }

        setItem(4, plugin.getHeadManager().createBuilder(weatherKey)
                .name("<yellow><bold>Погода на поле</bold></yellow>")
                .lore(
                        "<gray>Текущее состояние: " + weatherName + "</gray>",
                        "",
                        "<dark_gray>• Мороз: ближний бой = 1</dark_gray>",
                        "<dark_gray>• Туман: дальний бой = 1</dark_gray>",
                        "<dark_gray>• Ливень: осадный ряд = 1</dark_gray>"
                )
                .build());

        // Header Slot 6: Pass Button
        setItem(6, plugin.getHeadManager().createBuilder("gwent.pass_round")
                .name("<red><bold>ПАСОВАТЬ</bold></red>")
                .lore(
                        "<gray>Завершить участие в этом раунде.</gray>",
                        myTurn ? "<red>▶ Нажмите для паса</red>" : "<gray>Ожидание...</gray>"
                )
                .build(), click -> {
            if (myTurn) {
                game.actionPass(player);
            } else {
                SoundUtil.playError(player);
            }
        });

        // Header Slot 7: Tutorial Button
        setItem(7, plugin.getHeadManager().createBuilder("ui.tutorial")
                .name("<yellow><bold>Обучение Гвинту</bold></yellow>")
                .lore("<gray>Правила карт и рядов</gray>")
                .build(), click -> {
            game.openTutorial(player);
        });

        // Header Slot 8: Surrender Button
        setItem(8, plugin.getHeadManager().createBuilder("ui.surrender")
                .name("<dark_red><bold>Сдаться</bold></dark_red>")
                .lore("<gray>Признать поражение</gray>")
                .build(), click -> {
            game.resign(player);
        });

        // Battlefield cards lists
        List<GwentCard> oppMelee = isP1 ? game.getMeleeP2() : game.getMeleeP1();
        List<GwentCard> oppRanged = isP1 ? game.getRangedP2() : game.getRangedP1();
        List<GwentCard> oppSiege = isP1 ? game.getSiegeP2() : game.getSiegeP1();

        List<GwentCard> myMelee = isP1 ? game.getMeleeP1() : game.getMeleeP2();
        List<GwentCard> myRanged = isP1 ? game.getRangedP1() : game.getRangedP2();
        List<GwentCard> mySiege = isP1 ? game.getSiegeP1() : game.getSiegeP2();

        int oppMeleePower = game.calculateRowPower(oppMelee, !isP1);
        int oppRangedPower = game.calculateRowPower(oppRanged, !isP1);
        int oppSiegePower = game.calculateRowPower(oppSiege, !isP1);

        int myMeleePower = game.calculateRowPower(myMelee, isP1);
        int myRangedPower = game.calculateRowPower(myRanged, isP1);
        int mySiegePower = game.calculateRowPower(mySiege, isP1);

        // --- Row 1 (9-17): Opponent Row Indicators ---
        setItem(10, plugin.getHeadManager().createBuilder("gwent.melee_row")
                .name("<red><bold>⚔ Мечи врага</bold></red> <dark_gray>•</dark_gray> <yellow><bold>Сила: " + oppMeleePower + "</bold></yellow>")
                .lore(
                        "<gray>Ряд ближнего боя противника</gray>",
                        game.isFrost() ? "<aqua>❄ Активен Мороз (не-герои = 1)</aqua>" : "<gray>Погода: ясно</gray>",
                        game.hasHornMelee(!isP1) ? "<gold>🎺 Командирский рог (x2)</gold>" : "",
                        "<gray>Карт в ряду: <white>" + oppMelee.size() + "</white></gray>"
                ).build());

        setItem(13, plugin.getHeadManager().createBuilder("gwent.ranged_row")
                .name("<red><bold>🏹 Луки врага</bold></red> <dark_gray>•</dark_gray> <yellow><bold>Сила: " + oppRangedPower + "</bold></yellow>")
                .lore(
                        "<gray>Ряд дальнего боя противника</gray>",
                        game.isFog() ? "<gray>🌫 Активен Туман (не-герои = 1)</gray>" : "<gray>Погода: ясно</gray>",
                        game.hasHornRanged(!isP1) ? "<gold>🎺 Командирский рог (x2)</gold>" : "",
                        "<gray>Карт в ряду: <white>" + oppRanged.size() + "</white></gray>"
                ).build());

        setItem(16, plugin.getHeadManager().createBuilder("gwent.siege_row")
                .name("<red><bold>💣 Осада врага</bold></red> <dark_gray>•</dark_gray> <yellow><bold>Сила: " + oppSiegePower + "</bold></yellow>")
                .lore(
                        "<gray>Осадный ряд противника</gray>",
                        game.isRain() ? "<blue>🌧 Активен Ливень (не-герои = 1)</blue>" : "<gray>Погода: ясно</gray>",
                        game.hasHornSiege(!isP1) ? "<gold>🎺 Командирский рог (x2)</gold>" : "",
                        "<gray>Карт в ряду: <white>" + oppSiege.size() + "</white></gray>"
                ).build());

        // --- Row 2 (18-26): Opponent Battlefield (Melee 18-20, Ranged 21-23, Siege 24-26) ---
        renderRow(oppMelee, 18, 20, false, "Враг Мечи");
        renderRow(oppRanged, 21, 23, false, "Враг Луки");
        renderRow(oppSiege, 24, 26, false, "Враг Осада");

        // --- Row 3 (27-35): Player Battlefield (Melee 27-29, Ranged 30-32, Siege 33-35) ---
        renderRow(myMelee, 27, 29, true, "Ваши Мечи");
        renderRow(myRanged, 30, 32, true, "Ваши Луки");
        renderRow(mySiege, 33, 35, true, "Ваша Осада");

        // --- Row 4 (Slots 36-44): Player Row Indicators & Pagination ---
        List<GwentCard> myHand = isP1 ? game.getHandP1() : game.getHandP2();
        int maxPerPage = 8;
        int totalPages = Math.max(1, (int) Math.ceil((double) myHand.size() / maxPerPage));
        if (handPage >= totalPages) handPage = totalPages - 1;
        if (handPage < 0) handPage = 0;

        if (handPage > 0) {
            setItem(36, plugin.getHeadManager().createBuilder("ui.arrow_left")
                    .name("<yellow><bold>← Предыдущие карты (" + handPage + "/" + totalPages + ")</bold></yellow>")
                    .lore("<gray>Перейти к предыдущей странице руки.</gray>")
                    .build(), click -> {
                handPage--;
                SoundUtil.playClick(player);
                initializeItems();
            });
        } else {
            setItem(36, glass);
        }

        setItem(37, plugin.getHeadManager().createBuilder("gwent.melee_row")
                .name("<green><bold>⚔ Ваши Мечи</bold></green> <dark_gray>•</dark_gray> <yellow><bold>Сила: " + myMeleePower + "</bold></yellow>")
                .lore(
                        "<gray>Ваш ряд ближнего боя</gray>",
                        game.isFrost() ? "<aqua>❄ Активен Мороз (не-герои = 1)</aqua>" : "<gray>Погода: ясно</gray>",
                        game.hasHornMelee(isP1) ? "<gold>🎺 Командирский рог (x2)</gold>" : "",
                        "<gray>Карт в ряду: <white>" + myMelee.size() + "</white></gray>"
                ).build());

        setItem(40, plugin.getHeadManager().createBuilder("gwent.ranged_row")
                .name("<green><bold>🏹 Ваши Луки</bold></green> <dark_gray>•</dark_gray> <yellow><bold>Сила: " + myRangedPower + "</bold></yellow>")
                .lore(
                        "<gray>Ваш ряд дальнего боя</gray>",
                        game.isFog() ? "<gray>🌫 Активен Туман (не-герои = 1)</gray>" : "<gray>Погода: ясно</gray>",
                        game.hasHornRanged(isP1) ? "<gold>🎺 Командирский рог (x2)</gold>" : "",
                        "<gray>Карт в ряду: <white>" + myRanged.size() + "</white></gray>"
                ).build());

        setItem(43, plugin.getHeadManager().createBuilder("gwent.siege_row")
                .name("<green><bold>💣 Ваша Осада</bold></green> <dark_gray>•</dark_gray> <yellow><bold>Сила: " + mySiegePower + "</bold></yellow>")
                .lore(
                        "<gray>Ваш осадный ряд</gray>",
                        game.isRain() ? "<blue>🌧 Активен Ливень (не-герои = 1)</blue>" : "<gray>Погода: ясно</gray>",
                        game.hasHornSiege(isP1) ? "<gold>🎺 Командирский рог (x2)</gold>" : "",
                        "<gray>Карт в ряду: <white>" + mySiege.size() + "</white></gray>"
                ).build());

        if (handPage < totalPages - 1) {
            setItem(44, plugin.getHeadManager().createBuilder("ui.arrow_right")
                    .name("<yellow><bold>Следующие карты (" + (handPage + 2) + "/" + totalPages + ") →</bold></yellow>")
                    .lore("<gray>Перейти к следующей странице руки.</gray>")
                    .build(), click -> {
                handPage++;
                SoundUtil.playClick(player);
                initializeItems();
            });
        } else {
            setItem(44, glass);
        }

        // --- Row 5 (45-53): Hand Cards (45-52) & Glass (53) ---
        int startIndex = handPage * maxPerPage;
        for (int i = 0; i < maxPerPage; i++) {
            int cardIdx = startIndex + i;
            int slot = 45 + i;
            if (cardIdx < myHand.size()) {
                GwentCard card = myHand.get(cardIdx);
                String tex = plugin.getHeadManager().getTexture(card.getTextureKey());
                if (tex.isEmpty()) tex = plugin.getHeadManager().getTexture("gwent.card_hero");

                String powerTag = card.isUnit() ? (card.isHero() ? " <gold>[" + card.getBaseStrength() + " ★]</gold>" : " <yellow>[" + card.getBaseStrength() + "]</yellow>") : " <aqua>[Особая]</aqua>";

                ItemBuilder cardBuilder = ItemBuilder.base64Head(tex)
                        .name((card.isHero() ? "<gold><bold>" : "<yellow><bold>") + card.getName() + (card.isHero() ? "</bold></gold>" : "</bold></yellow>") + powerTag)
                        .lore(
                                "<gray>Ряд: <white>" + card.getRow().getNameRu() + "</white></gray>",
                                card.getAbility() != GwentCard.Ability.NONE ? "<gold>Способность: " + card.getAbility().getDescRu() + "</gold>" : "<gray>Обычный отряд</gray>",
                                "",
                                myTurn ? "<green>▶ Нажмите, чтобы разыграть карту</green>" : "<red>Сейчас не ваш ход</red>"
                        );

                if (card.isHero()) {
                    cardBuilder.glow(true);
                }

                setItem(slot, cardBuilder.build(), click -> {
                    if (myTurn) {
                        game.actionPlayCard(player, cardIdx);
                    } else {
                        SoundUtil.playError(player);
                    }
                });
            } else {
                setItem(slot, glass);
            }
        }

        // Slot 53: Glass (Surrender is at Header slot 8, no duplicate!)
        setItem(53, glass);
    }

    private void renderRow(List<GwentCard> cards, int startSlot, int endSlot, boolean isMyRow, String label) {
        if (cards == null) return;
        int maxSlots = endSlot - startSlot + 1;
        for (int i = 0; i < maxSlots; i++) {
            int slot = startSlot + i;
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
