package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.LeaveUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Disconnects you from the server when health is low or a player gets close. */
public class AutoLeave extends ClientModule {
    private final NumberSetting health = add(new NumberSetting("Leave Below Health", 6, 0, 20, 1));
    private final NumberSetting totems = add(new NumberSetting("Leave Below Totems (0 = off)", 0, 0, 36, 1));
    private final NumberSetting nearby = add(new NumberSetting("Leave If Player Within (0 = off)", 0, 0, 200, 4));

    public AutoLeave() { super("AutoLeave", "Auto-disconnect when in danger", Category.COMBAT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (health.i() > 0 && p.getHealth() <= health.i()) { setEnabled(false); LeaveUtil.leave(mc, "AutoLeave", "low health"); return; }
        if (totems.i() > 0) {
            int n = p.getOffHandStack().isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING) ? p.getOffHandStack().getCount() : 0;
            for (int i = 0; i < 36; i++) { var st = p.getInventory().getStack(i); if (st.isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING)) n += st.getCount(); }
            if (n < totems.i()) { setEnabled(false); LeaveUtil.leave(mc, "AutoLeave", "only " + n + " totem(s) left"); return; }
        }
        if (nearby.i() > 0)
            for (Entity e : mc.world.getEntities())
                if (e instanceof PlayerEntity o && o != p && p.distanceTo(o) <= nearby.i()) {
                    setEnabled(false); LeaveUtil.leave(mc, "AutoLeave", "player nearby (" + o.getName().getString() + ")"); return;
                }
    }
}
