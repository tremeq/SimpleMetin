# Quick Start - Statystyki i Boosty

## 🚀 Fast Setup (5 minut)

### 1. Instalacja
```bash
1. Wrzuć SimpleMetin-1.0.0.jar do /plugins/
2. (Opcjonalnie) Zainstaluj PlaceholderAPI
3. Restart serwera
4. Gotowe!
```

### 2. Pierwsze Kroki

#### Sprawdź Swoje Statystyki:
```
/metin stats
```

#### Daj Sobie Voucher Testowy:
```
/metin givevoucher TwojaNick 2.0 600
```
*(2x boost na 10 minut)*

#### Użyj Vouchera:
```
- Prawy klik z voucherem w ręce
- Zobaczysz: "BOOST ACTIVATED! 2.0x drop rate for 10m"
```

#### Aktywuj Global Boost:
```
/metin boost global 1.5 3600
```
*(1.5x dla wszystkich na 1h)*

---

## 💡 Typowe Use Cases

### Use Case 1: VIP System
```bash
# W skrypcie nadawania VIP:
execute console command: metin givevoucher %player% 2.0 2592000

# Gracz dostaje 2x boost na 30 dni jako benefit VIP
```

### Use Case 2: Daily Login Reward
```bash
# W pluginie nagród dziennych:
on day 7:
  execute: metin givevoucher %player% 3.0 86400

# Co 7 dni gracz dostaje 3x boost na 24h
```

### Use Case 3: Happy Hour
```bash
# Codziennie o 20:00 przez 2 godziny:
/metin boost global 2.0 7200

# Automatyzacja przez cron/scheduler
```

---

## 📊 Integracja z PlaceholderAPI

### Scoreboard (DeluxeScoreboard):
```yaml
scoreboard.yml:
  lines:
    - "&e&lMETIN STATS"
    - ""
    - "&7Destroyed: &f%metin_crystals_destroyed%"
    - "&7Damage: &f%metin_total_damage%"
    - ""
    - "&eBoost: &f%metin_boost_multiplier%x"
    - "&7Time: &f%metin_personal_boost_time%"
```

### TAB List:
```yaml
# TAB plugin config:
tabprefix: "&7[&e★%metin_boost_multiplier%x&7] "

# Wynik: [★2.0x] NickGracza
```

### Chat Format:
```yaml
# EssentialsChat format:
format: '{DISPLAYNAME}&7: &f{MESSAGE} &8(&e%metin_crystals_destroyed%&8)'
```

---

## 🎨 Customizacja Voucherów

### Własny Materiał:
```yaml
boosts:
  personal:
    voucher:
      material: NETHER_STAR  # Zmień na Nether Star
      name: "&b&l⚡ MEGA BOOST ⚡"
      glow: true
```

### Custom Model Data (Resource Pack):
```yaml
boosts:
  personal:
    voucher:
      custom-model-data: 1001  # Twój custom item
```

### Własne Lore:
```yaml
boosts:
  personal:
    voucher:
      lore:
        - ""
        - "&6&lPREMIUM BOOST"
        - "&eKliknij PPM aby aktywować"
        - ""
        - "&7Bonus: &e%multiplier%x dropów"
        - "&7Czas: &e%duration%"
        - ""
        - "&c&lJEDNORAZOWE UŻYCIE"
```

---

## 🔍 Troubleshooting

### Problem: Placeholdery nie działają
**Rozwiązanie:**
```bash
1. Sprawdź czy PlaceholderAPI jest zainstalowany: /papi list
2. Sprawdź czy expansion jest loaded: /papi info metin
3. Zrestartuj serwer
```

### Problem: Voucher nie aktywuje boosta
**Rozwiązanie:**
```bash
1. Sprawdź czy voucher ma NBT data (wygenerowany przez /metin givevoucher)
2. Sprawdź czy gracz już nie ma aktywnego boosta
3. Sprawdź logi serwera dla błędów
```

### Problem: Boosty nie stackują się
**Rozwiązanie:**
```yaml
# W config.yml:
boosts:
  personal:
    stacking-enabled: true  # Musi być true

  global:
    stacking-mode: 1.0  # 1.0 = additive, 2.0 = multiplicative
```

---

## 📈 Monitoring

### Check Active Boosts:
```bash
# W grze jako gracz:
/metin stats

# Zobacz sekcję z boostami
# Placeholder: %metin_boost_multiplier%
```

### Admin Monitoring:
```bash
# Logi serwera pokażą:
[SimpleMetin] Global boost activated: 2.0x for 3600 seconds
[SimpleMetin] Personal boost activated for Notch: 1.5x for 1800 seconds
```

---

## 🎯 Best Practices

### 1. Balance Boostów:
- **Daily players**: 1.2x - 1.5x
- **VIP/Donors**: 2.0x - 2.5x
- **Events**: 3.0x - 5.0x
- **Max**: Nie przekraczaj 10x (może zepsuć economię)

### 2. Czasy Trwania:
- **Short**: 10-30 min (event rewards)
- **Medium**: 1-4h (happy hour)
- **Long**: 24h-7dni (VIP benefits)
- **Permanent**: Nie zalecane (używaj permisji zamiast)

### 3. Stackowanie:
- **Additive** (1.0): Bezpieczniejsze, łatwiej balansować
- **Multiplicative** (2.0): Potężniejsze, wymaga ostrożności

### 4. Wiadomości:
- Wyłącz spamujące wiadomości dla często używanych voucherów
- Zostaw broadcast dla global boostów (server-wide)

---

Wszystko gotowe! Miłego użytkowania! 🎮✨
