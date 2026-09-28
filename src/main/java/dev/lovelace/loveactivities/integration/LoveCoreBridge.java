package dev.lovelace.loveactivities.integration;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.economy.LoveEconomy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class LoveCoreBridge {

    public boolean hasEconomy() {
        try {
            return LoveCore.service(LoveEconomy.class).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Optional<LoveEconomy> getEconomy() {
        try {
            return LoveCore.service(LoveEconomy.class);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public boolean hasBalance(Player player, long amount) {
        if (amount <= 0) return true;
        if (player == null || !player.isOnline()) return false;

        Optional<LoveEconomy> eco = getEconomy();
        if (eco.isPresent()) {
            return eco.get().has(player, amount);
        }

        long physicalTotal = 0L;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                physicalTotal += CurrencyUtil.getCoinValue(item);
                if (physicalTotal >= amount) return true;
            }
        }
        return physicalTotal >= amount;
    }

    public long getBalance(Player player) {
        if (player == null || !player.isOnline()) return 0L;

        Optional<LoveEconomy> eco = getEconomy();
        if (eco.isPresent()) {
            return eco.get().balance(player);
        }

        long physicalTotal = 0L;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                physicalTotal += CurrencyUtil.getCoinValue(item);
            }
        }
        return physicalTotal;
    }

    public boolean charge(Player player, long amount) {
        if (amount <= 0) return true;
        if (player == null || !player.isOnline()) return false;

        Optional<LoveEconomy> eco = getEconomy();
        if (eco.isPresent()) {
            return eco.get().charge(player, amount);
        }

        long needed = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR && CurrencyUtil.isCoin(item)) {
                long unitVal = CurrencyUtil.getUnitCoinValue(item);
                int count = item.getAmount();
                long stackVal = unitVal * count;
                if (stackVal <= needed) {
                    needed -= stackVal;
                    player.getInventory().setItem(i, null);
                } else {
                    int removeCount = (int) ((needed + unitVal - 1) / unitVal);
                    if (removeCount > count) removeCount = count;
                    long totalRemovedVal = (long) removeCount * unitVal;
                    long change = totalRemovedVal - needed;
                    item.setAmount(count - removeCount);
                    if (item.getAmount() <= 0) {
                        player.getInventory().setItem(i, null);
                    }
                    needed = 0;
                    if (change > 0) {
                        CurrencyUtil.giveCoinsToPlayer(player, change);
                    }
                    break;
                }
                if (needed <= 0) break;
            }
        }
        return true;
    }

    public void give(Player player, long amount) {
        if (amount <= 0) return;
        if (player == null || !player.isOnline()) return;

        Optional<LoveEconomy> eco = getEconomy();
        if (eco.isPresent()) {
            eco.get().give(player, amount);
        } else {
            CurrencyUtil.giveCoinsToPlayer(player, amount);
        }
    }

    public void give(UUID uuid, long amount, String reason) {
        if (amount <= 0 || uuid == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            give(player, amount);
        } else {
            LoveActivities.getInstance().getOfflinePayoutDao().addPayout(uuid, amount, reason);
        }
    }

    public void give(UUID uuid, long amount) {
        give(uuid, amount, "Game payout");
    }

    public String currencyName() {
        return getEconomy().map(LoveEconomy::currencyName).orElse("монет");
    }

    /**
     * Плохая / конфликтная репутация — NPC не играют с таким игроком.
     * OUTCAST и BAD по ReputationOracle; если Oracle нет — пропускаем (разрешаем).
     */
    public boolean isBadOrConflictReputation(UUID playerId) {
        if (playerId == null) return false;
        try {
            return LoveCore.service(dev.lovelace.lovecore.api.social.ReputationOracle.class)
                    .map(oracle -> {
                        var tier = oracle.tier(playerId);
                        return tier == dev.lovelace.lovecore.api.social.ReputationOracle.Tier.OUTCAST
                                || tier == dev.lovelace.lovecore.api.social.ReputationOracle.Tier.BAD;
                    }).orElse(false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean hasDisallowedReputation(UUID playerId) {
        return isBadOrConflictReputation(playerId);
    }
}
