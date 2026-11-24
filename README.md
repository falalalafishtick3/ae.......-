# Aether (Fabric 1.21.11 client)

Push this repo to GitHub. The workflow in `.github/workflows/build.yml` builds on every push;
download the jar from the run's **Artifacts** section (use the one WITHOUT `-sources`).

Right Shift opens the ClickGUI. Right-click a module to expand its settings.
Config saves to `config/aether.json`.

If the build fails on dependency versions, update `gradle.properties` from https://fabricmc.net/develop.
Files marked `VERSION-SENSITIVE` (EspRenderer, AetherClient, ClickGuiScreen input) may need small mapping tweaks.

## Modules
Combat: Aimbot, Triggerbot, Reach, Velocity, AutoArmor, AutoTotem, CrystalMacro, AnchorMacro, StunSlam. Movement: Freecam, Flight, Sprint, NoFall. World also has FastPlace.
Render: StorageFinder, SpawnerFinder, Visuals (fullbright / FOV / entity ESP). World: TuffChunkFinder, BaseFinder.

**Keybinds:** middle-click a module in the GUI, press a key (Backspace clears, Esc cancels).

## New: HUD + sound
- **SpotifyHud** (HUD panel): shows the song from the Spotify *desktop app*. Settings: Show (While Playing / On Song Change / Always),
  Show Duration, Position + offsets, Scale, Background Opacity, Progress, Equalizer, Accent color.
  Windows reads the Spotify window title (no progress bar). Linux needs `playerctl`. macOS uses AppleScript.
- **Startup sound**: plays on the title screen once per launch (voice + chime). Toggle/volume in ClickGUI module settings.
- Other HUD: ActiveMods, InfoHud, TargetHud, Radar, Keystrokes. More gameplay: AutoClicker, Speed, Step, Zoom.

## More modules
Combat: Hitboxes, AutoEat. Movement: AutoWalk, AntiAFK. World: OreFinder, ChunkBorders, FastBreak.
HUD: Notifications, Watermark, ArmorHud, PotionHud. GUI: just start typing to search modules.

## Keybinds (every module)
Middle-click any module in the ClickGUI, then press a key. A **Key Mode** setting appears on that module:
**Toggle** (press to switch on/off) or **Hold** (active only while the key is held).
Defaults: Freelook = hold Left Alt, Zoom = hold C, Xray = toggle X.
**Xray** swaps terrain rendering for only the blocks you choose (not compatible with Sodium/Iris). **Freelook** lets you look around without turning your character.

## Even more modules
**EndBaseFinder** (World): loops `/rtp end` -> waits for chunks -> scans for player bases (ignores End cities/ships) -> saves coords to `config/aether_bases.txt`
-> stays, keeps going, or disconnects. Also leaves if a player gets close or you take damage. AutoLeave, AutoRespawn, DeathCoords, PlayerAlert, AutoSneak, AutoTool, AutoFish, ChestStealer.

## Client-style additions
KillAura, Criticals (Combat) | Scaffold, Spider, Glide, Jesus (Movement) | LogoutSpots (Render).

## Sober-style suite (new)
- **BaseHunter** (replaces EndBaseFinder): pick Dimension = End / Nether / Overworld; loops `/rtp <dim>`, scans, saves finds, then stays / keeps going / disconnects.
- **Hub screen** (buttons in the ClickGUI, or bind **OpenHub**): browse found bases + waypoints, copy coords, base -> waypoint, delete.
- **BaseMap** (HUD map of bases + waypoints), **Waypoints** (in-world markers + distance list), **AddWaypoint** (bind a key to drop one).
- **DonutEconomy**: auction price tooltips, inventory value, money/shards. Put your `/api` key in `config/aether/donut_token.txt`. Experimental.
- **Themes** (ClickGUI module: Light Blue (default)/Teal/Ice/Purple/Sunset/Rose/Mono/Custom), **Favorites** (shift+click a module), **Per-Server Config** (ClickGUI module), **Panic** (Right Ctrl disables everything).

## Batch: more modules
FriendToggle (bind a key, aim at a player: friends are skipped by Aimbot/KillAura/Triggerbot and shown green in Visuals), ElytraFly, FastLadder,
AutoMine, BlockSearch (torches/portals/beacons/TNT/rails/workstations ESP), Compass, InvHud, ItemCounter. Visuals also got Chest Minecart ESP.

## Performance
- ESP outlines now write edges directly (no per-box allocations) and use camera-relative coordinates.
- Block scanners (OreFinder, BlockSearch, TuffChunkFinder) have a per-tick time budget (ClickGUI module -> Scan Budget). Default scan interval is 20 ticks.
- Menu/GUI winter background has a **Background Quality** (High/Medium/Low). Spotify polling is lighter (5s, no PowerShell).
- **PerfMonitor** module: shows which Aether module costs the most (ms per tick / render / hud) so you can switch off the heavy one.
- Tip: install **Sodium** (+ Lithium) for a big FPS boost. Everything works with it except Xray.

