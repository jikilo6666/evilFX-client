package dev.evilfx.feature;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Null-safe item counting helpers (main inventory + offhand, or hotbar only). */
public final class InventoryUtil {
    private InventoryUtil() {}

    public static int count(PlayerEntity player, Item item) {
        if (player == null) return 0;
        PlayerInventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        ItemStack off = player.getOffHandStack();
        if (off.isOf(item)) n += off.getCount();
        return n;
    }

    public static int hotbar(PlayerEntity player, Item item) {
        if (player == null) return 0;
        PlayerInventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }
}
