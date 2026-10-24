# Dungeon in a Minute

A move-only, auto-attack roguelike for Android. Each run starts with 60 seconds on
the clock, and the clock is your health: hits cost time, and enemies drop time
shards you walk over to win it back.

Built with **Godot 4.3** (2D, GDScript, Compatibility renderer).

## Status

Build steps 1–4 from the handoff are in, so the core loop is playable:

1. **Movement**: floating joystick (appears where your thumb lands, follows if you drag past the edge), camera, walled arena.
2. **Enemies**: chasers spawn just off screen in growing waves and swarm you.
3. **Auto-weapon**: a projectile fires at the nearest enemy in range.
4. **Time system**: 60s countdown, hits cost 5s (with a short invulnerability and knockback), kills drop shards worth +1.5s that vanish after 8s. At 0 it's game over; tap to retry.

Next up is step 5, level-up cards. `main.gd` already emits `wave_cleared(wave)` for it.

## Run it

1. Install [Godot 4.3+](https://godotengine.org/download) (standard build, not .NET).
2. In the Project Manager, **Import** → pick `dungeon-in-a-minute/project.godot`.
3. Press **F5**.

On desktop, click and drag to use the joystick (mouse emulates touch), or use WASD / arrow keys.

## Tuning

Every starting number (clock length, seconds per hit, seconds per shard, speeds,
wave sizes) is in [`scripts/balance.gd`](scripts/balance.gd).

## Layout

| Path | What |
|---|---|
| `scenes/main.tscn` + `scripts/main.gd` | Run controller: clock, waves, kills, game over |
| `scripts/player.gd` | Movement, auto-fire, taking hits |
| `scripts/joystick.gd` | Floating joystick |
| `scripts/chaser.gd` | Chaser enemy |
| `scripts/projectile.gd` | Auto-weapon bullet |
| `scripts/time_shard.gd` | Time pickup with magnet and expiry |
| `scripts/arena.gd` | Floor and walls |
| `scripts/hud.gd` | Clock, counters, wave banner, game-over screen |
| `scripts/balance.gd` | All tuning numbers (autoload `Balance`) |

Everything is drawn with simple shapes for now, so there are no art assets yet.

Physics layers: 1 walls, 2 player, 3 enemies, 4 player projectiles.

## Android APK

**Project → Export → Add… → Android**. This needs the Android SDK, a JDK 17 and a
debug keystore set up in **Editor Settings → Export → Android**
([Godot docs](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html)).
