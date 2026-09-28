package dev.lovelace.loveactivities.listener;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.gui.PhysicalDepositGUI;
import dev.lovelace.loveactivities.gui.SharedBetReviewGUI;
import dev.lovelace.loveactivities.util.CurrencyUtil;
import dev.lovelace.loveactivities.util.SoundUtil;
import dev.lovelace.loveactivities.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof AbstractGUI gui) {

            // Shared Simultaneous Betting Room
            if (gui instanceof SharedBetReviewGUI betGUI) {
                Player clicker = (Player) event.getWhoClicked();
                boolean isP1 = clicker.getUniqueId().equals(betGUI.getChallenger().getUniqueId());
                boolean isP2 = clicker.getUniqueId().equals(betGUI.getTarget().getUniqueId());
                int rawSlot = event.getRawSlot();
                int topSize = event.getInventory().getSize();

                // If player is clicking in their own bottom inventory
                if (rawSlot >= topSize) {
                    if (event.isShiftClick()) {
                        event.setCancelled(true);
                        ItemStack moving = event.getCurrentItem();
                        if (moving != null && moving.getType() != Material.AIR) {
                            if (!CurrencyUtil.isCoin(moving)) {
                                SoundUtil.playError(clicker);
                                clicker.sendMessage(TextUtil.parse("<red>В ставку можно добавлять только монеты!</red>"));
                                return;
                            }

                            int[] targetSlots = isP1 ? SharedBetReviewGUI.SLOTS_P1 : SharedBetReviewGUI.SLOTS_P2;
                            boolean transferred = false;

                            // 1. Try to stack into existing matching stacks
                            for (int dSlot : targetSlots) {
                                ItemStack cur = event.getInventory().getItem(dSlot);
                                if (cur != null && cur.isSimilar(moving)) {
                                    int max = cur.getMaxStackSize();
                                    int canAdd = Math.min(moving.getAmount(), max - cur.getAmount());
                                    if (canAdd > 0) {
                                        cur.setAmount(cur.getAmount() + canAdd);
                                        event.getInventory().setItem(dSlot, cur);
                                        moving.setAmount(moving.getAmount() - canAdd);
                                        transferred = true;
                                        if (moving.getAmount() <= 0) {
                                            event.setCurrentItem(null);
                                            break;
                                        } else {
                                            event.setCurrentItem(moving);
                                        }
                                    }
                                }
                            }

                            // 2. Place into empty target slots if still remaining
                            if (moving != null && moving.getAmount() > 0) {
                                for (int dSlot : targetSlots) {
                                    ItemStack cur = event.getInventory().getItem(dSlot);
                                    if (cur == null || cur.getType() == Material.AIR) {
                                        event.getInventory().setItem(dSlot, moving.clone());
                                        event.setCurrentItem(null);
                                        transferred = true;
                                        break;
                                    }
                                }
                            }

                            if (transferred && LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                                Bukkit.getScheduler().runTask(LoveActivities.getInstance(), isP1 ? betGUI::onP1SlotsChanged : betGUI::onP2SlotsChanged);
                            }
                        }
                    }
                    return;
                }

                // If clicking directly in top GUI
                if (betGUI.isP1DepositSlot(rawSlot)) {
                    if (!isP1) {
                        event.setCancelled(true);
                        SoundUtil.playError(clicker);
                        clicker.sendMessage(TextUtil.parse("<red>Вы не можете изменять слоты соперника!</red>"));
                        return;
                    }
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR && !CurrencyUtil.isCoin(cursor)) {
                        event.setCancelled(true);
                        SoundUtil.playError(clicker);
                        clicker.sendMessage(TextUtil.parse("<red>В этот слот можно класть только монеты!</red>"));
                        return;
                    }
                    if (LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                        Bukkit.getScheduler().runTask(LoveActivities.getInstance(), betGUI::onP1SlotsChanged);
                    }
                    return;
                }

                if (betGUI.isP2DepositSlot(rawSlot)) {
                    if (!isP2) {
                        event.setCancelled(true);
                        SoundUtil.playError(clicker);
                        clicker.sendMessage(TextUtil.parse("<red>Вы не можете изменять слоты соперника!</red>"));
                        return;
                    }
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR && !CurrencyUtil.isCoin(cursor)) {
                        event.setCancelled(true);
                        SoundUtil.playError(clicker);
                        clicker.sendMessage(TextUtil.parse("<red>В этот слот можно класть только монеты!</red>"));
                        return;
                    }
                    if (LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                        Bukkit.getScheduler().runTask(LoveActivities.getInstance(), betGUI::onP2SlotsChanged);
                    }
                    return;
                }

                // Control buttons in top GUI
                event.setCancelled(true);

                if (rawSlot == SharedBetReviewGUI.SLOT_P1_READY) {
                    if (!isP1) {
                        SoundUtil.playError(clicker);
                        return;
                    }
                    betGUI.toggleReadyP1();
                    return;
                }

                if (rawSlot == SharedBetReviewGUI.SLOT_P2_READY) {
                    if (!isP2) {
                        SoundUtil.playError(clicker);
                        return;
                    }
                    betGUI.toggleReadyP2();
                    return;
                }

                if (rawSlot == SharedBetReviewGUI.SLOT_CANCEL) {
                    betGUI.cancelAndReturnAll();
                    return;
                }

                if (rawSlot == SharedBetReviewGUI.SLOT_STATUS) {
                    if (betGUI.getState().countdownTask != null) {
                        betGUI.cancelCountdownByUser(clicker);
                    }
                    return;
                }

                return;
            }

            // Legacy / fallback PhysicalDepositGUI
            if (gui instanceof PhysicalDepositGUI depositGUI) {
                int rawSlot = event.getRawSlot();
                int topSize = event.getInventory().getSize();

                if (rawSlot >= topSize) {
                    if (event.isShiftClick()) {
                        event.setCancelled(true);
                        ItemStack moving = event.getCurrentItem();
                        if (moving != null && moving.getType() != Material.AIR) {
                            for (int dSlot : new int[]{21, 22, 23}) {
                                ItemStack cur = event.getInventory().getItem(dSlot);
                                if (cur == null || cur.getType() == Material.AIR) {
                                    event.getInventory().setItem(dSlot, moving.clone());
                                    event.setCurrentItem(null);
                                    break;
                                } else if (cur.isSimilar(moving)) {
                                    int max = cur.getMaxStackSize();
                                    int canAdd = Math.min(moving.getAmount(), max - cur.getAmount());
                                    if (canAdd > 0) {
                                        cur.setAmount(cur.getAmount() + canAdd);
                                        event.getInventory().setItem(dSlot, cur);
                                        moving.setAmount(moving.getAmount() - canAdd);
                                        if (moving.getAmount() <= 0) {
                                            event.setCurrentItem(null);
                                        } else {
                                            event.setCurrentItem(moving);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                        Bukkit.getScheduler().runTask(LoveActivities.getInstance(), depositGUI::updateStatusDisplay);
                    }
                    return;
                }

                if (depositGUI.isDepositSlot(rawSlot)) {
                    if (LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                        Bukkit.getScheduler().runTask(LoveActivities.getInstance(), depositGUI::updateStatusDisplay);
                    }
                    return;
                }

                event.setCancelled(true);
                if (rawSlot >= 0 && rawSlot < topSize) {
                    gui.handleClick(rawSlot, event.getClick());
                }
                return;
            }

            // Normal locked GUI
            event.setCancelled(true);
            if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
                gui.handleClick(event.getRawSlot(), event.getClick());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof AbstractGUI gui) {

            if (gui instanceof SharedBetReviewGUI betGUI) {
                Player clicker = (Player) event.getWhoClicked();
                boolean isP1 = clicker.getUniqueId().equals(betGUI.getChallenger().getUniqueId());
                boolean isP2 = clicker.getUniqueId().equals(betGUI.getTarget().getUniqueId());
                int topSize = event.getInventory().getSize();

                boolean allValid = true;
                for (int rawSlot : event.getRawSlots()) {
                    if (rawSlot < topSize) {
                        if (isP1 && !betGUI.isP1DepositSlot(rawSlot)) {
                            allValid = false;
                            break;
                        } else if (isP2 && !betGUI.isP2DepositSlot(rawSlot)) {
                            allValid = false;
                            break;
                        }
                    }
                }

                ItemStack oldCursor = event.getOldCursor();
                if (oldCursor != null && oldCursor.getType() != Material.AIR && !CurrencyUtil.isCoin(oldCursor)) {
                    allValid = false;
                }

                if (allValid) {
                    if (LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                        Bukkit.getScheduler().runTask(LoveActivities.getInstance(), isP1 ? betGUI::onP1SlotsChanged : betGUI::onP2SlotsChanged);
                    }
                    return;
                }

                event.setCancelled(true);
                return;
            }

            if (gui instanceof PhysicalDepositGUI depositGUI) {
                int topSize = event.getInventory().getSize();
                boolean allValid = true;
                for (int rawSlot : event.getRawSlots()) {
                    if (rawSlot < topSize && !depositGUI.isDepositSlot(rawSlot)) {
                        allValid = false;
                        break;
                    }
                }
                if (allValid) {
                    if (LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                        Bukkit.getScheduler().runTask(LoveActivities.getInstance(), depositGUI::updateStatusDisplay);
                    }
                    return;
                }
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof AbstractGUI gui) {
            if (gui.isClosed() || gui.isSwitchingInventory()) {
                return;
            }
            gui.setClosed(true);
            gui.handleClose();
        }
    }
}
