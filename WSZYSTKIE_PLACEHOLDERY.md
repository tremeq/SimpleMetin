# 📋 WSZYSTKIE PLACEHOLDERY - SimpleMetin

## ✅ Wymagania
- **PlaceholderAPI** musi być zainstalowane na serwerze
- Po instalacji restartuj serwer

---

## 📊 STATYSTYKI GRACZA

### Podstawowe Statystyki:

| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_crystals_destroyed%` | Łączna liczba zniszczonych kryształów | `42` |
| `%metin_total_damage%` | Suma zadanych obrażeń | `1250` |
| `%metin_items_received%` | Liczba otrzymanych przedmiotów | `156` |
| `%metin_money_earned%` | Zarobiona waluta | `50000` |

### Statystyki Per-Crystal Type:

| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_type_<crystal_id>%` | Zniszczone kryształy danego typu | `25` |

**Przykłady użycia:**
- `%metin_type_metin_common%` - Liczba zniszczonych Common Metin
- `%metin_type_metin_rare%` - Liczba zniszczonych Rare Metin
- `%metin_type_metin_epic%` - Twój własny typ
- `%metin_type_boss_crystal%` - Dowolny ID z config.yml

### 🏆 TOP Leaderboard (Ranking):

**Format:** `%metin_<stat_type>_top_<position>_name%` i `%metin_<stat_type>_top_<position>%`

#### TOP - Zniszczone Kryształy:
| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_destroyed_top_1_name%` | Nick gracza #1 | `Notch` |
| `%metin_destroyed_top_1%` | Zniszczone kryształy gracza #1 | `542` |
| `%metin_destroyed_top_2_name%` | Nick gracza #2 | `jeb_` |
| `%metin_destroyed_top_2%` | Zniszczone kryształy gracza #2 | `421` |

#### TOP - Zadane Obrażenia:
| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_damage_top_1_name%` | Nick gracza #1 | `Notch` |
| `%metin_damage_top_1%` | Obrażenia gracza #1 | `150000` |

#### TOP - Otrzymane Przedmioty:
| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_items_top_1_name%` | Nick gracza #1 | `Notch` |
| `%metin_items_top_1%` | Przedmioty gracza #1 | `1250` |

#### TOP - Zarobione Pieniądze:
| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_money_top_1_name%` | Nick gracza #1 | `Notch` |
| `%metin_money_top_1%` | Pieniądze gracza #1 | `500000` |

**Możesz użyć dowolnej pozycji (1-100) dla każdego typu:**
- `%metin_destroyed_top_1%` do `%metin_destroyed_top_100%`
- `%metin_damage_top_1%` do `%metin_damage_top_100%`
- `%metin_items_top_1%` do `%metin_items_top_100%`
- `%metin_money_top_1%` do `%metin_money_top_100%`

**Jeśli pozycja nie istnieje (np. tylko 5 graczy, a prosisz o TOP 10):**
- `%metin_destroyed_top_10_name%` zwróci: `-`
- `%metin_destroyed_top_10%` zwróci: `0`

---

## 💎 BOOSTY DROPÓW

### Status Boostów:

| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_boost_multiplier%` | **CAŁKOWITY** mnożnik (personal + global) | `2.5` |
| `%metin_boost_active%` | Czy jakikolwiek boost aktywny | `true` / `false` |

### Personal Boost (Osobisty):

| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_personal_boost_multiplier%` | Mnożnik personal boosta | `2.0` (lub `1.0` jeśli brak) |
| `%metin_personal_boost_time%` | Pozostały czas personal boosta | `45m 30s` / `5h 12m 45s` / `0s` |

### Global Boost (Globalny):

| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `%metin_global_boost_multiplier%` | Mnożnik global boosta | `1.5` (lub `1.0` jeśli brak) |
| `%metin_global_boost_time%` | Pozostały czas global boosta | `1h 25m 10s` / `0s` |

---

## 🎨 PRZYKŁADY UŻYCIA

### 1. Scoreboard (DeluxeScoreboard / FeatherBoard)

```yaml
scoreboard.yml:
  lines:
    - "&e&l━━━━━━━━━━━━━━━"
    - "&6&lMETIN STATS"
    - "&e&l━━━━━━━━━━━━━━━"
    - ""
    - "&7Zniszczono: &e%metin_crystals_destroyed%"
    - "&7Obrażenia: &e%metin_total_damage%"
    - "&7Przedmioty: &e%metin_items_received%"
    - "&7Zarobki: &a$%metin_money_earned%"
    - ""
    - "&6&lBOOST"
    - "&7Mnożnik: &e%metin_boost_multiplier%x"
    - "&7Personal: &e%metin_personal_boost_time%"
    - "&7Global: &e%metin_global_boost_time%"
    - ""
    - "&e&l━━━━━━━━━━━━━━━"
```

