# SimpleMetin 1.1.1 — Complete Guide

**English** | [Polski](GUIDE-PL.md)

[← Back to README](../README.md)

A plugin by TremeQu for Metin crystals: crystals with custom HP, rewards, holograms, statistics, and drop boosts. Administrators can place crystals manually or configure automatic spawns within areas and at fixed points.

Requirements: **Java 21 and Paper 1.21.1** (the version used for builds and tests). PlaceholderAPI is optional. The plugin uses the Paper API; compatibility with other versions and forks requires separate testing.

## Installation and First Start

1. Run `mvn clean package` from the project root and copy `target/SimpleMetin-1.1.1.jar` into your server's `plugins/` directory.
2. Start the server. Configuration files will be created in `plugins/SimpleMetin/`.
3. The default language is **English**. To enable Polish, set `language: pl` in `config.yml` and run `/metin reload`.
4. As an administrator, run `/metin spawn metin_common`. A crystal will appear at your location with the ID `common-1`.
5. Players can attack it without additional permissions. Regular players can also use `/metin stats` to view their own statistics.

Example spawners are **disabled** by default. Configure an area or spawn points before enabling them.

## Combat and Crystal Lifecycle

How much HP an accepted hit removes is chosen per crystal type:

| `crystals.<type>` | Damage of one hit |
|---|---|
| `use-damage-per-hit: true` | `damage-per-hit` (default 1); player strength is ignored. |
| `use-damage-per-hit: false` | The player's strength stored in `players.yml` (`/ma`, default 10). |
| not set | `true` if the type has `damage-per-hit` (1.0.0 configs), otherwise player strength (1.1.0 configs). |

Damage is always at least 1. Standard weapon damage does not affect crystal damage. `/metin info` shows the damage mode of a crystal. A player's hit cooldown is shared across all crystals; the default is one second (`settings.hit-cooldown`).

Each hit rolls `hit-drops`, `hit-commands`, and `hit-pools`. The final hit also rolls `death-drops`, `death-commands`, and `death-pools`. Destruction rewards go to **the player who lands the final hit**, without sharing them among participants. If the inventory is full, excess items drop next to the crystal and a message is displayed.

Destruction triggers effects, updates statistics, and fires `MetinCrystalDestroyedEvent`. Damage statistics count only HP actually removed, excluding excess damage from the final hit.

| Origin / type | After destruction |
|---|---|
| Manual placement, `type: respawn` | A hologram shows the countdown; the crystal returns after `respawn-time` seconds. |
| Manual placement, `type: one-time` | The record and entity are permanently removed. |
| Created by a spawner, any type | Permanently removed; the spawner creates subsequent crystals. |

Vanilla damage, TNT, fire, and mob projectiles do not destroy crystals. `settings.projectile-hits: true` enables player projectile hits using the same strength and cooldown; the handled projectile is removed. This option is disabled by default.

Crystal and hologram entities are non-persistent (`setPersistent(false)`). When a chunk loads, the plugin restores them from its own data; unloading removes the entities while preserving HP. Leftover entities from older versions are cleaned up, including old ArmorStand holograms. Records with an unavailable world or unknown type remain in `data.yml` and appear as `PENDING` in the list. Loading the world or running `/metin reload` retries their restoration.

## Configuration, Languages and Updates

On startup and `/metin reload`, missing global options in `config.yml` and keys in the bundled `messages-en.yml` and `messages-pl.yml` files are added with comments. Existing values and comments are preserved. The updater does not overwrite files with invalid YAML syntax.

The `crystals` section belongs to the administrator: existing types, drops, and holograms are not replaced with JAR examples, and deleted types do not return. Add new settings inside your custom types manually; the code uses fallback values otherwise. Spawner definitions also belong to the administrator; the example `spawners.yml` is copied only if the file is missing. `items.yml` stores saved items and is not automatically populated with defaults.

When migrating from 1.0.0, the old `messages` section, boost BossBar titles, and voucher name and lore move from `config.yml` into the active language file. Custom text keeps its content; migration does not translate it.

```yaml
language: en  # en -> messages-en.yml, pl -> messages-pl.yml
```

You can create, for example, `messages-de.yml` and set `language: de`. Missing messages in custom languages use English defaults in memory. Automatic insertion of missing keys applies to the bundled EN/PL languages.

