package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.manager.NpcActivityConfig;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcConfirmationGUI extends AbstractGUI {

    private final NpcActivityConfig npcConfig;
    private long currentBet;

    public NpcConfirmationGUI(Player player, NpcActivityConfig npcConfig) {
        super(player, 27, "<white>Игра с </white>" + npcConfig.getCustomName());
        this.npcConfig = npcConfig;
        if (!npcConfig.isPlaysBets()) {
            this.currentBet = 0L;
        } else {
            long b = npcConfig.getDefaultBet();
            if (npcConfig.getMaxBet() > 0 && b > npcConfig.getMaxBet()) {
                b = npcConfig.getMaxBet();
            }
            this.currentBet = b;
        }
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack border = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        // Slot 4: Game & NPC Info
        String iconKey = npcConfig.getGameType() != null ? npcConfig.getGameType().getIconKey() : "chess";
        String gameName = npcConfig.getGameType() != null ? npcConfig.getGameType().getNameRu() : "Мини-игра";
        long playerBalance = plugin.getLoveCoreBridge().getBalance(player);

        List<String> infoLore = new ArrayList<>();
        infoLore.add("<gray>Соперник: </gray>" + npcConfig.getCustomName());
        if (npcConfig.isPlaysBets()) {
            infoLore.add("<gray>Текущая ставка: <gold>" + (currentBet > 0 ? CurrencyUtil.formatCoinsWords(currentBet) : "Без ставки") + "</gold></gray>");
            infoLore.add("<gray>Лимит ставки NPC: <gold>" + (npcConfig.getMaxBet() > 0 ? CurrencyUtil.formatCoinsWords(npcConfig.getMaxBet()) : "Без ограничений") + "</gold></gray>");
            infoLore.add("<gray>Ваш баланс: <yellow>" + playerBalance + " " + plugin.getLoveCoreBridge().currencyName() + "</yellow></gray>");
            infoLore.add("<gray>Формат: <white>Матч против NPC</white></gray>");
            infoLore.add("");
            infoLore.add("<dark_gray>Используйте кнопки снизу для изменения ставки</dark_gray>");
        } else {
            infoLore.add("<gray>Ставки: <yellow>Выключены (игра без денег)</yellow></gray>");
            infoLore.add("<gray>Формат: <white>Дружеская игра против NPC</white></gray>");
        }

        setItem(4, plugin.getHeadManager().createBuilder("game_icons." + iconKey)
                .name("<yellow>Партия: " + gameName + "</yellow>")
                .lore(infoLore.toArray(new String[0]))
                .build());

        // Slot 11: Accept & Start Button
        List<String> startLore = new ArrayList<>();
        startLore.add("<gray>Подтвердить условия и начать игру.</gray>");
        if (currentBet > 0) {
            startLore.add("<gray>Ставка: <gold>" + CurrencyUtil.formatCoinsWords(currentBet) + "</gold></gray>");
            startLore.add("<gray>Ставка будет удержана в банк матча.</gray>");
        } else {
            startLore.add("<gray>Дружеская игра без ставки.</gray>");
        }
        startLore.add("");
        startLore.add("<green>▶ Нажмите для старта</green>");

        setItem(11, plugin.getHeadManager().createBuilder("ui.confirm_ready")
                .name("<green>Начать партию</green>")
                .lore(startLore.toArray(new String[0]))
                .build(), click -> {
            if (plugin.getSessionManager().isInGame(player.getUniqueId())) {
                player.closeInventory();
                SoundUtil.playError(player);
                plugin.getNpcManager().speak(player, npcConfig.getCustomName(), "Ты уже участвуешь в игре!");
                return;
            }

            if (currentBet > 0 && !plugin.getLoveCoreBridge().hasBalance(player, currentBet)) {
                SoundUtil.playError(player);
                plugin.getLocaleManager().send(player, "insufficient_funds", Map.of(
                        "cost", String.valueOf(currentBet),
                        "currency", plugin.getLoveCoreBridge().currencyName()
                ));
                return;
            }

            setSwitchingInventory(true);
            SoundUtil.playClick(player);
            // Cleanly close inventory and start session on next tick to avoid window packet races
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                plugin.getSessionManager().startNpcSession(player, npcConfig.getGameType(), currentBet, npcConfig.getCustomName(), npcConfig);
            }, 2L);
        });

        // Slot 15: Decline Button
        setItem(15, plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red>Отказаться</red>")
                .lore(
                        "<gray>Закрыть меню и отказаться от игры</gray>",
                        "",
                        "<red>▶ Нажмите для отказа</red>"
                )
                .build(), click -> {
            SoundUtil.playClick(player);
            player.closeInventory();
        });

        // Bet adjustment buttons (only if NPC plays with bets)
        if (npcConfig.isPlaysBets()) {
            long maxAllowed = (npcConfig.getMaxBet() > 0) ? Math.min(npcConfig.getMaxBet(), playerBalance) : playerBalance;

            // Slot 19: -50
            setItem(19, plugin.getHeadManager().createBuilder("ui.minus")
                    .name("<red>-50 монет</red>")
                    .lore("<gray>Уменьшить ставку на 50</gray>")
                    .build(), click -> adjustBet(-50, maxAllowed));

            // Slot 20: -10
            setItem(20, plugin.getHeadManager().createBuilder("ui.minus")
                    .name("<red>-10 монет</red>")
                    .lore("<gray>Уменьшить ставку на 10</gray>")
                    .build(), click -> adjustBet(-10, maxAllowed));

            // Slot 21: Min (0)
            setItem(21, plugin.getHeadManager().createBuilder("ui.status_waiting")
                    .name("<yellow>Сбросить в 0</yellow>")
                    .lore("<gray>Играть без ставки</gray>")
                    .build(), click -> {
                currentBet = 0L;
                SoundUtil.playClick(player);
                initializeItems();
            });

            // Slot 23: +10
            setItem(23, plugin.getHeadManager().createBuilder("ui.plus")
                    .name("<green>+10 монет</green>")
                    .lore("<gray>Увеличить ставку на 10</gray>")
                    .build(), click -> adjustBet(10, maxAllowed));

            // Slot 24: +50
            setItem(24, plugin.getHeadManager().createBuilder("ui.plus")
                    .name("<green>+50 монет</green>")
                    .lore("<gray>Увеличить ставку на 50</gray>")
                    .build(), click -> adjustBet(50, maxAllowed));

            // Slot 25: Max
            setItem(25, plugin.getHeadManager().createBuilder("ui.confirm_arrow")
                    .name("<gold>Максимальная ставка</gold>")
                    .lore(
                            "<gray>Установить максимальную ставку: <gold>" + maxAllowed + "</gold></gray>",
                            npcConfig.getMaxBet() > 0 ? "<gray>Лимит NPC: <yellow>" + npcConfig.getMaxBet() + "</yellow></gray>" : "<gray>По балансу игрока</gray>"
                    )
                    .build(), click -> {
                currentBet = Math.max(0L, maxAllowed);
                SoundUtil.playClick(player);
                initializeItems();
            });
        }
    }

    private void adjustBet(long delta, long maxAllowed) {
        long newBet = currentBet + delta;
        if (newBet < 0) newBet = 0;
        if (newBet > maxAllowed) newBet = maxAllowed;
        if (newBet != currentBet) {
            currentBet = newBet;
            SoundUtil.playClick(player);
            initializeItems();
        } else {
            SoundUtil.playError(player);
        }
    }
}
