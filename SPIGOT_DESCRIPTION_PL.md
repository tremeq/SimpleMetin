# SimpleMetin - Zaawansowany System Kryształów Metin

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.1+-green.svg)
![Java](https://img.shields.io/badge/java-21-orange.svg)

---

## 📖 O Pluginie

**SimpleMetin** to innowacyjny plugin wprowadzający do Twojego serwera Minecraft unikalny system kopania kryształów inspirowany popularną grą MMORPG - Metin2. Gracze mogą niszczyć potężne kryształy Metin rozrzucone po świecie, zdobywając przy tym cenne nagrody, przedmioty i pieniądze!

Plugin został stworzony z myślą o serwerach RPG, PvE i survival, oferując graczom dodatkową aktywność oraz możliwość zdobywania wartościowych nagród. Dzięki zaawansowanemu systemowi boostów, rankingów i statystyk, gracze będą rywalizować o pozycję na leaderboardzie!

---

## ✨ Główne Funkcje

### 🔮 Zaawansowany System Kryształów

Plugin pozwala na tworzenie dwóch typów kryształów:

- **Kryształy Respawn** - Po zniszczeniu automatycznie odradzają się po określonym czasie, idealny do farm i ciągłej zabawy
- **Kryształy One-Time** - Jednorazowe kryształy, które po zniszczeniu znikają na zawsze, świetne do eventów i specjalnych nagród

**Cechy kryształów:**
- Konfigurowalny HP (żywotność kryształu)
- Ustawialny damage per hit (ile HP zabiera jedno uderzenie)
- Konfigurowalny czas respawnu dla kryształów typu Respawn
- Możliwość nadania własnej nazwy podczas spawnu
- Wizualne hologramy wyświetlające:
  - Nazwę kryształu
  - Aktualny stan HP
  - Własne komunikaty i informacje
- Dynamiczne Boss Bary pokazujące:
  - Postęp niszczenia kryształu
  - Wizualizację pozostałego HP
  - Automatyczne pokazywanie/chowanie w zależności od odległości gracza
- System zasięgu wyświetlania (konfigurowalny)
- Efekty wizualne po zniszczeniu (cząsteczki)
- Efekty dźwiękowe po zniszczeniu (konfigurowane)

**System Cooldown:**
- Konfigurowalny cooldown między uderzeniami (zapobiega spamowaniu)
- Komunikaty ActionBar informujące o czasie oczekiwania
- Całkowicie wyłączalny (ustaw 0.0 aby wyłączyć)

### 💎 Rozbudowany System Dropów

Plugin oferuje jeden z najbardziej zaawansowanych systemów dropów:

**Hit-Drops (Nagrody za uderzenie):**
- Przedmioty dropują się przy każdym trafieniu w kryształ
- Konfigurowane szanse procentowe (0-100%)
- Mnożnik szans przez system boostów
- Niestandardowe nazwy przedmiotów
- Wieloliniowe opisy (lore)
- Automatyczne dodawanie do ekwipunku lub dropowanie na ziemię
- Komunikat gdy ekwipunek jest pełny

**Death-Drops (Nagrody za zniszczenie):**
- Specjalne nagrody za całkowite zniszczenie kryształu
- Wyższe szanse i lepsze przedmioty niż hit-drops
- Wszystkie funkcje jak w hit-drops
- Idealne do rzadkich i wartościowych nagród

**Hit-Commands i Death-Commands:**
- Wykonywanie dowolnych komend jako nagrody
- Idealne do integracji z ekonomią serwera (Vault, EssentialsX)
- Wsparcie dla zmiennej %player%
- Konfigurowane szanse wykonania
- Automatyczne śledzenie zarobionych pieniędzy w statystykach
- Możliwość broadcastowania wiadomości, teleportacji, nadawania permisji, etc.

**Przykłady komend:**
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
    command: "broadcast &6%player% zniszczył Rare Metin!"
    chance: 100.0
```

### ⚡ Kompleksowy System Boostów

**Osobiste Boosty (Personal Boosts):**
- Boosty dla pojedynczych graczy
- Aktywacja przez vouchery (fizyczne przedmioty)
- Aktywacja przez admina komendą
- Konfigurowalny mnożnik (1.0 = brak boostu, 2.0 = 2x szanse)
- Konfigurowalny czas trwania (w sekundach)
- Nie można aktywować gdy gracz już ma aktywny boost
- Komunikaty o aktywacji i wygaśnięciu

**Globalne Boosty (Global Boosts):**
- Boosty dla całego serwera
- Aktywacja tylko przez admina
- Broadcast dla wszystkich graczy o aktywacji
- Idealny do Happy Hours, eventów serwerowych
- Działa równocześnie z osobistymi boostami

**System Voucherów:**
- Fizyczne przedmioty (domyślnie PAPER)
- Konfigurowalny wygląd:
  - Material
  - Nazwa (kolorowa)
  - Lore (wieloliniowy opis)
  - Enchant Glow (świecący efekt)
  - Custom Model Data (dla resource packów)
- Dane NBT/PDC zapewniające bezpieczeństwo
- Nie można sfałszować lub zduplikować
- Kliknięcie PPM = aktywacja

**Boss Bary Boostów:**
- Osobny Boss Bar dla boostu osobistego
- Osobny Boss Bar dla boostu globalnego
- Wyświetlają:
  - Mnożnik (np. 2.0x)
  - Pozostały czas (format: 1h 30m 45s)
  - Progress bar wizualizujący czas
- Konfigurowane kolory i style
- Automatyczne aktualizacje co sekundę
- Możliwość całkowitego wyłączenia

**Stackowanie Boostów:**
- Możliwość łączenia osobistego + globalnego
- Dwa tryby stackowania:
  - Addytywny (1.0 = dodawanie)
  - Multiplikatywny (2.0 = mnożenie)
- Pełna kontrola w konfiguracji

### 📊 Zaawansowany System Statystyk i Rankingów

**Śledzone Statystyki Graczy:**
- **Crystals Destroyed** - Całkowita liczba zniszczonych kryształów
- **Total Damage** - Suma wszystkich zadanych obrażeń
- **Items Received** - Liczba otrzymanych przedmiotów
- **Money Earned** - Zarobione pieniądze (z komend)
- **Per-Type Stats** - Statystyki dla każdego typu kryształu osobno
- **Last Seen** - Data ostatniej aktywności

**System Rankingów TOP 100:**
- Automatyczne generowanie TOP 100 dla każdej kategorii:
  - TOP zniszczonych kryształów
  - TOP zadanych obrażeń
  - TOP otrzymanych przedmiotów
  - TOP zarobionych pieniędzy
- Eksport do pliku `leaderboards.yml`
- Automatyczna aktualizacja co 5 minut (konfigurowalne)
- Manualna aktualizacja komendą `/metin updateleaderboard`
- Format przyjazny dla innych pluginów i stron WWW

**Eksport Danych:**
- `stats.yml` - Surowe dane wszystkich graczy (UUID-based)
- `leaderboards.yml` - TOP 100 rankingi dla każdej kategorii
- `statistics.yml` - Czytelne statystyki posortowane alfabetycznie po nickach
- Wszystkie pliki automatycznie aktualizowane
- Dane zachowywane między restartami

**Auto-Save System:**
- Automatyczny zapis co 5 minut (konfigurowalne)
- Zapis przy wyłączeniu serwera
- Asynchroniczne operacje (brak lagów)
- Backup danych przy każdym zapisie

### 🎨 Integracja z PlaceholderAPI

**Ponad 40+ placeholderów** gotowych do użycia w:
- Scoreboard (Featherboard, AnimatedScoreboard)
- TAB List (TAB, NametagEdit)
- Holograms (HolographicDisplays, DecentHolograms)
- Chat (DeluxeChat, ChatControl)
- Titles, ActionBar, Boss Bars

**Kategorie Placeholderów:**

**Statystyki Gracza:**
```
%metin_crystals_destroyed% - Zniszczone kryształy
%metin_total_damage% - Zadane obrażenia
%metin_items_received% - Otrzymane przedmioty
%metin_money_earned% - Zarobione pieniądze
%metin_type_<crystal_id>% - Statystyki dla konkretnego typu
```

**System Boostów:**
```
%metin_boost_multiplier% - Całkowity mnożnik (personal + global)
%metin_boost_active% - Czy aktywny? (true/false)
%metin_personal_boost_multiplier% - Mnożnik osobisty
%metin_personal_boost_time% - Pozostały czas osobistego
%metin_global_boost_multiplier% - Mnożnik globalny
%metin_global_boost_time% - Pozostały czas globalnego
```

**Rankingi TOP (1-100):**
```
%metin_destroyed_top_1_name% - Nick TOP 1 (zniszczone)
%metin_destroyed_top_1% - Wartość TOP 1 (zniszczone)
%metin_damage_top_1_name% - Nick TOP 1 (obrażenia)
%metin_damage_top_1% - Wartość TOP 1 (obrażenia)
%metin_items_top_1_name% - Nick TOP 1 (przedmioty)
%metin_items_top_1% - Wartość TOP 1 (przedmioty)
%metin_money_top_1_name% - Nick TOP 1 (pieniądze)
%metin_money_top_1% - Wartość TOP 1 (pieniądze)
```

*Zamień "1" na dowolną liczbę od 1 do 100 aby uzyskać kolejne miejsca!*

**Pełna dokumentacja** placeholderów dostępna w pliku `WSZYSTKIE_PLACEHOLDERY.md` w głównym folderze pluginu.

---

## 🎮 Komendy

Wszystkie komendy wymagają permisji `simplemetin.admin` (domyślnie OP).

| Komenda | Opis | Przykład |
|---------|------|----------|
| `/metin spawn <typ> [nazwa]` | Tworzy kryształ w lokalizacji gracza | `/metin spawn metin_rare Legendarny Metin` |
| `/metin remove <id>` | Usuwa kryształ o podanym ID | `/metin remove 550e8400-e29b...` |
| `/metin list` | Wyświetla listę wszystkich aktywnych kryształów | `/metin list` |
| `/metin respawn <id>` | Manualnie respawnuje zniszczony kryształ | `/metin respawn 550e8400...` |
| `/metin reload` | Przeładowuje konfigurację pluginu | `/metin reload` |
| `/metin stats [gracz]` | Wyświetla statystyki gracza | `/metin stats` lub `/metin stats Notch` |
| `/metin givevoucher <gracz> <mnożnik> <czas>` | Daje graczowi voucher boostujący | `/metin givevoucher Steve 2.0 3600` |
| `/metin boost global <mnożnik> <czas>` | Aktywuje globalny boost | `/metin boost global 3.0 7200` |
| `/metin boost player <mnożnik> <czas> <gracz>` | Aktywuje boost dla gracza | `/metin boost player 1.5 1800 Steve` |
| `/metin updateleaderboard` | Manualnie aktualizuje pliki rankingów | `/metin updateleaderboard` |

**Parametry:**
- `<typ>` - ID kryształu z config.yml (np. metin_common, metin_rare)
- `<id>` - UUID kryształu (z komendy /metin list)
- `<nazwa>` - Opcjonalna własna nazwa kryształu
- `<mnożnik>` - Wartość boostu (1.5 = 1.5x, 2.0 = 2x, etc.)
- `<czas>` - Czas trwania w sekundach (3600 = 1h, 7200 = 2h)

---

## 🔐 Permisje

Plugin używa prostego systemu permisji:

```
simplemetin.admin
```

**Domyślnie:** Tylko operatorzy (OP)
**Daje dostęp do:** Wszystkich komend pluginu

Możesz przypisać tę permisję grupom w swoim pluginie do permisji (LuckPerms, PermissionsEx, GroupManager).

---

## ⚙️ Wymagania

### Minimalne:
- **Minecraft:** 1.21.1 lub nowszy
- **Silnik serwera:** Spigot 1.21.1+
- **Java:** 21 lub nowszy

### Zalecane:
- **Silnik serwera:** Paper / Purpur / Pufferfish (lepsza wydajność)
- **Java:** 21
- **RAM:** Minimum 2GB dla serwera

### Opcjonalne zależności:
- **PlaceholderAPI** - Wymagane do działania placeholderów
- **Vault** - Jeśli chcesz używać komend ekonomicznych
- **EssentialsX** - Alternatywa dla Vault

---

## 📦 Instalacja

### Krok 1: Pobierz plugin
Pobierz najnowszą wersję pliku `SimpleMetin-X.X.X.jar`

### Krok 2: Zainstaluj na serwerze
1. Zatrzymaj serwer Minecraft
2. Umieść plik `.jar` w folderze `/plugins/`
3. (Opcjonalnie) Zainstaluj PlaceholderAPI jeśli chcesz używać placeholderów

### Krok 3: Uruchom serwer
Uruchom serwer. Plugin automatycznie utworzy:
- `/plugins/SimpleMetin/config.yml` - Główna konfiguracja
- `/plugins/SimpleMetin/data.yml` - Dane kryształów
- `/plugins/SimpleMetin/stats.yml` - Statystyki graczy
- `/plugins/SimpleMetin/leaderboards.yml` - Rankingi
- `/plugins/SimpleMetin/statistics.yml` - Czytelne statystyki

### Krok 4: Konfiguracja
1. Zatrzymaj serwer
2. Edytuj `config.yml` według swoich potrzeb:
   - Dostosuj HP kryształów
   - Skonfiguruj dropy i szanse
   - Ustaw czasy respawnu
   - Zmień wiadomości
3. Zapisz i uruchom serwer

### Krok 5: Stwórz pierwsze kryształy!
W grze użyj komendy:
```
/metin spawn metin_common
```

Plugin zawiera dwa pre-skonfigurowane typy kryształów:
- `metin_common` - Zwykły kryształ (50 HP, 5 min respawn)
- `metin_rare` - Rzadki kryształ (150 HP, one-time)

---

## 🎯 Przykłady Użycia

### Tworzenie Kryształów

**Podstawowy spawn:**
```
/metin spawn metin_common
```

**Spawn z własną nazwą:**
```
/metin spawn metin_rare &d&lLEGENDARNY METIN BOSSA
```

**Spawn z niestandardową nazwą i kolorami:**
```
/metin spawn metin_common &6&l⭐ &e&lEVENT METIN &6&l⭐
```

### Zarządzanie Kryształami

**Lista wszystkich kryształów:**
```
/metin list
```
Wynik:
```
Active Crystals:
- 550e8400-e29b-41d4-a716-446655440000 [ACTIVE] Type: metin_common HP: 50/50
- 550e8400-e29b-41d4-a716-446655440001 [DESTROYED] Type: metin_rare HP: 0/150
```

**Usuwanie kryształu:**
```
/metin remove 550e8400-e29b-41d4-a716-446655440000
```

**Manualny respawn:**
```
/metin respawn 550e8400-e29b-41d4-a716-446655440001
```

### System Boostów

**Globalny boost 2x na 1 godzinę:**
```
/metin boost global 2.0 3600
```
*Wszyscy gracze na serwerze otrzymają broadcast i 2x szanse na drop przez 1h*

**Globalny boost 3x na 2 godziny (Happy Hour):**
```
/metin boost global 3.0 7200
```

**Osobisty boost dla gracza:**
```
/metin boost player 1.5 1800 Steve
```
*Gracz Steve otrzyma 1.5x boost na 30 minut*

**Dawanie vouchera:**
```
/metin givevoucher Notch 2.5 3600
```
*Daje graczowi Notch voucher na 2.5x boost przez 1 godzinę*

### Statystyki

**Własne statystyki:**
```
/metin stats
```

**Statystyki innego gracza:**
```
/metin stats Notch
```

Wynik:
```
Notch's Stats:
Crystals Destroyed: 47
Total Damage: 2350
Items Received: 156
Money Earned: $12500
```

### Aktualizacja Rankingów

**Manualna aktualizacja:**
```
/metin updateleaderboard
```
*Natychmiastowo aktualizuje pliki leaderboards.yml i statistics.yml*

---

## 🔧 Konfiguracja

### Podstawowa Struktura config.yml

```yaml
settings:
  save-interval: 300  # Auto-zapis co 5 minut
  hologram-update-interval: 20  # Aktualizacja hologramów (ticki)
  actionbar-enabled: true  # Włącz ActionBar
  bossbar-enabled: true  # Włącz Boss Bary
  bossbar-range: 30.0  # Zasięg Boss Bara
  hit-cooldown: 1.0  # Cooldown między uderzeniami (sekundy)
  leaderboard-update-interval: 6000  # Aktualizacja rankingów (ticki)

crystals:
  metin_common:  # ID kryształu
    display-name: "&e&lCommon Metin"
    type: respawn  # respawn lub one-time
    max-hp: 50
    damage-per-hit: 1
    respawn-time: 300  # sekundy
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
        chance: 5.0  # 5% szans
        name: "&bMetin Diamond"
    death-drops:
      drop1:
        item: DIAMOND
        amount: 5
        chance: 100.0  # 100% szans
```

### Przykładowe Konfiguracje

**Kryształ Boss (Trudny, Rzadki):**
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

**Kryształ Farmowy (Łatwy, Częsty Respawn):**
```yaml
crystals:
  metin_farm:
    display-name: "&a&lFarm Metin"
    type: respawn
    max-hp: 30
    damage-per-hit: 1
    respawn-time: 60  # 1 minuta
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

## 🛠️ Funkcje Techniczne

### Thread-Safety
- Wszystkie operacje są thread-safe
- Używa `ConcurrentHashMap` dla bezpieczeństwa wielowątkowego
- Asynchroniczne operacje I/O (zapis plików)
- Brak lagów podczas zapisu danych

### Event API dla Deweloperów

Plugin udostępnia custom event `MetinCrystalDestroyedEvent`:

```java
@EventHandler
public void onMetinDestroyed(MetinCrystalDestroyedEvent event) {
    CrystalData crystal = event.getCrystalData();
    Player killer = event.getKiller();
    List<DropItem> drops = event.getDrops();

    // Twoja logika...
}
```

### NBT/Persistent Data Container
- Vouchery używają PDC (bezpieczne NBT)
- Niemożliwe do sfałszowania lub duplikacji
- Zachowują dane między restartami

### Optymalizacja Wydajności
- Lazy loading hologramów
- Efficient update cycles
- Minimalne użycie CPU
- Optymalne dla serwerów 100+ graczy

---

## 📝 FAQ (Często Zadawane Pytania)

**Q: Czy plugin działa z Spigot?**
A: Tak, ale zalecamy Paper/Purpur dla lepszej wydajności.

**Q: Czy mogę mieć więcej niż 2 typy kryształów?**
A: Tak! Możesz dodać dowolną ilość typów w config.yml.

**Q: Czy statystyki są zachowywane po restarcie?**
A: Tak, wszystkie dane są auto-zapisywane co 5 minut i przy wyłączeniu serwera.

**Q: Czy mogę wyłączyć Boss Bary?**
A: Tak, ustaw `settings.bossbar-enabled: false` w config.yml.

**Q: Jak zmienić szanse na drop?**
A: Edytuj wartość `chance` w sekcji dropów (0-100%).

**Q: Czy boosty stackują się?**
A: Tak, możesz skonfigurować czy personal + global się łączą w sekcji `boosts`.

**Q: Czy plugin wspiera ekonomię?**
A: Tak, przez komendy możesz używać Vault/EssentialsX do dawania pieniędzy.

**Q: Jak używać placeholderów?**
A: Zainstaluj PlaceholderAPI i używaj placeholderów w innych pluginach (np. %metin_crystals_destroyed%).

**Q: Czy mogę zmienić wygląd voucherów?**
A: Tak, pełna konfiguracja w sekcji `boosts.personal.voucher`.

**Q: Jak zrobić backup danych?**
A: Skopiuj folder `/plugins/SimpleMetin/` - zawiera wszystkie dane.

---

## 🐛 Zgłaszanie Błędów

Znalazłeś bug? Pomóż ulepszyć plugin!

1. Sprawdź czy problem nie został już zgłoszony
2. Zbierz informacje:
   - Wersja pluginu
   - Wersja serwera (Paper/Spigot)
   - Wersja Javy
   - Error log (jeśli występuje)
   - Kroki do reprodukcji błędu
3. Zgłoś w sekcji **Issues** lub **Discussion**

---

## 💡 Propozycje Funkcji

Masz pomysł na nową funkcję? Daj znać!

- Opisz dokładnie jak ma działać
- Wyjaśnij dlaczego byłaby przydatna
- Zostaw komentarz w sekcji **Discussion**

---

## 🔄 Changelog

### Version 1.0.0
- Początkowe wydanie
- System kryształów (Respawn + One-Time)
- System dropów (Hit + Death)
- System boostów (Personal + Global)
- System statystyk i rankingów TOP 100
- Integracja z PlaceholderAPI (40+ placeholderów)
- Boss Bary dla kryształów i boostów
- System voucherów
- Auto-save system
- Event API dla deweloperów

---

## 📄 Licencja

Plugin jest własnością autora. Zakup daje prawo do:
- Używania na własnym serwerze
- Modyfikacji konfiguracji
- Prywatnej edycji kodu (bez redistrybucji)

Zabronione:
- Odsprzedaż pluginu
- Publiczne udostępnianie pliku .jar
- Publikowanie zmodyfikowanego kodu
- Claimowanie autorstwa

---

## ⭐ Podoba Ci się plugin?

Jeśli **SimpleMetin** spełnia Twoje oczekiwania:

- Zostaw **pozytywną ocenę** ⭐⭐⭐⭐⭐
- Podziel się **screenshotami** z Twojego serwera
- Napisz **review** opisujący Twoje doświadczenia
- **Polecaj** znajomym właścicielom serwerów

To motywuje do dalszego rozwoju i dodawania nowych funkcji!

---

## 📧 Kontakt

- **SpigotMC**: [Profil Autora]
- **Discord**: [Twój Discord Tag]
- **Support**: Przez sekcję Discussion na SpigotMC

---

## 🎉 Podziękowania

Dziękuję wszystkim testerom i graczom za feedback oraz pomoc w rozwoju pluginu!

**Miłej zabawy z SimpleMetin!** 🎮✨
