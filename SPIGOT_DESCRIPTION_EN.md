# SimpleMetin - Advanced Metin Crystal System

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.1+-green.svg)
![Java](https://img.shields.io/badge/java-21-orange.svg)

---

## 📖 About the Plugin

**SimpleMetin** is an innovative plugin that brings a unique crystal mining system to your Minecraft server, inspired by the popular MMORPG game - Metin2. Players can destroy powerful Metin crystals scattered throughout the world, earning valuable rewards, items, and money!

The plugin was designed with RPG, PvE, and survival servers in mind, offering players additional activity and the opportunity to earn valuable rewards. Thanks to the advanced boost, ranking, and statistics system, players will compete for positions on the leaderboard!

---

## ✨ Main Features

### 🔮 Advanced Crystal System

The plugin allows you to create two types of crystals:

- **Respawn Crystals** - After destruction, they automatically respawn after a set time, perfect for farming and continuous gameplay
- **One-Time Crystals** - Single-use crystals that disappear forever after destruction, great for events and special rewards

**Crystal Features:**
- Configurable HP (crystal health)
- Adjustable damage per hit (how much HP one hit removes)
- Configurable respawn time for Respawn-type crystals
- Ability to assign custom names during spawn
- Visual holograms displaying:
  - Crystal name
  - Current HP status
  - Custom messages and information
- Dynamic Boss Bars showing:
  - Crystal destruction progress
  - Visualization of remaining HP
  - Automatic show/hide based on player distance
- Display range system (configurable)
- Visual effects on destruction (particles)
- Sound effects on destruction (configurable)

**Cooldown System:**
- Configurable cooldown between hits (prevents spamming)
- ActionBar messages informing about wait time
- Completely disableable (set 0.0 to disable)

### 💎 Advanced Drop System

The plugin offers one of the most advanced drop systems:

**Hit-Drops (Rewards per hit):**
- Items drop with each hit on the crystal
- Configurable percentage chances (0-100%)
- Chance multiplier through boost system
- Custom item names
- Multi-line descriptions (lore)
- Automatic addition to inventory or dropping on ground
- Message when inventory is full

**Death-Drops (Rewards for destruction):**
- Special rewards for complete crystal destruction
- Higher chances and better items than hit-drops
- All features as in hit-drops
- Perfect for rare and valuable rewards

**Hit-Commands and Death-Commands:**
- Execute any commands as rewards
- Perfect for server economy integration (Vault, EssentialsX)
- Support for %player% variable
- Configurable execution chances
- Automatic tracking of earned money in statistics
- Ability to broadcast messages, teleport, grant permissions, etc.

**Command Examples:**
```yaml
hit-commands:
  cmd1:
    command: "eco give %player% 10"
    chance: 20.0
death-commands:
  cmd1:
    command: "eco give %player% 500"
    chance: 100.0
  cmd2:
    command: "broadcast &6%player% destroyed a Rare Metin!"
    chance: 100.0
```

### ⚡ Comprehensive Boost System

**Personal Boosts:**
- Boosts for individual players
- Activation via vouchers (physical items)
- Activation by admin with command
- Configurable multiplier (1.0 = no boost, 2.0 = 2x chances)
- Configurable duration (in seconds)
- Cannot activate when player already has active boost
- Activation and expiration messages

**Global Boosts:**
- Boosts for entire server
- Activation only by admin
- Broadcast to all players on activation
- Perfect for Happy Hours, server events
- Works simultaneously with personal boosts

**Voucher System:**
- Physical items (default PAPER)
- Configurable appearance:
  - Material
  - Name (colored)
  - Lore (multi-line description)
  - Enchant Glow (glowing effect)
  - Custom Model Data (for resource packs)
- NBT/PDC data ensuring security
- Cannot be forged or duplicated
- Right-click = activation

**Boost Boss Bars:**
- Separate Boss Bar for personal boost
- Separate Boss Bar for global boost
- Display:
  - Multiplier (e.g., 2.0x)
  - Remaining time (format: 1h 30m 45s)
  - Progress bar visualizing time
- Configurable colors and styles
- Automatic updates every second
- Option to completely disable

**Boost Stacking:**
- Ability to combine personal + global
- Two stacking modes:
  - Additive (1.0 = addition)
  - Multiplicative (2.0 = multiplication)
- Full control in configuration

### 📊 Advanced Statistics and Ranking System

**Tracked Player Statistics:**
- **Crystals Destroyed** - Total number of destroyed crystals
- **Total Damage** - Sum of all damage dealt
- **Items Received** - Number of items received
- **Money Earned** - Money earned (from commands)
- **Per-Type Stats** - Statistics for each crystal type separately
- **Last Seen** - Date of last activity

**TOP 100 Ranking System:**
- Automatic generation of TOP 100 for each category:
  - TOP destroyed crystals
  - TOP damage dealt
  - TOP items received
  - TOP money earned
- Export to `leaderboards.yml` file
- Automatic update every 5 minutes (configurable)
- Manual update with `/metin updateleaderboard` command
- Format friendly for other plugins and websites

**Data Export:**
- `stats.yml` - Raw data of all players (UUID-based)
- `leaderboards.yml` - TOP 100 rankings for each category
- `statistics.yml` - Readable statistics sorted alphabetically by nicknames
- All files automatically updated
- Data preserved between restarts

**Auto-Save System:**
- Automatic save every 5 minutes (configurable)
- Save on server shutdown
- Asynchronous operations (no lag)
- Data backup with each save

### 🎨 PlaceholderAPI Integration

**Over 40+ placeholders** ready to use in:
- Scoreboard (Featherboard, AnimatedScoreboard)
- TAB List (TAB, NametagEdit)
- Holograms (HolographicDisplays, DecentHolograms)
- Chat (DeluxeChat, ChatControl)
- Titles, ActionBar, Boss Bars

**Placeholder Categories:**

**Player Statistics:**
```
%metin_crystals_destroyed% - Destroyed crystals
%metin_total_damage% - Damage dealt
%metin_items_received% - Items received
%metin_money_earned% - Money earned
%metin_type_<crystal_id>% - Statistics for specific type
```

**Boost System:**
```
%metin_boost_multiplier% - Total multiplier (personal + global)
%metin_boost_active% - Is active? (true/false)
%metin_personal_boost_multiplier% - Personal multiplier
%metin_personal_boost_time% - Personal remaining time
%metin_global_boost_multiplier% - Global multiplier
%metin_global_boost_time% - Global remaining time
```

**TOP Rankings (1-100):**
```
%metin_destroyed_top_1_name% - TOP 1 nick (destroyed)
%metin_destroyed_top_1% - TOP 1 value (destroyed)
%metin_damage_top_1_name% - TOP 1 nick (damage)
%metin_damage_top_1% - TOP 1 value (damage)
%metin_items_top_1_name% - TOP 1 nick (items)
%metin_items_top_1% - TOP 1 value (items)
%metin_money_top_1_name% - TOP 1 nick (money)
%metin_money_top_1% - TOP 1 value (money)
```

*Replace "1" with any number from 1 to 100 to get subsequent places!*

**Full documentation** of placeholders available in `WSZYSTKIE_PLACEHOLDERY.md` file in the main plugin folder.

---

## 🎮 Commands

All commands require `simplemetin.admin` permission (default OP).

| Command | Description | Example |
|---------|-------------|---------|
| `/metin spawn <type> [name]` | Creates a crystal at player's location | `/metin spawn metin_rare Legendary Metin` |
| `/metin remove <id>` | Removes a crystal with given ID | `/metin remove 550e8400-e29b...` |
| `/metin list` | Displays list of all active crystals | `/metin list` |
| `/metin respawn <id>` | Manually respawns a destroyed crystal | `/metin respawn 550e8400...` |
| `/metin reload` | Reloads plugin configuration | `/metin reload` |
| `/metin stats [player]` | Displays player statistics | `/metin stats` or `/metin stats Notch` |
| `/metin givevoucher <player> <multiplier> <time>` | Gives player a boost voucher | `/metin givevoucher Steve 2.0 3600` |
| `/metin boost global <multiplier> <time>` | Activates global boost | `/metin boost global 3.0 7200` |
| `/metin boost player <multiplier> <time> <player>` | Activates boost for player | `/metin boost player 1.5 1800 Steve` |
| `/metin updateleaderboard` | Manually updates ranking files | `/metin updateleaderboard` |

**Parameters:**
- `<type>` - Crystal ID from config.yml (e.g., metin_common, metin_rare)
- `<id>` - Crystal UUID (from /metin list command)
- `<name>` - Optional custom crystal name
- `<multiplier>` - Boost value (1.5 = 1.5x, 2.0 = 2x, etc.)
- `<time>` - Duration in seconds (3600 = 1h, 7200 = 2h)

---

## 🔐 Permissions

The plugin uses a simple permission system:

```
simplemetin.admin
```

**Default:** Operators only (OP)
**Grants access to:** All plugin commands

You can assign this permission to groups in your permission plugin (LuckPerms, PermissionsEx, GroupManager).

---

## ⚙️ Requirements

### Minimum:
- **Minecraft:** 1.21.1 or newer
- **Server Software:** Spigot 1.21.1+
- **Java:** 21 or newer

### Recommended:
- **Server Software:** Paper / Purpur / Pufferfish (better performance)
- **Java:** 21
- **RAM:** Minimum 2GB for server

### Optional Dependencies:
- **PlaceholderAPI** - Required for placeholder functionality
- **Vault** - If you want to use economy commands
- **EssentialsX** - Alternative to Vault

---

## 📦 Installation

### Step 1: Download the plugin
Download the latest version of `SimpleMetin-X.X.X.jar` file

### Step 2: Install on server
1. Stop the Minecraft server
2. Place the `.jar` file in the `/plugins/` folder
3. (Optional) Install PlaceholderAPI if you want to use placeholders

### Step 3: Start the server
Start the server. The plugin will automatically create:
- `/plugins/SimpleMetin/config.yml` - Main configuration
- `/plugins/SimpleMetin/data.yml` - Crystal data
- `/plugins/SimpleMetin/stats.yml` - Player statistics
- `/plugins/SimpleMetin/leaderboards.yml` - Rankings
- `/plugins/SimpleMetin/statistics.yml` - Readable statistics

### Step 4: Configuration
1. Stop the server
2. Edit `config.yml` according to your needs:
   - Adjust crystal HP
   - Configure drops and chances
   - Set respawn times
   - Change messages
3. Save and start the server

### Step 5: Create your first crystals!
In-game use the command:
```
/metin spawn metin_common
```

The plugin includes two pre-configured crystal types:
- `metin_common` - Common crystal (50 HP, 5 min respawn)
- `metin_rare` - Rare crystal (150 HP, one-time)

---

## 🎯 Usage Examples

### Creating Crystals

**Basic spawn:**
```
/metin spawn metin_common
```

**Spawn with custom name:**
```
/metin spawn metin_rare &d&lLEGENDARY BOSS METIN
```

**Spawn with custom colored name:**
```
/metin spawn metin_common &6&l⭐ &e&lEVENT METIN &6&l⭐
```

### Managing Crystals

**List all crystals:**
```
/metin list
```
Output:
```
Active Crystals:
- 550e8400-e29b-41d4-a716-446655440000 [ACTIVE] Type: metin_common HP: 50/50
- 550e8400-e29b-41d4-a716-446655440001 [DESTROYED] Type: metin_rare HP: 0/150
```

**Removing a crystal:**
```
/metin remove 550e8400-e29b-41d4-a716-446655440000
```

**Manual respawn:**
```
/metin respawn 550e8400-e29b-41d4-a716-446655440001
```

### Boost System

**Global 2x boost for 1 hour:**
```
/metin boost global 2.0 3600
```
*All players on the server will receive a broadcast and 2x drop chances for 1h*

**Global 3x boost for 2 hours (Happy Hour):**
```
/metin boost global 3.0 7200
```

**Personal boost for player:**
```
/metin boost player 1.5 1800 Steve
```
*Player Steve will receive 1.5x boost for 30 minutes*

**Giving a voucher:**
```
/metin givevoucher Notch 2.5 3600
```
*Gives player Notch a voucher for 2.5x boost for 1 hour*

### Statistics

**Own statistics:**
```
/metin stats
```

**Another player's statistics:**
```
/metin stats Notch
```

Output:
```
Notch's Stats:
Crystals Destroyed: 47
Total Damage: 2350
Items Received: 156
Money Earned: $12500
```

### Updating Rankings

**Manual update:**
```
/metin updateleaderboard
```
*Immediately updates leaderboards.yml and statistics.yml files*

---

## 🔧 Configuration

### Basic config.yml Structure

```yaml
settings:
  save-interval: 300  # Auto-save every 5 minutes
  hologram-update-interval: 20  # Hologram update (ticks)
  actionbar-enabled: true  # Enable ActionBar
  bossbar-enabled: true  # Enable Boss Bars
  bossbar-range: 30.0  # Boss Bar range
  hit-cooldown: 1.0  # Cooldown between hits (seconds)
  leaderboard-update-interval: 6000  # Ranking update (ticks)

crystals:
  metin_common:  # Crystal ID
    display-name: "&e&lCommon Metin"
    type: respawn  # respawn or one-time
    max-hp: 50
    damage-per-hit: 1
    respawn-time: 300  # seconds
    show-actionbar: true
    show-bossbar: true
    hologram:
      enabled: true
      range: 15.0
      lines:
        - "&e&lCOMMON METIN"
        - "&7HP: &c%hp%&7/&c%max_hp%"
    hit-drops:
      drop1:
        item: DIAMOND
        amount: 1
        chance: 5.0  # 5% chance
        name: "&bMetin Diamond"
    death-drops:
      drop1:
        item: DIAMOND
        amount: 5
        chance: 100.0  # 100% chance
```

### Example Configurations

**Boss Crystal (Hard, Rare):**
```yaml
crystals:
  metin_boss:
    display-name: "&4&l⚔ BOSS METIN ⚔"
    type: one-time
    max-hp: 500
    damage-per-hit: 5
    respawn-time: 0
    show-actionbar: true
    show-bossbar: true
    hologram:
      enabled: true
      range: 30.0
      lines:
        - "&4&l⚔ BOSS METIN ⚔"
        - "&c&lHP: %hp%/%max_hp%"
        - "&6&lDESTROY FOR EPIC LOOT!"
    death-drops:
      drop1:
        item: NETHERITE_SWORD
        amount: 1
        chance: 100.0
        name: "&4&lBoss Slayer Sword"
        lore:
          - "&7Legendary weapon"
          - "&7Obtained from Boss Metin"
      drop2:
        item: DIAMOND_BLOCK
        amount: 16
        chance: 100.0
    death-commands:
      cmd1:
        command: "eco give %player% 10000"
        chance: 100.0
      cmd2:
        command: "broadcast &6&l[BOSS] &e%player% &6defeated the Boss Metin!"
        chance: 100.0
```

**Farm Crystal (Easy, Frequent Respawn):**
```yaml
crystals:
  metin_farm:
    display-name: "&a&lFarm Metin"
    type: respawn
    max-hp: 30
    damage-per-hit: 1
    respawn-time: 60  # 1 minute
    show-actionbar: true
    show-bossbar: true
    hit-drops:
      drop1:
        item: IRON_INGOT
        amount: 1
        chance: 30.0
      drop2:
        item: GOLD_INGOT
        amount: 1
        chance: 15.0
    hit-commands:
      cmd1:
        command: "eco give %player% 5"
        chance: 50.0
```

---

## 🛠️ Technical Features

### Thread-Safety
- All operations are thread-safe
- Uses `ConcurrentHashMap` for multithreading safety
- Asynchronous I/O operations (file saving)
- No lag during data saving

### Event API for Developers

The plugin provides custom event `MetinCrystalDestroyedEvent`:

```java
@EventHandler
public void onMetinDestroyed(MetinCrystalDestroyedEvent event) {
    CrystalData crystal = event.getCrystalData();
    Player killer = event.getKiller();
    List<DropItem> drops = event.getDrops();

    // Your logic...
}
```

### NBT/Persistent Data Container
- Vouchers use PDC (safe NBT)
- Impossible to forge or duplicate
- Preserve data between restarts

### Performance Optimization
- Lazy loading of holograms
- Efficient update cycles
- Minimal CPU usage
- Optimal for 100+ player servers

---

## 📝 FAQ (Frequently Asked Questions)

**Q: Does the plugin work with Spigot?**
A: Yes, but we recommend Paper/Purpur for better performance.

**Q: Can I have more than 2 crystal types?**
A: Yes! You can add any number of types in config.yml.

**Q: Are statistics saved after restart?**
A: Yes, all data is auto-saved every 5 minutes and on server shutdown.

**Q: Can I disable Boss Bars?**
A: Yes, set `settings.bossbar-enabled: false` in config.yml.

**Q: How do I change drop chances?**
A: Edit the `chance` value in the drops section (0-100%).

**Q: Do boosts stack?**
A: Yes, you can configure whether personal + global combine in the `boosts` section.

**Q: Does the plugin support economy?**
A: Yes, through commands you can use Vault/EssentialsX to give money.

**Q: How do I use placeholders?**
A: Install PlaceholderAPI and use placeholders in other plugins (e.g., %metin_crystals_destroyed%).

**Q: Can I change the voucher appearance?**
A: Yes, full configuration in the `boosts.personal.voucher` section.

**Q: How do I backup data?**
A: Copy the `/plugins/SimpleMetin/` folder - it contains all data.

---

## 🐛 Bug Reporting

Found a bug? Help improve the plugin!

1. Check if the issue hasn't already been reported
2. Gather information:
   - Plugin version
   - Server version (Paper/Spigot)
   - Java version
   - Error log (if any)
   - Steps to reproduce the bug
3. Report in the **Issues** or **Discussion** section

---

## 💡 Feature Suggestions

Have an idea for a new feature? Let us know!

- Describe exactly how it should work
- Explain why it would be useful
- Leave a comment in the **Discussion** section

---

## 🔄 Changelog

### Version 1.0.0
- Initial release
- Crystal system (Respawn + One-Time)
- Drop system (Hit + Death)
- Boost system (Personal + Global)
- Statistics and TOP 100 ranking system
- PlaceholderAPI integration (40+ placeholders)
- Boss Bars for crystals and boosts
- Voucher system
- Auto-save system
- Event API for developers

---

## 📄 License

The plugin is property of the author. Purchase grants the right to:
- Use on your own server
- Modify configuration
- Private code editing (without redistribution)

Prohibited:
- Reselling the plugin
- Public sharing of .jar file
- Publishing modified code
- Claiming authorship

---

## ⭐ Like this plugin?

If **SimpleMetin** meets your expectations:

- Leave a **positive review** ⭐⭐⭐⭐⭐
- Share **screenshots** from your server
- Write a **review** describing your experience
- **Recommend** to fellow server owners

This motivates continued development and adding new features!

---

## 📧 Contact

- **SpigotMC**: [Author Profile]
- **Discord**: [Your Discord Tag]
- **Support**: Through Discussion section on SpigotMC

---

## 🎉 Acknowledgments

Thank you to all testers and players for feedback and help in plugin development!

**Enjoy SimpleMetin!** 🎮✨
