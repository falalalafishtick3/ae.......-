package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;

public class Keystrokes extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Bottom Right", "Top Left", "Top Right", "Bottom Left"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 8, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 8, 0, 400, 1));
    private final BoolSetting mouse  = add(new BoolSetting("Mouse Buttons", true));
    private final BoolSetting space  = add(new BoolSetting("Space Bar", true));

    public Keystrokes() { super("Keystrokes", "Shows pressed movement keys", Category.HUD); }

    private void key(DrawContext g, String label, KeyBinding k, int x, int y, int w, int h) {
        var tr = MinecraftClient.getInstance().textRenderer;
        boolean on = k.isPressed();
        dev.aether.gui.Ui.rrect(g, x, y, w, h, 4, on ? (ClickGuiModule.accent() & 0x00FFFFFF) | 0xE0000000 : 0xB00D1015);
        g.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, on ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        var o = MinecraftClient.getInstance().options;
        final int s = 20, gap = 2;
        int w = s * 3 + gap * 2, h = s * 2 + gap + (mouse.get() ? s + gap : 0) + (space.get() ? 12 + gap : 0);
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        int x = p[0], y = p[1];
        key(g, "W", o.forwardKey, x + s + gap, y, s, s);
        key(g, "A", o.leftKey, x, y + s + gap, s, s);
        key(g, "S", o.backKey, x + s + gap, y + s + gap, s, s);
        key(g, "D", o.rightKey, x + 2 * (s + gap), y + s + gap, s, s);
        int ny = y + 2 * (s + gap);
        if (mouse.get()) {
            int bw = (w - gap) / 2;
            key(g, "LMB", o.attackKey, x, ny, bw, s);
            key(g, "RMB", o.useKey, x + bw + gap, ny, bw, s);
            ny += s + gap;
        }
        if (space.get()) key(g, "_____", o.jumpKey, x, ny, w, 12);
    }
}
