<p align="center">
  <img src="https://i.imgur.com/MHK0Eqw.png" alt="SimpleMetin - Advanced Metin Crystal System" width="820">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.1.1-blue.svg" alt="Version 1.1.1">
  <img src="https://img.shields.io/badge/paper-1.21.1-green.svg" alt="Paper 1.21.1">
  <img src="https://img.shields.io/badge/java-21-orange.svg" alt="Java 21">
</p>

<p align="center"><b>English</b> | <a href="README-PL.md">Polski</a></p>

**Metin crystals, rewards, and event areas for your Minecraft server.**

**Version:** 1.1.1 · **Author:** TremeQu · **Tested platform:** Paper 1.21.1 · **Java:** 21

## 📢 About SimpleMetin

<img src="https://www.spigotmc.org/attachments/crystal-1-png.925536/" alt="SimpleMetin crystal" width="140" align="right">

SimpleMetin brings Metin-style crystal combat to Minecraft. Players attack crystals with custom HP, earn rewards, and build their statistics, while administrators control crystal difficulty, drops, and spawn locations.

Use it for arenas, event areas, or an additional activity on a survival map. It combines manually placed crystals, automatic spawns, holograms, boosts, and leaderboards in one system.

**Found a bug or have an idea?** Open an [issue](https://github.com/tremeq/SimpleMetin/issues) with your server version, configuration, and steps to reproduce the problem.

## 📦 Requirements & Dependencies

- **Java 21** — required.
- **Paper** — the plugin uses the Paper API; builds and tests were run on **1.21.1**.
- **PlaceholderAPI** — optional, exposes statistics and leaderboards to other plugins.
- **An economy plugin** — needed only for rewards that use commands such as `eco give`.

SimpleMetin works without PlaceholderAPI or an economy plugin. No separate hologram plugin is required.

---

## 💎 Core Features

<img src="https://i.imgur.com/qS6Sz1S.png" alt="Features" width="100%">

- **Crystals with custom HP** — configurable types, names, health, and destruction effects.
- **Two crystal modes** — one-time crystals and crystals that respawn after a configurable delay.
- **Damage per hit or player strength** — each crystal type either takes a fixed `damage-per-hit` or the player's strength (`/ma`, persistent, with a leaderboard).
- **Hit and destruction rewards** — items and console commands with individual drop chances.
- **Advanced drops** — random quantities, weighted pools, enchantments, custom model data, and items saved from your hand with their NBT.
- **Automatic spawns** — random locations within areas or fixed points, weighted crystal types, limits, and crystal lifetimes.
- **Event announcements** — advance spawn warnings and messages for spawning, destruction, and despawning.
- **TextDisplay holograms** — one entity per hologram, custom colors, backgrounds, view range, and respawn countdowns.
- **BossBars and ActionBar** — current HP, hit cooldowns, and active boost information.
- **Drop boosts** — global and personal chance multipliers, configurable stacking, and vouchers.
- **Statistics and leaderboards** — destroyed crystals, damage dealt, items received, and amounts recognized from economy commands.
- **Convenient administration** — short IDs such as `common-1`, crystal details, teleportation, and nearest-crystal selection.
- **EN/PL messages** — language selection, custom text, and support for `&` colors, HEX, and MiniMessage.
- **Configuration updates** — missing global options and EN/PL messages are added while preserving your settings.
- **Persistent data** — crystal state, strength, statistics, and active boosts are restored after a restart.

Destruction rewards go to the player who lands the final hit. Boosts increase drop chances, and items that do not fit in a full inventory are dropped on the ground.

### 🖼️ In-Game Preview

<table>
  <tr>
    <td align="center"><img src="https://www.spigotmc.org/attachments/upload_2025-10-19_20-36-27-png.925551/" alt="Common Metin with its hologram" width="280"><br><sub>Common Metin — hologram with HP</sub></td>
    <td align="center"><img src="https://www.spigotmc.org/attachments/upload_2025-10-19_20-37-35-png.925552/" alt="Rare Metin" width="280"><br><sub>Rare Metin — a tougher one-time crystal</sub></td>
  </tr>
  <tr>
    <td colspan="2" align="center"><img src="https://www.spigotmc.org/attachments/upload_2025-10-19_20-1-35-png.925537/" alt="Crystal BossBar" width="560"><br><sub>BossBar with the crystal's current HP</sub></td>
  </tr>
</table>

---

## ⚡ Quick Start

1. Build the plugin with `mvn clean package` and place `target/SimpleMetin-1.1.1.jar` in your server's `plugins/` directory.
2. Start the server to generate the configuration files.
3. Configure crystal types and rewards in `plugins/SimpleMetin/config.yml`.
4. Run `/metin spawn metin_common` to place your first crystal.
5. Check your statistics with `/metin stats`.

The default language is **English**. To enable Polish, set `language: pl` and run `/metin reload`.

The example spawners in `spawners.yml` are disabled. Configure an area or add spawn points before enabling them.

---

## ⌨️ Commands

<img src="https://i.imgur.com/A2I8T4d.png" alt="Commands" width="100%">

### Player Commands

- `/metin stats` — view your statistics, strength, and leaderboard positions.
- `/metin` — display help containing the commands available to you.

### Crystals & Configuration

- `/metin spawn <type> [id|auto] [name]` — create a crystal at your location.
- `/metin remove <id|nearest>` — remove a specific crystal or the nearest one.
- `/metin list` — list crystals and their current state.
- `/metin info [id|nearest]` — display crystal details.
- `/metin tp <id>` — teleport next to a crystal.
- `/metin respawn <id|nearest>` — immediately respawn a destroyed crystal.
- `/metin reload` — reload configuration and message files.
- `/metin stats <player>` — view another player's statistics.
- `/metin updateleaderboard` — refresh leaderboard exports.

### Boosts & Rewards

- `/metin boost global <multiplier> <seconds>` — activate a server-wide boost.
- `/metin boost player <multiplier> <seconds> <player>` — activate a personal boost.
- `/metin givevoucher <player> <multiplier> <seconds>` — give a voucher activated by right-clicking.
- `/metin item save <key>` — save the item in your main hand as a drop template.
- `/metin item give <key> [player] [amount]` — give a saved item.
- `/metin item list` and `/metin item remove <key>` — manage saved items.
- `/metin adddrop <type> <hit|death> <chance> [amount] [key]` — add the held item to a crystal type's rewards.

### Spawners & Player Strength

- `/metin spawner list` — display spawner status.
- `/metin spawner spawn <spawner>` — force a spawn attempt, bypassing automatic spawn limits.
- `/metin spawner point add <spawner>` — save your current location as a spawn point.
- `/metin spawner point list <spawner>` — list spawn points.
- `/metin spawner point remove <spawner> <number>` — remove a spawn point.
- `/ma add <player> <value>` and `/ma set <player> <value>` — change player strength.
- `/ma get <player>` and `/ma reset <player>` — check strength or restore the default value of 10.

**Permissions:** viewing your own statistics is available to everyone by default. `simplemetin.admin` grants full access, and individual features have separate permissions. [Full permission list →](docs/GUIDE.md#commands-and-permissions)

---

## 🧩 Placeholders

### PlaceholderAPI

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). SimpleMetin registers two expansions: **`metin`** (statistics, boosts, leaderboards) and **`simplemetin`** (player strength). Numbers always use a dot (`2.5`), regardless of the server locale.

#### Player Statistics

| Placeholder | Returns | Without data |
|---|---|---|
| `%metin_crystals_destroyed%` | Crystals destroyed by the player. | `0` |
| `%metin_total_damage%` | Damage actually dealt (overkill is not counted). | `0` |
| `%metin_items_received%` | Items received from drops, including items dropped on the ground when the inventory is full. | `0` |
| `%metin_money_earned%` | Money from reward commands `eco give <player> <amount>` that ran successfully. | `0` |
| `%metin_type_<type>%` | Crystals of one type destroyed by the player, e.g. `%metin_type_metin_common%`, `%metin_type_metin_rare%`. | `0` |

#### Boosts

| Placeholder | Returns | Without data |
|---|---|---|
| `%metin_boost_multiplier%` | Effective drop multiplier (global + personal, according to `stacking-mode`), e.g. `3.0`. | `1.0` (also for offline players) |
| `%metin_boost_active%` | `true` when the effective multiplier is above `1.0`. | `false` |
| `%metin_personal_boost_multiplier%` | Multiplier of the player's personal boost. | `1.0` |
| `%metin_personal_boost_time%` | Remaining personal boost time: `45s`, `2m 5s`, `1h 2m 3s`. | `0s` |
| `%metin_global_boost_multiplier%` | Multiplier of the global boost. | `1.0` |
| `%metin_global_boost_time%` | Remaining global boost time: `45s`, `2m 5s`, `1h 2m 3s`. | `0s` |

#### Leaderboards (positions 1–100)

| Placeholder | Returns | Empty position |
|---|---|---|
| `%metin_destroyed_top_<N>%` | Crystals destroyed by the player at position N. | `0` |
| `%metin_destroyed_top_<N>_name%` | Name of the player at position N (crystals destroyed). | `-` |
| `%metin_damage_top_<N>%` | Damage dealt by the player at position N. | `0` |
| `%metin_damage_top_<N>_name%` | Name of the player at position N (damage dealt). | `-` |
| `%metin_items_top_<N>%` | Items received by the player at position N. | `0` |
| `%metin_items_top_<N>_name%` | Name of the player at position N (items received). | `-` |
| `%metin_money_top_<N>%` | Money earned by the player at position N. | `0` |
| `%metin_money_top_<N>_name%` | Name of the player at position N (money earned). | `-` |

`<N>` is a number from `1` to `100`, e.g. `%metin_damage_top_1_name%`. Leaderboard placeholders also work without a player (global holograms, tab lists). Rankings are cached for `settings.top-cache-seconds` (default 5 s). Positions outside the list, `0` or negative numbers return `0` / `-`; a player without a known name shows `Unknown`.

#### Player Strength

| Placeholder | Returns | Without data |
|---|---|---|
| `%simplemetin_damage%` | The player's strength (damage per hit, managed with `/ma`). | `10` (default strength) |
| `%simplemetin_top_name_<N>%` | Name of the player at position N of the strength top 10. | `-` |
| `%simplemetin_top_value_<N>%` | Strength of the player at position N of the strength top 10. | `0` |

`<N>` is a number from `1` to `10`. The strength top 10 is rebuilt on startup and every 5 minutes.

**Example** (scoreboard / hologram plugin):

```
&6&lMETIN TOP
&e1. &f%metin_destroyed_top_1_name% &7- &e%metin_destroyed_top_1%
&e2. &f%metin_destroyed_top_2_name% &7- &e%metin_destroyed_top_2%
&e3. &f%metin_destroyed_top_3_name% &7- &e%metin_destroyed_top_3%
&7Your crystals: &e%metin_crystals_destroyed% &7| Strength: &e%simplemetin_damage%
&7Boost: &e%metin_boost_multiplier%x &7(%metin_global_boost_time%)
```

### Internal Placeholders

These work inside SimpleMetin's own files and do not need PlaceholderAPI. PlaceholderAPI placeholders are **not** parsed inside SimpleMetin messages or holograms.

| Where | Placeholders |
|---|---|
| Hologram lines — `crystals.<type>.hologram.lines` in `config.yml` | `%hp%` current HP, `%max_hp%` maximum HP, `%id%` crystal ID |
| Reward commands — `hit-commands`, `death-commands`, `command:` entries in drops and pools | `%player%` the rewarded player's name |

### Message Placeholders

Available in `messages-en.yml` and `messages-pl.yml`. Every message also accepts `&` colors, HEX and MiniMessage; `prefix` is added to most chat messages.

| Message key(s) | Placeholders |
|---|---|
| `player-not-found`, `player-never-joined`, `stats-none` | `%player%` |
| `invalid-number` | `%value%` |
| `usage` | `%usage%` |
| `reload-pending-loaded`, `list-header`, `items-list-header` | `%count%` |
| `spawned` | `%id%`, `%type%` |
| `spawn-unknown-type` | `%type%` |
| `spawn-invalid-id`, `spawn-id-taken`, `removed`, `not-found`, `respawned`, `not-destroyed`, `teleported` | `%id%` |
| `no-crystal-nearby` | `%range%` |
| `list-entry` | `%id%`, `%status%`, `%type%`, `%hp%`, `%max_hp%`, `%world%`, `%x%`, `%y%`, `%z%`, `%spawner%` |
| `list-entry-pending` | `%id%`, `%status%` |
| `info` | `%id%`, `%type%`, `%display_name%`, `%status%`, `%hp%`, `%max_hp%`, `%damage%`, `%world%`, `%x%`, `%y%`, `%z%`, `%respawn%`, `%spawner%`, `%expires%` |
| `damage-fixed` (`%damage%` in `info`) | `%damage%` |
| `crystal-destroyed` | `%display_name%`, `%id%`, `%player%` |
| `crystal-hit` (ActionBar) | `%hp%`, `%max_hp%`, `%damage%`, `%display_name%` |
| `respawn-countdown` (hologram), `hit-cooldown` (ActionBar) | `%time%` |
| `bossbar-crystal` | `%display_name%`, `%hp%`, `%max_hp%`, `%id%` |
| `stats` | `%player%`, `%destroyed%`, `%damage%`, `%items%`, `%money%`, `%strength%`, `%rank_destroyed%`, `%rank_damage%`, `%rank_items%`, `%rank_money%` |
| `stats-type-line` | `%type%`, `%count%` |
| `boost-activated`, `global-boost-activated` | `%multiplier%`, `%duration%` |
| `boost-player-activated`, `boost-player-replaced` | `%multiplier%`, `%player%`, `%duration%` |
| `bossbar-boost-personal`, `bossbar-boost-global` | `%multiplier%`, `%time%` |
| `voucher-name`, `voucher-lore` | `%multiplier%`, `%duration%` |
| `voucher-given` | `%player%`, `%multiplier%`, `%duration%` |
| `strength-added` | `%value%`, `%player%`, `%strength%` |
| `strength-added-capped` | `%value%`, `%player%`, `%strength%`, `%min%` |
| `strength-set`, `strength-get`, `strength-reset` | `%player%`, `%strength%` |
| `strength-too-low` | `%min%` |
| `strength-help` | `%default%` |
| `spawner-warning` | `%spawner%`, `%display_name%`, `%type%`, `%time%` |
| `spawner-spawned`, `spawner-despawned` | `%spawner%`, `%display_name%`, `%type%`, `%id%`, `%world%`, `%x%`, `%y%`, `%z%` |
| `spawner-destroyed` | `%spawner%`, `%display_name%`, `%type%`, `%id%`, `%world%`, `%x%`, `%y%`, `%z%`, `%player%` |
| `spawner-list-entry` | `%spawner%`, `%state%`, `%mode%`, `%alive%`, `%max%`, `%next%` |
| `spawner-not-found`, `spawner-force-searching`, `spawner-force-failed`, `spawner-point-header`, `spawner-point-none` | `%spawner%` |
| `spawner-forced` | `%id%`, `%spawner%` |
| `spawner-point-added`, `spawner-point-removed`, `spawner-point-invalid` | `%index%`, `%spawner%` |
| `spawner-point-entry` | `%index%`, `%world%`, `%x%`, `%y%`, `%z%` |
| `item-saved`, `item-not-found`, `item-removed` | `%key%` |
| `item-given` | `%amount%`, `%key%`, `%player%` |
| `items-list-entry` | `%key%`, `%material%` |
| `drop-added` | `%key%`, `%type%`, `%section%`, `%chance%`, `%amount%` |

Messages without placeholders: `prefix`, `no-permission`, `player-only`, `reloaded`, `list-empty`, `status-active`, `status-destroyed`, `status-pending`, `none`, `damage-strength`, `inventory-full`, `stats-types-header`, `boost-expired`, `global-boost-expired`, `boost-already-active`, `boost-invalid-type`, `voucher-received`, `leaderboard-updating`, `leaderboard-updated`, `spawner-list-header`, `spawner-state-enabled`, `spawner-state-disabled`, `item-hand-empty`, `items-list-empty`, `drop-invalid-section` and the `help-*` lines.

---

## 🔧 Recent Updates & Bug Fixes

### Version 1.1.1

- ✅ `damage-per-hit` is back: every crystal type chooses its damage with `use-damage-per-hit: true` (fixed `damage-per-hit`) or `false` (player strength from `/ma`).
- ✅ Configs from 1.0.0 (with `damage-per-hit`) and 1.1.0 (without it) keep working as before when `use-damage-per-hit` is not set.
- ✅ `/metin info` shows how a crystal takes damage (`%damage%` in the `info` message).

### Version 1.1.0

**New Features:**

- ✅ Automatic addition of missing global settings and messages.
- ✅ TextDisplay holograms with configurable appearance and view range.
- ✅ Separate `messages-en.yml` and `messages-pl.yml` files.
- ✅ Player-accessible statistics and separate administrative permissions.
- ✅ Short IDs, custom names, `info`, `tp`, and `nearest` support.
- ✅ Random and fixed-point spawners with limits, lifetimes, and announcements.
- ✅ Drop quantity ranges, weighted pools, and custom items saved from your hand.

**Bug Fixes:**

- ✅ Spawn limits are checked again after chunks finish loading.
- ✅ Pending spawns are cancelled when the plugin reloads or shuts down.
- ✅ The configuration updater no longer overwrites malformed YAML.
- ✅ `/metin item give` preserves overflow items when the inventory is full.
- ✅ `/metin adddrop` validates drop chances.

**Migration:** existing UUIDs are converted to short IDs, and legacy messages are moved to the active language file. The third argument of `/metin spawn` now specifies the ID — use `auto` before a custom display name.

---

## 📚 Documentation

The guide is available in [English](docs/GUIDE.md) and [Polish](docs/GUIDE-PL.md). The test report is currently available in **Polish**.

- [📖 Complete Guide](docs/GUIDE.md) — detailed plugin behavior and configuration.
- [⚙️ Configuration & Languages](docs/GUIDE.md#configuration-languages-and-updates) — file updates, translations, and formatting.
- [💎 Holograms & BossBars](docs/GUIDE.md#holograms-and-bossbars) — appearance, range, and HP placeholders.
- [🌍 Automatic Spawns](docs/GUIDE.md#automatic-spawns) — areas, points, limits, and announcements.
- [🎁 Drops & Custom Items](docs/GUIDE.md#drops-pools-and-custom-items) — chances, weights, and reward examples.
- [🚀 Boosts & Vouchers](docs/GUIDE.md#boosts-and-vouchers) — multipliers, stacking, and activation.
- [🔑 Commands & Permissions](docs/GUIDE.md#commands-and-permissions) — the full permission list.
- [📊 Statistics & Placeholders](docs/GUIDE.md#statistics-and-placeholders) — player values and leaderboards.
- [💾 Data Storage](docs/GUIDE.md#files-and-data-storage) — plugin files, autosaving, and restarts.
- [🧪 Test Report](TEST_REPORT.md) — verification coverage for version 1.1.1.

Example files: [config.yml](src/main/resources/config.yml) · [spawners.yml](src/main/resources/spawners.yml) · [messages-en.yml](src/main/resources/messages-en.yml) · [messages-pl.yml](src/main/resources/messages-pl.yml)

---

## 🔌 Integrations

- **PlaceholderAPI** — `%metin_...%` and `%simplemetin_...%` placeholders for scoreboards, holograms, and other displays provided by external plugins.
- **Console commands** — rewards through economy, key, rank, and other plugins, with `%player%` substitution.
- **Custom items** — preserve item NBT and components; special item behavior still requires the original plugin.
- **API and entity tags** — the `MetinCrystalDestroyedEvent` event and tags for identifying crystals.

---

**Verified on Paper 1.21.1:** 126 unit tests and 128 E2E checks passed. Environment details and verification scope are available in the [test report (Polish)](TEST_REPORT.md).
