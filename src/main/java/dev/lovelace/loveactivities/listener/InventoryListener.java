package dev.lovelace.loveactivities.listener;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.gui.AbstractGUI;
import dev.lovelace.loveactivities.gui.PhysicalDepositGUI;
import org.bukkit.Bukkit;
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

            if (gui instanceof PhysicalDepositGUI depositGUI) {
                int rawSlot = event.getRawSlot();
                int topSize = event.getInventory().getSize();

                // If player is clicking in their own bottom inventory
                if (rawSlot >= topSize) {
                    if (event.isShiftClick()) {
                        event.setCancelled(true);
                        ItemStack moving = event.getCurrentItem();
                        if (moving != null && moving.getType() != org.bukkit.Material.AIR) {
                            for (int dSlot : new int[]{21, 22, 23}) {
                                ItemStack cur = event.getInventory().getItem(dSlot);
                                if (cur == null || cur.getType() == org.bukkit.Material.AIR) {
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

                // If clicking directly on one of the 3 deposit slots
                if (depositGUI.isDepositSlot(rawSlot)) {
                    if (LoveActivities.getInstance() != null && LoveActivities.getInstance().isEnabled()) {
                        Bukkit.getScheduler().runTask(LoveActivities.getInstance(), depositGUI::updateStatusDisplay);
                    }
                    return;
                }

                // Any other GUI button / frame slot: cancel and handle button action
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