## Base-hunting batch
- **EntityStash**: base finder from item frames / armor stands / paintings / chest minecarts+boats / named mobs / dropped items.
- **PlacedBlocks**: finds chunks full of non-natural blocks (metal blocks, glass, concrete/wool, redstone parts, planks...) - catches chest-less bases.
- **SignReader**: prints + saves (`config/aether/signs.txt`) text from nearby signs.
- **HuntAlerts** (on by default): toast, ping sound, auto-copy coords, Discord webhook (`config/aether/webhook.txt`) for every find.
- **HuntStats** HUD, **AutoReconnect** (rejoin after kicks/restarts), InfoHud now shows Nether/Overworld converted coords.

## Keybinds (updated)
Right-click any module -> first row is **Keybind**: left-click it, press a key (F4 works), right-click clears. (Middle-click on the module still works too.)
**Freecam** now has its own mouse-look: your character no longer turns while the camera moves.

## AntiVanish
Detects staff: spectators in the tab list, spectator players right next to you (spectating you), invisible players. On detect: alert / panic (all cheats off) / disconnect.
It cannot see staff that a vanish plugin hides from your client entirely.

## Visual overhaul
ClickGUI rebuilt: rounded panels with shadows/outlines, animated toggle switches, smooth expand/collapse, glow sliders, value chips, module tooltips, cursor glow,
pill search bar and buttons, panels slide in on open. HUD elements share the same rounded glass cards. The main menu has an animated AETHER CLIENT badge.

## FpsBoost
Render panel -> **FpsBoost**: minimal particles, clouds off, entity shadows off, smooth lighting off, biome blend 1, unlock FPS + VSync off, and caps for render/simulation/entity distance.
Original values are saved and restored when you turn it off. For a much larger gain install **Sodium + Lithium** alongside (everything except Xray works with Sodium).

## Batch: more modules
AutoBlock, AutoGap, PopCounter, QuickPearl / QuickXP / QuickFirework (bind a key, press to use) | SafeWalk, Parkour | HoleESP, LightOverlay. SpawnerFinder now prints each spawner's mob type.

## Chat commands + more modules
Type `.help` in chat: `.toggle <module>`, `.bind <module> <KEY>`, `.unbind`, `.modules`, `.wp add|list|del`, `.friend add|remove`, `.coords`, `.panic`. (Messages starting with "." + a letter are never sent to the server.)
New modules: ChestSwap (elytra <-> chestplate), AutoReplenish, AutoDrop, WaypointWalk, CopyCoords, TotemAlert, HealthVignette, DurabilityAlert.

## Batch: sound / elytra / HUD
SoundESP (marks where chests/doors/pistons/block breaking/armor-equip sounds came from), ElytraPilot (heading or waypoint, altitude hold, auto rockets), InventoryMove,
AttackIndicator (cooldown bar), NearbyPlayers (HUD list). InfoHud can show Biome and Light level.

## Hunting + QoL batch
- **NewChunks**: marks NEW vs already-explored (OLD) chunks using the fluid-flow heuristic.
- **CustomESP**: `.esp add <block>` (e.g. `.esp add spawner`) - ESP for any blocks you choose.
- **LootAlert**: alerts for valuable dropped items. **PlayerLog**: remembers where players were (`.seen [name]`). **AutoWeapon**.
- `.config save|load|list <name>` presets. ClickGUI panel layout now persists across restarts.

## AutoTotem fix + more
**AutoTotem** no longer ghosts: totems in the hotbar are moved with the vanilla "swap hands" action (works while moving, no inventory click). Totems only in the inventory are pulled into the hotbar only while you stand still.
AutoArmor / AutoReplenish / AutoDrop now also only click while you stand still (ClickGUI module -> **Safe Inventory Clicks**).
New: **Trajectories** (pearl/potion/bow path preview), **Breadcrumbs** (your trail). Visuals can also outline projectiles.

## Another batch
- **Hub** now has **Players** (from PlayerLog: copy coords / waypoint / friend) and **Friends** tabs.
- **AutoCommand**: `.cmd add <sec> <command>` runs commands on a timer. **ChatAlert**: toast + ping for mentions / PMs / keywords (`.alert add <word>`).
- **TpsHud** (server TPS estimate). **TargetHud** now also shows the target's armor and held items.

## Batch
**HudTweaks** (hide scoreboard / boss bars / effect icons / titles / action bar / held-item name / XP level), **ChatTimestamps**, **JoinLeave** (player join/leave alerts),
`.goto <x> <z>` (auto-walk), AutoLeave can leave when you are low on totems.

## Batch
YawLock (snap yaw/pitch to fixed angles), StorageCount HUD (chests/barrels/shulkers/... around you), HuntAlerts can take a screenshot on every find.
