# SimpleMetin - Nowe Funkcje

## 📊 System Statystyk

### Trackowane Statystyki:
- **Crystals Destroyed** - Liczba zniszczonych kryształów (ogółem i per-type)
- **Total Damage** - Łączne obrażenia zadane kryształom
- **Items Received** - Liczba otrzymanych przedmiotów
- **Money Earned** - Zarobiona waluta
- **Last Seen** - Ostatnia aktywność

### Komendy:
```
/metin stats - Zobacz swoje statystyki
/metin stats <gracz> - Zobacz statystyki innego gracza
```

### Zapisywanie:
- Statystyki zapisywane w `plugins/SimpleMetin/stats.yml`
- Automatyczny zapis przy wyłączeniu serwera
- Tracking w czasie rzeczywistym

---

## 💎 System Drop Boostów

### Rodzaje Boostów:

#### 1. Personal Boost (Osobisty)
- Aktywowany przez voucher (prawy klik)
- Działa tylko dla danego gracza
- Stackuje się z global boost (konfigurowalne)

#### 2. Global Boost (Globalny)
- Aktywowany przez admina
- Działa dla wszystkich graczy na serwerze
- Broadcast przy aktywacji

### Konfiguracja (config.yml):

```yaml
boosts:
  global:
    enabled: true
    stacking-mode: 1.0  # 1.0 = dodawanie, 2.0 = mnożenie z personal
  personal:
    enabled: true
    stacking-enabled: true  # Czy stackuje z global
    voucher:
      material: PAPER
      name: "&e&l✦ DROP BOOST VOUCHER ✦"
      lore:
        - "&7Right-click to activate"
        - "&7Boost: &e%multiplier%x"
        - "&7Duration: &e%duration%"
        - ""
        - "&6&lPERSONAL BOOST"
      glow: true  # Enchant glow effect
      custom-model-data: 0  # Resource pack support
```

### Komendy Admina:

#### Dawanie Voucherów:
```
/metin givevoucher <gracz> <mnożnik> <czas_w_sekundach>

Przykłady:
/metin givevoucher Notch 2.0 3600  (2x boost na 1 godzinę)
/metin givevoucher Steve 1.5 600   (1.5x boost na 10 minut)
```

#### Aktywacja Boostów:
```
# Global boost (dla wszystkich)
/metin boost global <mnożnik> <czas>
/metin boost global 2.0 7200  (2x drop dla wszystkich na 2h)

# Personal boost (dla konkretnego gracza)
/metin boost player <mnożnik> <czas> <gracz>
/metin boost player 3.0 1800 Notch  (3x drop dla Notch na 30min)
```

### Jak Działa Boost:

#### Przykład bez boosta:
- Diamond chance: 5%
- Wynik: 5% szansy na drop

#### Przykład z 2x boostem:
- Diamond chance: 5%
- Z boostem: 5% * 2.0 = 10%
- Wynik: 10% szansy na drop

#### Przykład z 3x boostem:
- Diamond chance: 35%
- Z boostem: 35% * 3.0 = 100% (cap)
- Wynik: 100% szansy na drop (gwarantowane)

### Stackowanie Boostów:

#### Additive Mode (stacking-mode: 1.0):
```
Personal: 2.0x
Global: 1.5x
Total: 2.0 + 0.5 = 2.5x
```

#### Multiplicative Mode (stacking-mode: 2.0):
```
Personal: 2.0x
Global: 1.5x
Total: 2.0 * 1.5 = 3.0x
```

### Wiadomości:

```yaml
messages:
  boost-activated: "&a&lBOOST ACTIVATED! &e%multiplier%x &adrop rate for &e%duration%"
  boost-expired: "&c&lBOOST EXPIRED! &7Your drop boost has ended."
  global-boost-activated: "&6&l[SERVER] &e%multiplier%x &6Global Drop Boost activated for &e%duration%&6!"
  global-boost-expired: "&c&l[SERVER] &7Global Drop Boost has expired."
  boost-already-active: "&cYou already have an active boost!"
```

---

## 🎮 PlaceholderAPI Integration

### Instalacja:
1. Zainstaluj PlaceholderAPI na serwerze
2. Restart serwera
3. Placeholdery automatycznie dostępne

### Dostępne Placeholdery:
Zobacz `PLACEHOLDERS.md` dla pełnej listy

---

## 📝 Przykładowe Scenariusze

### Scenario 1: Weekend Event
```bash
# Piątek 18:00 - Aktywuj global boost na weekend
/metin boost global 2.0 259200

# Komunikat dla graczy:
"Global 2x Drop Boost activated for 72h 0m 0s!"
```

### Scenario 2: VIP Rewards
```bash
# Daj VIP graczom voucher na 1.5x przez 7 dni
/metin givevoucher VIP_Gracz 1.5 604800
```

### Scenario 3: Event Rewards
```bash
# Zwycięzca eventu dostaje 3x boost na godzinę
/metin boost player 3.0 3600 Zwyciezca
```

---

## 🔧 Techniczne Detale

### Performance:
- Statystyki w ConcurrentHashMap (thread-safe)
- Boosty sprawdzane tylko przy dropie
- Minimum overhead

### Data Storage:
- `stats.yml` - Statystyki graczy
- Auto-save przy shutdown
- Lazy loading przy loginie

### Compatibility:
- Paper 1.21.1+
- PlaceholderAPI (optional)
- Backwards compatible z istniejącymi kryształami

---

## ⚙️ Migration Guide

### Dla Istniejących Serwerów:
1. Update plugin JARa
2. Restart serwera
3. Statystyki zaczną się trackować automatycznie
4. Stare kryształy działają bez zmian
5. (Opcjonalnie) Zainstaluj PlaceholderAPI

### Config Changes:
- Dodano sekcję `boosts:`
- Dodano wiadomości boost-related
- Wszystko backwards compatible

Enjoy! 🎉
