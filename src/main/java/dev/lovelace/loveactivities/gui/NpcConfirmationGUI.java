package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.manager.NpcActivityConfig;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class NpcConfirmationGUI extends AbstractGUI {

    private final NpcActivityConfig npcConfig;

    public NpcConfirmationGUI(Player player, NpcActivityConfig npcConfig) {
        super(player, 27, "<gradient:#FF9966:#FF5E62>Игра с " + npcConfig.getCustomName() + "</gradient>");
        this.npcConfig = npcConfig;
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
        long bet = npcConfig.getDefaultBet();

        setItem(4, plugin.getHeadManager().createBuilder("game_icons." + iconKey)
                .name("<yellow>Партия: " + gameName + "</yellow>")
                .lore(
                        "<gray>Соперник: <white>" + npcConfig.getCustomName() + "</white></gray>",
                        "<gray>Ставка: <gold>" + (bet > 0 ? CurrencyUtil.formatCoinsWords(bet) : "Без ставки") + "</gold></gray>",
                        "<gray>Формат: <white>Матч против NPC</white></gray>"
                )
                .build());

        // Slot 11: Accept & Start Button
        setItem(11, plugin.getHeadManager().createBuilder("ui.confirm_ready")
                .name("<green>Начать партию</green>")
                .lore(
                        "<gray>Подтвердить условия и начать игру.</gray>",
                        bet > 0 ? "<gray>Ставка будет удержана в банк матча.</gray>" : "<gray>Дружеская игра без ставки.</gray>",
                        "",
                        "<green>▶ Нажмите для старта</green>"
                )
                .build(), click -> {
            if (plugin.getSessionManager().isInGame(player.getUniqueId())) {
                player.closeInventory();
                SoundUtil.playError(player);
                plugin.getNpcManager().speak(player, npcConfig.getCustomName(), "Ты уже участвуешь в игре!");
                return;
            }

            if (bet > 0 && !plugin.getLoveCoreBridge().hasBalance(player, bet)) {
                SoundUtil.playError(player);
                plugin.getLocaleManager().send(player, "insufficient_funds", Map.of(
                        "cost", String.valueOf(bet),
                        "currency", plugin.getLoveCoreBridge().currencyName()
                ));
                return;
            }

            setSwitchingInventory(true);
            SoundUtil.playClick(player);
            plugin.getSessionManager().startNpcSession(player, npcConfig.getGameType(), bet, npcConfig.getCustomName(), npcConfig);
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
    }
}