Supported formats include `&` codes, `&#RRGGBB` colors, and MiniMessage, such as `<gradient:gold:yellow>Metin</gradient>`. An empty string `""` disables a message; a list of strings creates multiple lines. Comments in the language files describe message placeholders. Crystal type names, holograms, and drop text remain with their definitions in `config.yml`.

`/metin reload` refreshes configuration, language, saved items, drop tables, holograms, BossBars, boosts, and spawners. A full-health crystal stays at full health when `max-hp` changes; a damaged crystal keeps its current HP, capped at the new maximum. Spawner timers are preserved, while pending location searches from the previous configuration are cancelled.

### Global Settings

| Option under `settings.*` | Default | Meaning |
|---|---|---|
| `save-interval` | 300 | Seconds between saves of crystals, statistics, and boosts. |
| `hologram-update-interval` | 20 | Ticks between hologram updates. |
| `actionbar-enabled` | true | HP and cooldown messages on the ActionBar. |
| `bossbar-enabled` | true | Crystal BossBars. |
| `bossbar-range` | 30.0 | BossBar range in blocks. |
| `hit-cooldown` | 1.0 | Seconds between hits; 0 disables the cooldown. |
| `leaderboard-update-interval` | 6000 | Ticks between leaderboard exports (6000 = 5 minutes). |
| `top-cache-seconds` | 5 | Statistics leaderboard cache duration; 0 recalculates each request. |
| `projectile-hits` | false | Hits from player projectiles. |
| `nearest-range` | 10.0 | Search range for the nearest crystal. |