### 2. TAB List (TAB Plugin)

```yaml
# Header
header:
  - "&e&lMETIN SERVER"
  - "&7Boost: &e%metin_boost_multiplier%x"

# Player prefix
tabprefix: "&7[&e★%metin_boost_multiplier%x&7] "

# Player suffix
tabsuffix: " &8(&e%metin_crystals_destroyed%&8)"

# Rezultat:
# [★2.0x] NickGracza (42)
```

### 3. Chat (EssentialsChat)

```yaml
format: '{DISPLAYNAME}&7: &f{MESSAGE} &8[&e%metin_crystals_destroyed% &7crystals&8]'

# Rezultat:
# Gracz: Hej! [42 crystals]
```

### 4. Hologramy (DecentHolograms)

**Osobiste Statystyki:**
```yaml
lines:
  - "&e&l⚡ TOP HUNTER ⚡"
  - "&7%player_name%"
  - ""
  - "&6Stats:"
  - "&7Zniszczono: &e%metin_crystals_destroyed%"
  - "&7Damage: &e%metin_total_damage%"
  - ""
  - "&aBoost: &e%metin_boost_multiplier%x"
  - "&7Czas: &e%metin_personal_boost_time%"
```

**TOP 10 Ranking - Zniszczone Kryształy:**
```yaml
lines:
  - content: '&a&lᴛᴏᴘᴋᴀ &7- &e&lᴢɴɪsᴢᴄᴢᴏɴᴇ ᴋʀʏsᴢᴛᴀʟʏ'
    height: 0.3
  - content: '&6&l1. &f%metin_destroyed_top_1_name% &7- &e%metin_destroyed_top_1%'
    height: 0.3
  - content: '&6&l2. &f%metin_destroyed_top_2_name% &7- &e%metin_destroyed_top_2%'
    height: 0.3
  - content: '&6&l3. &f%metin_destroyed_top_3_name% &7- &e%metin_destroyed_top_3%'
    height: 0.3
  - content: '4. &f%metin_destroyed_top_4_name% &7- &e%metin_destroyed_top_4%'
    height: 0.3
  - content: '5. &f%metin_destroyed_top_5_name% &7- &e%metin_destroyed_top_5%'
    height: 0.3
  - content: '6. &f%metin_destroyed_top_6_name% &7- &e%metin_destroyed_top_6%'
    height: 0.3
  - content: '7. &f%metin_destroyed_top_7_name% &7- &e%metin_destroyed_top_7%'
    height: 0.3
  - content: '8. &f%metin_destroyed_top_8_name% &7- &e%metin_destroyed_top_8%'
    height: 0.3
  - content: '9. &f%metin_destroyed_top_9_name% &7- &e%metin_destroyed_top_9%'
    height: 0.3
  - content: '10. &f%metin_destroyed_top_10_name% &7- &e%metin_destroyed_top_10%'
    height: 0.3
```

**TOP 10 Ranking - Zadane Obrażenia:**
```yaml
lines:
  - content: '&a&lᴛᴏᴘᴋᴀ &7- &c&lᴏʙʀᴀᴢᴇɴɪᴀ'
    height: 0.3
  - content: '&6&l1. &f%metin_damage_top_1_name% &7- &c%metin_damage_top_1%'
    height: 0.3
  - content: '&6&l2. &f%metin_damage_top_2_name% &7- &c%metin_damage_top_2%'
    height: 0.3
  - content: '&6&l3. &f%metin_damage_top_3_name% &7- &c%metin_damage_top_3%'
    height: 0.3
  - content: '4. &f%metin_damage_top_4_name% &7- &c%metin_damage_top_4%'
    height: 0.3
  - content: '5. &f%metin_damage_top_5_name% &7- &c%metin_damage_top_5%'
    height: 0.3
```

