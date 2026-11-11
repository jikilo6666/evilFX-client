package dev.evilfx.hud;

import dev.evilfx.config.ModConfig;
import dev.evilfx.config.ModConfig.HudSettings;
import dev.evilfx.feature.InventoryUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Armor + held items. Damageable items show remaining durability (vanilla bar color, raw or percent);
 * stackable items show the total amount you carry. Rebuilt 20x per second.
 */
public final class ArmorHud extends AbstractHud {
    private static final int ROW = 18;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private record Entry(ItemStack stack, String text, int color) {}

    private final List<Entry> entries = new ArrayList<>();

    @Override
    protected HudSettings settings() {
        return ModConfig.get().armor;
    }

    @Override
    protected boolean prepare() {
        if (refreshDue(50)) rebuild();
        return !entries.isEmpty();
    }

    private void rebuild() {
        entries.clear();
        if (mc.player == null) return;
        for (EquipmentSlot slot : ARMOR) add(mc.player.getEquippedStack(slot));
        if (ModConfig.get().armorShowHeld) {
            add(mc.player.getMainHandStack());
            add(mc.player.getOffHandStack());
        }
        int max = 0;
        for (Entry e : entries) max = Math.max(max, mc.textRenderer.getWidth(e.text()));
        w = 20 + max + 2;
        h = Math.max(0, entries.size() * ROW - 2);
    }

    private void add(ItemStack stack) {
        if (stack.isEmpty()) return;
        ModConfig cfg = ModConfig.get();
        if (stack.isDamageable()) {
            int max = stack.getMaxDamage();
            int left = max - stack.getDamage();
            String t = cfg.armorPercent ? Math.round(left * 100f / max) + "%" : String.valueOf(left);
            entries.add(new Entry(stack, t, 0xFF000000 | stack.getItemBarColor()));
        } else if (stack.getMaxCount() > 1) {
            int total = InventoryUtil.count(mc.player, stack.getItem());
            entries.add(new Entry(stack, "x" + total, argb(cfg.armor)));
        }
    }

    @Override
    protected void draw(DrawContext ctx, HudSettings s) {
        background(ctx, s);
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int y = i * ROW;
            ctx.drawItem(e.stack(), 0, y);
            text(ctx, e.text(), 20, y + 4, e.color(), s.shadow);
        }
    }
}