Crystal types are defined under `crystals.<type>`: `display-name`, `type`, `max-hp`, `respawn-time`, `show-actionbar`, `show-bossbar`, `hologram`, and rewards. `death-effects` contains `particle`, `sound`, `volume`, and `pitch`. Legacy particle names, such as `EXPLOSION_HUGE`, have supported equivalents (`EXPLOSION_EMITTER`). Damage per hit is set with `use-damage-per-hit` and `damage-per-hit` (see [Combat and Crystal Lifecycle](#combat-and-crystal-lifecycle)).

## Holograms and BossBars

An entire hologram uses **one TextDisplay entity**, regardless of the number of lines. Text is sent again when HP or the countdown changes. Settings under `crystals.<type>.hologram`:

| Option | Meaning |
|---|---|
| `enabled` | Enables the hologram and respawn countdown. |
| `lines` | Lines with `%hp%`, `%max_hp%`, and `%id%` placeholders. |
| `height` | Height above the crystal, default 2.3 blocks. |
| `range` | View range passed to TextDisplay, such as 32 blocks; rendering also depends on the client. |
| `background` | `"#AARRGGBB"`, `"#RRGGBB"`, or `"default"`; `"#00000000"` is transparent. |
| `shadow` | Text shadow, default true. |
| `see-through` | Visibility through blocks, default false. |

A crystal BossBar displays its name and HP to nearby players. It is controlled by global settings and the type's `show-bossbar` option. The ActionBar displays HP and cooldown information, with a global switch and a per-type `show-actionbar` option. Countdown text uses the `respawn-countdown` message.

## Short IDs

`/metin spawn metin_common` creates `common-1`, followed by `common-2`, and so on. Available numbers may be reused. Custom IDs contain 1–32 characters: lowercase letters, digits, `_`, and `-`. The arguments `auto` and `nearest` are reserved in the spawn command.

```text
/metin spawn metin_common boss &cArena Boss
/metin spawn metin_common auto &eSpawn Metin
/metin info boss
/metin tp boss
/metin remove nearest
```

Legacy UUIDs are converted to short IDs when loaded, preserving position, HP, and respawn state. `/metin list` sorts numbers naturally. `nearest` searches within `settings.nearest-range`. Without an argument, `/metin info` also uses the nearest crystal, rather than the crosshair target.

**Syntax change from 1.0.0:** the third `spawn` argument now specifies the ID. To provide only a display name, put `auto` before it.

## Automatic Spawns

Shared settings in `config.yml`:

```yaml
auto-spawn:
  enabled: true
  max-alive-total: 10
  min-players-online: 1
```

The global limit counts only spawner-created crystals, including those in unloaded chunks; manually placed crystals are excluded. `max-alive-total: 0` removes the global limit. The per-spawner limit is `max-alive`; here, `0` blocks automatic spawns.

Attempts run every `interval` seconds, with ranges such as `"600-900"` also supported. When a limit is reached, the next attempt waits another interval. Too few online players or the global disable switch pauses attempts. Limits are checked again after chunk loading finishes, so concurrent location searches do not exceed them.

Example `spawners.yml`:

```yaml
spawners:
  forest:
    enabled: true
    mode: random
    world: world
    area:
      shape: circle
      center-x: 100
      center-z: 200
      radius: 150
      min-y: 60
      max-y: 160
    avoid-liquids: true
    min-distance: 20
    types:
      metin_common: 80
      metin_rare: 20
    interval: "600-900"
    max-alive: 3
    lifetime: 1800
    spawn-on-start: false
    announce:
      spawn: true
      despawn: true
      destroy: true
      warning: 60
      range: 0
      sound: ENTITY_ENDER_DRAGON_GROWL
```

`mode: random` selects locations in a circle or rectangle. For a rectangle, set `shape: rectangle` and `min-x`, `min-z`, `max-x`, `max-z`. Chunks load asynchronously; ground checks and entity creation run on the main thread.

`min-y`/`max-y` constrain the height of the highest surface block — they **do not search for caves below the Nether roof**. Locations require solid ground and free space above it. `types` defines weights for existing crystal types, and `min-distance` specifies spacing from other crystals.

`lifetime` counts seconds since creation. A crystal disappears after this time even if it has been damaged; `0` disables expiration. The deadline is stored in `data.yml`, so offline time also counts. Existing crystals continue to expire even when automatic spawning is disabled.

Use `mode: points` for fixed locations. Ensure these locations are safe yourself; this mode does not search for ground. Points are stored in the `points` list as `"world x y z"`:

```text
/metin spawner point add arena
/metin spawner point list arena
/metin spawner point remove arena 1
```

The command centers each point within the block. Occupied locations are skipped; the minimum spacing in points mode is 1.5 blocks even with `min-distance: 0`.

`announce.warning` announces an upcoming spawn attempt but does not guarantee a spawn if no space is available later. `announce.range: 0` sends announcements to everyone; a positive value limits them to the crystal's surroundings. Advance warnings go to everyone because the location is not yet known. Texts use `spawner-warning`, `spawner-spawned`, `spawner-despawned`, and `spawner-destroyed` in the message files.

`/metin spawner spawn <name>` forces an attempt regardless of timers, enabled state, or automatic limits. It still requires a valid type and a free location. After a restart, the attempt timer starts over; `spawn-on-start` enables an immediate first attempt.

## Drops, Pools and Custom Items

Each entry in `hit-drops` and `death-drops` is rolled independently. `hit-commands` and `death-commands` still support commands with their own chances. Quantities may be fixed or rolled from an inclusive range.

```yaml
death-drops:
  diamonds:
    item: DIAMOND
    amount: "1-3"
    chance: 25.0
    name: "&bMetin Diamond"
    lore: ["&7A reward from a crystal"]
    custom-model-data: 1001
  sword:
    item: DIAMOND_SWORD
    chance: 5.0
    enchantments:
      sharpness: 3
      unbreaking: 2
  money:
    command: "eco give %player% 100"
    chance: 50.0
```

`chance` is a percentage from 0 to 100. Boosts multiply chances, capped at 100%; they do not multiply item quantities.

`hit-pools` / `death-pools` select entries by relative weight:

```yaml
death-pools:
  bonus:
    chance: 50
    rolls: "1-2"
    unique: true
    entries:
      common:
        item: GOLD_INGOT
        amount: "2-4"
        weight: 9
      rare:
        custom-item: event_sword
        weight: 1
```

First, the pool's overall chance is rolled, followed by its number of selections, `rolls`. Entries are selected using `weight`, without using their individual `chance`. `unique: true` prevents repeated entries within a single pool evaluation; selection stops when all entries have been used. Boosts increase the chance of activating a pool without changing weights or the number of selections.

Hold an item in your main hand to save its NBT/components:

```text
/metin item save event_sword
/metin item give event_sword
/metin item give event_sword Steve 1
/metin item list
/metin item remove event_sword
/metin adddrop metin_rare death 25 1-3 event_sword
```

`item save` saves a copy of the template with an amount of 1 without taking the held item. `adddrop` saves the template and adds a `custom-item` entry to the configuration; it takes effect immediately. Syntax: `/metin adddrop <type> <hit|death> <chance> [amount] [key]`. Reusing an existing key replaces the template for all references to it. Keys contain 1–32 characters: `a-z`, `0-9`, `_`, and `-`.

`item give` gives at most one stack of the item. If the inventory is full, overflow is dropped next to the player; this does not increase drop statistics. Saving a custom item's data does not replace the plugin required for its special behavior.

## Boosts and Vouchers

Global boosts affect everyone; personal boosts affect one player. They have separate BossBars configured under `boosts.bossbar`; titles are stored in the message files.

```text
/metin boost global 2.0 600
/metin boost player 2.0 600 Steve
/metin givevoucher Steve 2.0 600
```

Durations are specified in seconds. Vouchers activate on right-click and are identified by their PersistentDataContainer, not their name. Material, glow, and custom model data are configured in `boosts.personal.voucher`; name and lore are in the message file. An active personal boost prevents consuming another voucher; an administrator's command replaces it and sends a notification.

| Settings | Global 2× + personal 2× |
|---|---|
| `boosts.personal.stacking-enabled: false` | 2×, the higher multiplier. |
| `stacking-enabled: true`, `boosts.global.stacking-mode: 1.0` | 3×: `1 + (global - 1) + (personal - 1)`; the default mode. |
| `stacking-enabled: true`, `boosts.global.stacking-mode: 2.0` | 4×: the product of both multipliers. |

Boosts are saved in `boosts.yml` and restored after a restart with their expiration deadlines preserved. Offline time also counts. Expiration messages work even with BossBars disabled.

## Commands and Permissions

`simplemetin.admin` grants access to all commands (OP by default). `simplemetin.stats` is available to everyone by default. Other permissions below default to OP. Help and TAB completion respect permissions.

| Command | Permission |
|---|---|
| `/metin stats` | `simplemetin.stats` |
| `/metin stats <player>` | `simplemetin.stats` and `simplemetin.stats.others` (your own name does not require `others`) |
| `/metin spawn <type> [id\|auto] [name]` | `simplemetin.spawn` |
| `/metin remove <id\|nearest>` | `simplemetin.remove` |
| `/metin list` | `simplemetin.list` |
| `/metin info [id\|nearest]` | `simplemetin.info` |
| `/metin tp <id>` | `simplemetin.tp` |
| `/metin respawn <id\|nearest>` | `simplemetin.respawn` |
| `/metin reload` | `simplemetin.reload` |
| `/metin boost global <multiplier> <seconds>` | `simplemetin.boost` |
| `/metin boost player <multiplier> <seconds> <player>` | `simplemetin.boost` |
| `/metin givevoucher <player> <multiplier> <seconds>` | `simplemetin.voucher` |
| `/metin updateleaderboard` (alias `refreshleaderboard`) | `simplemetin.leaderboard` |
| `/metin spawner list\|spawn\|point ...` | `simplemetin.spawner` |
| `/metin item save\|give\|remove\|list ...` | `simplemetin.items` |
| `/metin adddrop <type> <hit\|death> <chance> [amount] [key]` | `simplemetin.items` |
| `/ma add <player> <value>` | `simplemetin.strength` |
| `/ma set <player> <value>` | `simplemetin.strength` |
| `/ma get <player>` | `simplemetin.strength` |
| `/ma reset <player>` | `simplemetin.strength` |

`/ma` is an alias for `/metinadmin`. It changes strength, not combat statistics. `set` requires a minimum of 1; `add` may be negative, but the result cannot fall below 1. `reset` restores 10. Offline players known to the server are also supported. Commands that look up players require their full names.

## Statistics and Placeholders

`/metin stats` displays destroyed crystals, damage, items, money, strength, leaderboard positions, and destructions by type. Players outside the top 100 have no displayed leaderboard position.

Money is estimated from `eco give <player> <integer amount>` when Bukkit reports successful command handling. The plugin does not verify balances or integrate with Vault. Fractional amounts and other economy commands are not counted. The example `eco give` commands require a separate economy plugin.

PlaceholderAPI registers two identifiers:

| Placeholder | Meaning |
|---|---|
| `%metin_crystals_destroyed%` | Number of destroyed crystals. |
| `%metin_total_damage%` | Damage actually dealt. |
| `%metin_items_received%` | Reward items, including overflow dropped when the inventory is full. |
| `%metin_money_earned%` | Amounts recognized from economy commands. |
| `%metin_type_<type>%` | Destructions of a type, e.g. `%metin_type_metin_common%`. |
| `%metin_boost_multiplier%` | Effective multiplier. |
| `%metin_boost_active%` | `true` or `false`. |
| `%metin_personal_boost_multiplier%` | Personal multiplier. |
| `%metin_personal_boost_time%` | Remaining personal boost time. |
| `%metin_global_boost_multiplier%` | Global multiplier. |
| `%metin_global_boost_time%` | Remaining global boost time. |
| `%metin_<category>_top_<N>%` | The Nth player's value; categories: `destroyed`, `damage`, `items`, `money`; positions 1–100. |
| `%metin_<category>_top_<N>_name%` | Player name at that position. |
| `%simplemetin_damage%` | Player strength. |
| `%simplemetin_top_name_<N>%` | Player name in the strength top 10. |
| `%simplemetin_top_value_<N>%` | Strength value in the top 10. |

Leaderboard example: `&61. &f%metin_destroyed_top_1_name% &7- &e%metin_destroyed_top_1%`.

Leaderboard placeholders also work without player context. Statistics rankings use `settings.top-cache-seconds` (5 seconds). The strength leaderboard is built from in-memory data on startup and every 5 minutes. `leaderboards.yml` and `statistics.yml` exports refresh every `settings.leaderboard-update-interval` ticks and through the command.

## Files and Data Storage

| File in `plugins/SimpleMetin/` | Contents |
|---|---|
| `config.yml` | Global settings, types, drops, holograms, and boosts. |
| `messages-en.yml`, `messages-pl.yml` | Interface text, boost titles, and voucher text. |
| `spawners.yml` | Areas, points, intervals, limits, and announcements. |
| `items.yml` | Base64 item templates with readable descriptions. |
| `data.yml` | ID, position, HP, destroyed state, respawn, spawner, and expiration deadline. |
| `players.yml` | Player strength. |
| `stats.yml` | Combat and reward statistics. |
| `boosts.yml` | Active boosts and expiration deadlines. |
| `leaderboards.yml` | Top 100 exports for statistics categories. |
| `statistics.yml` | Readable export of all statistics. |

`data.yml`, `stats.yml`, and `boosts.yml` are saved every `settings.save-interval` seconds (300) and on shutdown. Strength is saved when changed and when a player leaves. An abrupt process termination may lose changes made since the last save. Data stays in RAM; autosaving does not remove it from memory or reload it from disk.

`/metin reload` is for configuration. Edit state files manually while the server is stopped so in-memory data does not overwrite your changes.

## Architecture and Integrations

- `CrystalManager` handles combat, entities, drops, and respawns; `HologramManager` manages TextDisplay entities, and `DataHandler` persists state.
- `SpawnerManager` handles areas, points, limits, expiration, and announcements.
- `DropTable` and `DropEntry` roll rewards; `CustomItemManager` stores item templates.
- `MessageManager`, `TextUtils`, and `ConfigUpdater` handle languages, formatting, and configuration updates.
- `StatsManager` tracks statistics, `PlayerStatsManager` manages strength, and `BoostManager` manages boosts.
- `CrystalListener` handles damage and chunk lifecycle events; `BoostVoucherListener` handles vouchers.

Entity tags are `simplemetin`, `metin_<configId>` (e.g. `metin_metin_common`), and `simplemetin_hologram` for TextDisplay entities. `MetinCrystalDestroyedEvent` exposes the crystal, player, and rolled rewards; it does not distribute drops among players.

## Verification

Run tests with `mvn test` and a full build with `mvn clean package` from the project root. JUnit reports are in `target/surefire-reports/`. A separate E2E suite on a real Paper 1.21.1 server checks combat, drops, boosts, permissions, languages, migration, TextDisplay, spawners, reloads, restarts, and crash recovery. Results and suite locations are documented in [TEST_REPORT.md (Polish)](../TEST_REPORT.md).

Other older documentation files in the repository may refer to 1.0.0; this guide describes version 1.1.1.