**TOP 5 Ranking - Zarobione Pieniądze:**
```yaml
lines:
  - content: '&a&lᴛᴏᴘᴋᴀ &7- &a&lᴘɪᴇɴɪᴀᴅᴢᴇ'
    height: 0.3
  - content: '&6&l1. &f%metin_money_top_1_name% &7- &a$%metin_money_top_1%'
    height: 0.3
  - content: '&6&l2. &f%metin_money_top_2_name% &7- &a$%metin_money_top_2%'
    height: 0.3
  - content: '&6&l3. &f%metin_money_top_3_name% &7- &a$%metin_money_top_3%'
    height: 0.3
  - content: '4. &f%metin_money_top_4_name% &7- &a$%metin_money_top_4%'
    height: 0.3
  - content: '5. &f%metin_money_top_5_name% &7- &a$%metin_money_top_5%'
    height: 0.3
```

### 5. Action Bar (z DeluxeMenus lub innym)

```yaml
actionbar: "&eBoost: &6%metin_boost_multiplier%x &7| &eZniszczono: &6%metin_crystals_destroyed%"
```

### 6. BossBar (z BossBarAPI)

```yaml
bars:
  metin_stats:
    message: "&e⚡ Boost: &6%metin_boost_multiplier%x &e| Destroyed: &6%metin_crystals_destroyed%"
```

### 7. Leaderboard (z AjLeaderboards)

```yaml
leaderboard:
  metin_top:
    placeholder: "%metin_crystals_destroyed%"
    format: "&7#%position% &e%player% &7- &6%value%"
```

---

## 🔢 FORMAT WARTOŚCI

### Liczby:
- Wszystkie liczby są zwracane jako string
- Bez formatowania (np. `1000` nie `1,000`)
- Jeśli chcesz formatowanie - użyj placeholder processora

### Czas (Time Format):
Automatyczne formatowanie dla `%metin_personal_boost_time%` i `%metin_global_boost_time%`:

| Czas | Format |
|------|--------|
| Powyżej godziny | `2h 30m 15s` |
| Powyżej minuty | `45m 30s` |
| Poniżej minuty | `45s` |
| Brak boostu | `0s` |

### Mnożniki:
- Format: `1.0`, `2.0`, `2.5`, `10.0`
- Jedna cyfra po przecinku
- Brak boostu = `1.0` (nie `0.0`)

### Boolean:
- `%metin_boost_active%` zwraca: `true` lub `false`
- Używaj w warunkach (z CMI, DeluxeMenus, itp.)

---

## 🎯 ZAAWANSOWANE PRZYKŁADY

### Conditional Display (z CMI Placeholders)

```yaml
# Pokaż boost tylko gdy aktywny
%cmi_is_placeholder_true_{metin_boost_active}?&aBoost: %metin_boost_multiplier%x:&cBrak boostu%

# Pokaż czas tylko gdy > 0
%cmi_is_placeholder_bigger_{metin_personal_boost_time}_0?Pozostało: %metin_personal_boost_time%:%
```

### Ranking Colors (z Conditional Placeholders)

```yaml
# Koloruj based on crystal count
%conditional_metin_crystals_destroyed_>=_100?&6&lLEGENDA:%
%conditional_metin_crystals_destroyed_>=_50?&e&lMISTRZ:%
%conditional_metin_crystals_destroyed_>=_10?&a&lŁOWCA:%
&7NOWICJUSZ
```

### Progress Bar dla Boosta

```yaml
# Z ProgressBars plugin
{bar_{metin_personal_boost_time}_0_3600_&a█_&7█_20}

# Pokazuje wizualny bar ile czasu zostało
```

---

## 🔧 TROUBLESHOOTING

### Problem: Pokazuje `%metin_crystals_destroyed%` zamiast liczby

**Rozwiązanie:**
```bash
1. Sprawdź czy PlaceholderAPI jest zainstalowane: /papi version
2. Sprawdź czy expansion jest loaded: /papi info metin
3. Jeśli nie ma na liście:
   - Zrestartuj serwer
   - Sprawdź logi czy SimpleMetin się załadował
4. Sprawdź czy używasz placeholdera w kompatybilnym pluginie
```

### Problem: Statystyki pokazują zawsze 0

**Rozwiązanie:**
```bash
1. Sprawdź czy masz już jakieś statystyki: /metin stats
2. Zniszcz kryształ aby zacząć trackowanie
3. Statystyki są per-player - sprawdź jako właściwy gracz
```

### Problem: Boost time pokazuje 0s mimo aktywnego boosta

**Rozwiązanie:**
```bash
1. Sprawdź czy boost rzeczywiście aktywny: /metin stats
2. Poczekaj 1-2 sekundy (update interval)
3. Sprawdź config: boosts.bossbar.update-interval
```

---

## 📝 NOTATKI TECHNICZNE

