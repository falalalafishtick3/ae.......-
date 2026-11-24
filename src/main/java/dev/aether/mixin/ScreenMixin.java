package dev.aether.mixin;

import dev.aether.module.impl.ClickGuiModule;
import dev.aether.render.WinterBackground;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swaps the rotating panorama for the winter scene on menus (only when not inside a world). */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), cancellable = true)
    private void aether$winterMenu(DrawContext context, float deltaTicks, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        String mode = ClickGuiModule.menuMode();
        if (mode.equals("Vanilla") || MinecraftClient.getInstance().world != null) return;
        if (mode.equals("Winter: Title Only") && !(self instanceof TitleScreen)) return;
        WinterBackground.draw(context, self.width, self.height, true);
        ci.cancel();
    }
}
