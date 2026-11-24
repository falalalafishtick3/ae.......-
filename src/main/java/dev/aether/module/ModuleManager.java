package dev.aether.module;

import dev.aether.module.impl.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import dev.aether.util.Perf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<ClientModule> MODULES = new ArrayList<>();

    public static void init() {
        MODULES.add(new Aimbot());
        MODULES.add(new CrystalMacro());
        MODULES.add(new AnchorMacro());
        MODULES.add(new StunSlam());
        MODULES.add(new KillAura());
        MODULES.add(new Criticals());
        MODULES.add(new Triggerbot());
        MODULES.add(new Hitboxes());
        MODULES.add(new AutoWeapon());
        MODULES.add(new TotemAlert());
        MODULES.add(new AttackIndicator());
        MODULES.add(new NearbyPlayers());
        MODULES.add(new HealthVignette());
        MODULES.add(new DurabilityAlert());
        MODULES.add(new AutoBlock());
        MODULES.add(new AutoGap());
        MODULES.add(new PopCounter());
        MODULES.add(new QuickPearl());
        MODULES.add(new QuickXP());
        MODULES.add(new QuickFirework());
        MODULES.add(new FriendToggle());
        MODULES.add(new AutoEat());
        MODULES.add(new Reach());
        MODULES.add(new Velocity());
        MODULES.add(new AutoArmor());
        MODULES.add(new NoFall());
        MODULES.add(new FastPlace());
        MODULES.add(new AutoTotem());
        MODULES.add(new Freecam());
        MODULES.add(new Freelook());
        MODULES.add(new Flight());
        MODULES.add(new AutoWalk());
        MODULES.add(new ChestSwap());
        MODULES.add(new YawLock());
        MODULES.add(new ElytraPilot());
        MODULES.add(new InventoryMove());
        MODULES.add(new WaypointWalk());
        MODULES.add(new SafeWalk());
        MODULES.add(new Parkour());
        MODULES.add(new ElytraFly());
        MODULES.add(new FastLadder());
        MODULES.add(new Scaffold());
        MODULES.add(new Spider());
        MODULES.add(new Glide());
        MODULES.add(new Jesus());
        MODULES.add(new AntiAFK());
        MODULES.add(new Sprint());
        MODULES.add(new StorageFinder());
        MODULES.add(new SpawnerFinder());
        MODULES.add(new Visuals());
        MODULES.add(new LogoutSpots());
        MODULES.add(new Trajectories());
        MODULES.add(new AutoCommand());
        MODULES.add(new TpsHud());
        MODULES.add(new StorageCount());
        MODULES.add(new HudTweaks());
        MODULES.add(new JoinLeave());
        MODULES.add(new Breadcrumbs());
        MODULES.add(new HoleESP());
        MODULES.add(new LightOverlay());
        MODULES.add(new TuffChunkFinder());
        MODULES.add(new AutoClicker());
        MODULES.add(new Speed());
        MODULES.add(new Step());
        MODULES.add(new Zoom());
        MODULES.add(new FpsBoost());
        MODULES.add(new SpotifyHud());
        MODULES.add(new ActiveMods());
        MODULES.add(new InfoHud());
        MODULES.add(new TargetHud());
        MODULES.add(new Radar());
        MODULES.add(new Keystrokes());
        MODULES.add(new Notifications());
        MODULES.add(new Watermark());
        MODULES.add(new ArmorHud());
        MODULES.add(new PotionHud());
        MODULES.add(new BaseFinder());
        MODULES.add(new OreFinder());
        MODULES.add(new AutoReplenish());
        MODULES.add(new AutoDrop());
        MODULES.add(new CopyCoords());
        MODULES.add(new SoundESP());
        MODULES.add(new NewChunks());
        MODULES.add(new CustomESP());
        MODULES.add(new LootAlert());
        MODULES.add(new PlayerLog());
        MODULES.add(new BlockSearch());
        MODULES.add(new EntityStash());
        MODULES.add(new PlacedBlocks());
        MODULES.add(new SignReader());
        MODULES.add(new AutoMine());
        MODULES.add(new Xray());
        MODULES.add(new BaseHunter());
        MODULES.add(new AutoLeave());
        MODULES.add(new AutoRespawn());
        MODULES.add(new DeathCoords());
        MODULES.add(new PlayerAlert());
        MODULES.add(new AutoSneak());
        MODULES.add(new AutoTool());
        MODULES.add(new AutoFish());
        MODULES.add(new ChestStealer());
        MODULES.add(new ChunkBorders());
        MODULES.add(new FastBreak());
        MODULES.add(new BaseMap());
        MODULES.add(new HuntStats());
        MODULES.add(new Compass());
        MODULES.add(new InvHud());
        MODULES.add(new ItemCounter());
        MODULES.add(new Waypoints());
        MODULES.add(new AddWaypoint());
        MODULES.add(new OpenHub());
        MODULES.add(new Panic());
        MODULES.add(new DonutEconomy());
        MODULES.add(new PerfMonitor());
        MODULES.add(new HuntAlerts());
        MODULES.add(new ChatAlert());
        MODULES.add(new ChatTimestamps());
        MODULES.add(new AntiVanish());
        MODULES.add(new AutoReconnect());
        MODULES.add(new ClickGuiModule());
    }
    public static List<ClientModule> in(Category c) { return MODULES.stream().filter(m -> m.category == c).toList(); }

    public static void tick(MinecraftClient mc) {
        // keybinds (only while no screen is open)
        for (ClientModule m : MODULES) {
            if (m.key < 0) continue;
            boolean down = mc.currentScreen == null && InputUtil.isKeyPressed(mc.getWindow(), m.key);
            if (m.bindMode.is("Hold")) {
                if (down && !m.keyWasDown) m.setEnabled(true);
                else if (!down && m.keyWasDown) m.setEnabled(false);
            } else if (down && !m.keyWasDown) m.toggle();
            m.keyWasDown = down;
        }
        if (Perf.on) {
            Perf.call(Perf.T);
            for (ClientModule m : MODULES) if (m.enabled) { long t0 = System.nanoTime(); m.onTick(mc); Perf.add(Perf.T, m, System.nanoTime() - t0); }
        } else for (ClientModule m : MODULES) if (m.enabled) m.onTick(mc);
    }

    public static void renderHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        if (Perf.on) {
            Perf.call(Perf.H); Perf.frame();
            for (ClientModule m : MODULES) if (m.enabled) { long t0 = System.nanoTime(); m.onHud(g, tc); Perf.add(Perf.H, m, System.nanoTime() - t0); }
        } else for (ClientModule m : MODULES) if (m.enabled) m.onHud(g, tc);
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (Perf.on) {
            Perf.call(Perf.R);
            for (ClientModule m : MODULES) if (m.enabled) { long t0 = System.nanoTime(); m.onRender(ctx, mc); Perf.add(Perf.R, m, System.nanoTime() - t0); }
        } else for (ClientModule m : MODULES) if (m.enabled) m.onRender(ctx, mc);
    }
}
