package dev.lovelace.loveactivities.gui;

import dev.lovelace.loveactivities.LoveActivities;
import dev.lovelace.loveactivities.util.ItemBuilder;
import dev.lovelace.loveactivities.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractGUI implements InventoryHolder {

    protected final LoveActivities plugin;
    protected final Player player;
    protected final int size;
    protected final Component title;
    protected final UUID guiId = UUID.randomUUID();
    protected Inventory inventory;
    protected final Map<Integer, Consumer<ClickType>> clickActions = new HashMap<>();
    protected boolean switchingInventory = false;
    protected boolean closed = false;

    public AbstractGUI(Player player, int size, String titleMiniMessage) {
        this(player, size, TextUtil.parse(titleMiniMessage));
    }

    public AbstractGUI(Player player, int size, Component title) {
        this.plugin = LoveActivities.getInstance();
        this.player = player;
        this.size = size;
        this.title = title;
        this.inventory = create(size, title);
    }

    /** 5 slots means a hopper menu (the confirmation menus); everything else is a chest of that size. */
    private Inventory create(int slots, Component name) {
        return slots == 5
                ? Bukkit.createInventory(this, org.bukkit.event.inventory.InventoryType.HOPPER, name)
                : Bukkit.createInventory(this, slots, name);
    }

    public abstract void initializeItems();

    public void setItem(int slot, ItemStack item, Consumer<ClickType> action) {
        if (slot >= 0 && slot < size) {
            inventory.setItem(slot, item);
            if (action != null) {
                clickActions.put(slot, action);
            } else {
                clickActions.remove(slot);
            }
        }
    }

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void setItem(int slot, ItemBuilder builder, Consumer<ClickType> action) {
        setItem(slot, builder != null ? builder.build() : null, action);
    }

    public void fillGlassRow(int startSlot, int endSlot) {
        ItemStack glass = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = startSlot; i <= endSlot && i < size; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, glass);
            }
        }
    }

    public void open() {
        this.closed = false;
        this.switchingInventory = false;
        initializeItems();
        player.openInventory(inventory);
    }

    private long lastClickTime = 0L;

    public void handleClick(int slot, ClickType clickType) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 60L) {
            return; // Anti-spam click debounce
        }
        lastClickTime = now;

        Consumer<ClickType> action = clickActions.get(slot);
        if (action != null) {
            action.accept(clickType);
        }
    }

    public void handleClose() {
        // Overridden by subclasses if needed
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isSwitchingInventory() {
        return switchingInventory;
    }

    public void setSwitchingInventory(boolean switchingInventory) {
        this.switchingInventory = switchingInventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getGuiId() {
        return guiId;
    }
}