### Case Sensitivity:
- **Wszystkie placeholdery są CASE INSENSITIVE**
- `%metin_crystals_destroyed%` = `%METIN_CRYSTALS_DESTROYED%` = `%Metin_Crystals_Destroyed%`

### Performance:
- Placeholdery są cachowane co update interval
- Minimalny impact na TPS
- Safe dla scoreboardów aktualizujących co tick

### Per-Player Data:
- Wszystkie placeholdery są per-player (oprócz global boost)
- Gracz A widzi swoje stats, Gracz B swoje

### Storage:
- Stats w `plugins/SimpleMetin/stats.yml`
- Auto-save przy wyłączeniu serwera
- Lazy loading (ładuje tylko gdy potrzebne)

---

## 📊 KOMPLETNA LISTA PLACEHOLDERÓW

### Statystyki Gracza (5):
```
%metin_crystals_destroyed%         - Łączna liczba zniszczonych kryształów
%metin_total_damage%                - Suma zadanych obrażeń
%metin_items_received%              - Liczba otrzymanych przedmiotów
%metin_money_earned%                - Zarobiona waluta
%metin_type_<crystal_id>%           - Zniszczone kryształy danego typu (dynamiczny)
```

### Boosty (6):
```
%metin_boost_multiplier%            - CAŁKOWITY mnożnik (personal + global)
%metin_boost_active%                - Czy jakikolwiek boost aktywny (true/false)
%metin_personal_boost_multiplier%   - Mnożnik personal boosta
%metin_personal_boost_time%         - Pozostały czas personal boosta
%metin_global_boost_multiplier%     - Mnożnik global boosta
%metin_global_boost_time%           - Pozostały czas global boosta
```

### 🏆 TOP Ranking (800+):

**Format:** `%metin_<stat>_top_<position>_name%` i `%metin_<stat>_top_<position>%`

#### Zniszczone Kryształy (200):
```
%metin_destroyed_top_1_name%        - Nick gracza #1
%metin_destroyed_top_1%             - Liczba zniszczonych kryształów #1
%metin_destroyed_top_2_name%        - Nick gracza #2
%metin_destroyed_top_2%             - Liczba zniszczonych kryształów #2
...                                 (działa dla TOP 1-100)
%metin_destroyed_top_100_name%      - Nick gracza #100
%metin_destroyed_top_100%           - Liczba zniszczonych kryształów #100
```

#### Zadane Obrażenia (200):
```
%metin_damage_top_1_name%           - Nick gracza #1
%metin_damage_top_1%                - Obrażenia #1
%metin_damage_top_2_name%           - Nick gracza #2
%metin_damage_top_2%                - Obrażenia #2
...                                 (działa dla TOP 1-100)
```

#### Otrzymane Przedmioty (200):
```
%metin_items_top_1_name%            - Nick gracza #1
%metin_items_top_1%                 - Przedmioty #1
%metin_items_top_2_name%            - Nick gracza #2
%metin_items_top_2%                 - Przedmioty #2
...                                 (działa dla TOP 1-100)
```

#### Zarobione Pieniądze (200):
```
%metin_money_top_1_name%            - Nick gracza #1
%metin_money_top_1%                 - Pieniądze #1
%metin_money_top_2_name%            - Nick gracza #2
%metin_money_top_2%                 - Pieniądze #2
...                                 (działa dla TOP 1-100)
```

**RAZEM: 811+ placeholderów** (5 statystyk + 6 boostów + 800 TOP ranking)

---

## 🎮 PRZYKŁADOWY KOMPLETNY SETUP

### Scoreboard + TAB + Chat + Hologram

**Scoreboard (FeatherBoard):**
```yaml
metin-board:
  interval: 20
  lines:
    - "&e&lMETIN STATS"
    - "&7Destroyed: &e%metin_crystals_destroyed%"
    - "&7Boost: &e%metin_boost_multiplier%x"
    - "&7Time: &e%metin_personal_boost_time%"
```

**TAB (TAB plugin):**
```yaml
tabprefix: "%conditional_metin_boost_active_true?&e★ :%"
```

**Chat (EssentialsChat):**
```yaml
format: '{TABPREFIX}{DISPLAYNAME}&7: &f{MESSAGE}'
```

**Hologram (DecentHolograms):**
```yaml
lines:
  - "&e&lTOP PLAYER"
  - "&7%player_name%"
  - "&e%metin_crystals_destroyed% &7crystals"
```

---

**🎉 GOTOWE! Wszystkie 12+ placeholderów gotowe do użycia!**
