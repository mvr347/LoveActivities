package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.api.GameType;
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
import java.util.function.Consumer;

public class PhysicalDepositGUI extends AbstractGUI {

    private final Player opponent;
    private final GameType gameType;
    private final List<ItemStack> initialItems;
    private final Consumer<List<ItemStack>> onConfirmed;
    private final Runnable onCancelled;

    private static final int[] DEPOSIT_SLOTS = {21, 22, 23};
    private boolean confirmed = false;

    public PhysicalDepositGUI(Player player, Player opponent, GameType gameType,
                              List<ItemStack> initialItems,
                              Consumer<List<ItemStack>> onConfirmed,
                              Runnable onCancelled) {
        super(player, 45, "<gradient:#FF5E62:#FF9966><bold>Внесение ставки (Физические монеты)</bold></gradient>");
        this.opponent = opponent;
        this.gameType = gameType;
        this.initialItems = initialItems != null ? new ArrayList<>(initialItems) : new ArrayList<>();
        this.onConfirmed = onConfirmed;
        this.onCancelled = onCancelled;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        clickActions.clear();

        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();

        // Header Row 0 (0-8) & Row 1 (9-17)
        for (int i = 0; i <= 17; i++) {
            inventory.setItem(i, glass);
        }

        // Row 3 & 4 (27-44)
        for (int i = 27; i < 45; i++) {
            inventory.setItem(i, glass);
        }

        // Side slots in row 2 (18, 19, 20, 24, 25, 26)
        inventory.setItem(18, glass);
        inventory.setItem(19, glass);
        inventory.setItem(20, glass);
        inventory.setItem(24, glass);
        inventory.setItem(25, glass);
        inventory.setItem(26, glass);

        // Header Slot 0: Player Head
        setItem(0, ItemBuilder.skull().playerHead(player.getUniqueId())
                .name("<gradient:#00C9FF:#92FE9D><bold>" + player.getName() + "</bold></gradient>")
                .lore("<gray>Внесите физическую валюту в 3 слота по центру</gray>")
                .build());

        // Header Slot 4: Game Info
        setItem(4, plugin.getHeadManager().createBuilder("game_icons." + gameType.getIconKey())
                .name("<yellow><bold>" + gameType.getNameRu() + "</bold></yellow>")
                .lore(
                        "<gray>Соперник: <white>" + (opponent != null ? opponent.getName() : "Игрок") + "</white></gray>",
                        "<gray>Перетащите монеты из своего инвентаря в слоты 1, 2, 3</gray>"
                )
                .build());

        // Put initial items back in deposit slots if present
        for (int i = 0; i < DEPOSIT_SLOTS.length && i < initialItems.size(); i++) {
            inventory.setItem(DEPOSIT_SLOTS[i], initialItems.get(i));
        }

        updateStatusDisplay();

        // Slot 38: Cancel Button
        setItem(38, plugin.getHeadManager().createBuilder("ui.cancel")
                .name("<red><bold>Отменить</bold></red>")
                .lore("<gray>Забрать монеты и отменить игру</gray>")
                .build(), click -> {
            confirmed = false;
            SoundUtil.playClick(player);
            returnDepositedItems();
            setSwitchingInventory(true);
            player.closeInventory();
            if (onCancelled != null) {
                onCancelled.run();
            }
        });

        // Slot 42: Confirm Button
        setItem(42, plugin.getHeadManager().createBuilder("ui.confirm")
                .name("<green><bold>Подтвердить ставку</bold></green>")
                .lore(
                        "<gray>Зафиксировать внесённые монеты</gray>",
                        "<yellow>▶ Нажмите для перехода к подтверждению</yellow>"
                )
                .build(), click -> {
            confirmed = true;
            SoundUtil.playSuccess(player);
            List<ItemStack> items = getDepositedItems();
            setSwitchingInventory(true);
            player.closeInventory();
            if (onConfirmed != null) {
                onConfirmed.accept(items);
            }
        });
    }

    public void updateStatusDisplay() {
        long total = calculateDepositedValue();
        setItem(31, plugin.getHeadManager().createBuilder("ui.coin_stack")
                .name("<gold><bold>Внесено: " + CurrencyUtil.formatCoinsShort(total) + "</bold></gold>")
                .lore(
                        "<gray>Состав ставки: <yellow>" + CurrencyUtil.formatCoinsWords(total) + "</yellow></gray>",
                        "<gray>Все монеты в центральных 3 слотах учитываются как ваша ставка.</gray>",
                        total == 0 ? "<yellow>Ставка 0 = Дружеский матч</yellow>" : "<green>Ставка готова к подтверждению!</green>"
                )
                .build());
    }

    public List<ItemStack> getDepositedItems() {
        List<ItemStack> list = new ArrayList<>();
        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                list.add(item.clone());
            }
        }
        return list;
    }

    public long calculateDepositedValue() {
        long total = 0;
        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                total += CurrencyUtil.getCoinValue(item);
            }
        }
        return total;
    }

    public void returnDepositedItems() {
        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                inventory.setItem(slot, null);
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
        }
    }

    public boolean isDepositSlot(int slot) {
        for (int s : DEPOSIT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    @Override
    public void handleClose() {
        if (!confirmed && !isSwitchingInventory()) {
            returnDepositedItems();
            if (onCancelled != null) {
                onCancelled.run();
            }
        }
    }
}
