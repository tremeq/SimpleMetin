<p align="center">
  <img src="https://i.imgur.com/MHK0Eqw.png" alt="SimpleMetin - Advanced Metin Crystal System" width="820">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.1.0-blue.svg" alt="Version 1.1.0">
  <img src="https://img.shields.io/badge/paper-1.21.1-green.svg" alt="Paper 1.21.1">
  <img src="https://img.shields.io/badge/java-21-orange.svg" alt="Java 21">
</p>

<p align="center"><a href="README.md">English</a> | <b>Polski</b></p>

**Metiny, nagrody i strefy eventowe dla Twojego serwera Minecraft.**

**Wersja:** 1.1.0 · **Autor:** TremeQu · **Platforma testowa:** Paper 1.21.1 · **Java:** 21

## 📢 O SimpleMetin

<img src="https://www.spigotmc.org/attachments/crystal-1-png.925536/" alt="Kryształ SimpleMetin" width="140" align="right">

SimpleMetin przenosi mechanikę metinów do Minecrafta. Gracze atakują kryształy z własnym HP, zdobywają nagrody i rozwijają statystyki, a administrator decyduje o sile metinów, dropach oraz miejscach ich pojawiania się.

Plugin sprawdzi się na arenach, w strefach eventowych i jako dodatkowa aktywność na mapie survivalowej. Łączy ręcznie ustawiane metiny, automatyczne spawny, hologramy, boosty i rankingi w jednym systemie.

**Znalazłeś błąd lub masz pomysł?** Opisz go w [Issues](https://github.com/tremeq/SimpleMetin/issues), dołączając wersję serwera, konfigurację i sposób odtworzenia problemu.

## 📦 Wymagania i zależności

- **Java 21** — wymagana.
- **Paper** — plugin korzysta z API Paper; kompilacja i testy były wykonywane na **1.21.1**.
- **PlaceholderAPI** — opcjonalne, udostępnia statystyki i rankingi innym pluginom.
- **Plugin ekonomii** — potrzebny tylko przy nagrodach korzystających z komend takich jak `eco give`.

SimpleMetin działa bez PlaceholderAPI i ekonomii. Nie wymaga osobnego pluginu do hologramów.

---

## 💎 Główne funkcje

<img src="https://i.imgur.com/qS6Sz1S.png" alt="Funkcje" width="100%">

- **Metiny z własnym HP** — konfigurowalne typy, nazwy, zdrowie oraz efekty zniszczenia.
- **Dwa tryby kryształów** — jednorazowe metiny i metiny odradzające się po określonym czasie.
- **Siła gracza** — osobna wartość obrażeń zarządzana przez `/ma`, z zapisem danych i rankingiem.
- **Nagrody za uderzenie i zniszczenie** — przedmioty oraz komendy konsolowe z indywidualnymi szansami.
- **Rozbudowane dropy** — losowe ilości, pule z wagami, enchanty, custom model data i przedmioty zapisane z ręki wraz z NBT.
- **Automatyczne spawny** — losowe miejsca w strefach lub ustalone punkty, wagi typów, limity i czas życia kryształów.
- **Ogłoszenia eventowe** — zapowiedzi pojawienia się metinu, komunikaty o spawnie, zniszczeniu i zniknięciu.
- **Hologramy TextDisplay** — jedna encja na cały hologram, własne kolory, tło, zasięg i odliczanie respawnu.
- **BossBary i ActionBar** — bieżące HP, cooldown uderzeń i informacje o aktywnych boostach.
- **Boosty dropu** — globalne i personalne mnożniki szans, konfigurowalne łączenie bonusów oraz vouchery.
- **Statystyki i rankingi** — zniszczone metiny, obrażenia, otrzymane przedmioty i kwoty rozpoznane z komend ekonomii.
- **Wygodna administracja** — krótkie ID typu `common-1`, informacje o metinie, teleportacja i wybór najbliższego kryształu.
- **Wiadomości EN/PL** — wybór języka, własne teksty oraz obsługa kolorów `&`, HEX i MiniMessage.
- **Uzupełnianie konfiguracji** — dopisywanie brakujących globalnych opcji i wiadomości EN/PL z zachowaniem własnych ustawień.
- **Trwały zapis danych** — stan metinów, siła, statystyki i aktywne boosty przywracane po restarcie.

Nagrody za zniszczenie otrzymuje gracz zadający ostatni cios. Boosty zwiększają szanse dropu, a nadmiar przedmiotów przy pełnym ekwipunku wypada na ziemię.

### 🖼️ Podgląd w grze

<table>
  <tr>
    <td align="center"><img src="https://www.spigotmc.org/attachments/upload_2025-10-19_20-36-27-png.925551/" alt="Common Metin z hologramem" width="280"><br><sub>Common Metin — hologram z HP</sub></td>
    <td align="center"><img src="https://www.spigotmc.org/attachments/upload_2025-10-19_20-37-35-png.925552/" alt="Rare Metin" width="280"><br><sub>Rare Metin — trudniejszy kryształ jednorazowy</sub></td>
  </tr>
  <tr>
    <td colspan="2" align="center"><img src="https://www.spigotmc.org/attachments/upload_2025-10-19_20-1-35-png.925537/" alt="BossBar kryształu" width="560"><br><sub>BossBar z aktualnym HP kryształu</sub></td>
  </tr>
</table>

---

## ⚡ Szybki start

1. Zbuduj plugin poleceniem `mvn clean package` i umieść `target/SimpleMetin-1.1.0.jar` w katalogu `plugins/`.
2. Uruchom serwer, aby wygenerować konfigurację.
3. Ustaw własne typy i nagrody w `plugins/SimpleMetin/config.yml`.
4. Wykonaj `/metin spawn metin_common`, aby postawić pierwszy kryształ.
5. Sprawdź swoje statystyki przez `/metin stats`.

Domyślny język to **angielski**. Aby włączyć polski, ustaw `language: pl` i użyj `/metin reload`.

Przykładowe spawnery w `spawners.yml` są wyłączone. Ustaw strefę lub dodaj punkty przed ich włączeniem.

---

## ⌨️ Komendy

<img src="https://i.imgur.com/A2I8T4d.png" alt="Komendy" width="100%">

### Dla graczy

- `/metin stats` — własne statystyki, siła i miejsca w rankingach.
- `/metin` — pomoc zawierająca komendy dostępne dla danego gracza.

### Metiny i konfiguracja

- `/metin spawn <typ> [id|auto] [nazwa]` — tworzy kryształ na Twojej pozycji.
- `/metin remove <id|nearest>` — usuwa wskazany lub najbliższy kryształ.
- `/metin list` — wyświetla metiny i ich stan.
- `/metin info [id|nearest]` — pokazuje szczegóły kryształu.
- `/metin tp <id>` — teleportuje obok metinu.
- `/metin respawn <id|nearest>` — przyspiesza odrodzenie zniszczonego metinu.
- `/metin reload` — przeładowuje konfigurację i pliki wiadomości.
- `/metin stats <gracz>` — pokazuje statystyki wskazanej osoby.
- `/metin updateleaderboard` — odświeża eksporty rankingów.

### Boosty i nagrody

- `/metin boost global <mnożnik> <sekundy>` — aktywuje boost dla całego serwera.
- `/metin boost player <mnożnik> <sekundy> <gracz>` — aktywuje boost personalny.
- `/metin givevoucher <gracz> <mnożnik> <sekundy>` — wydaje voucher aktywowany prawym kliknięciem.
- `/metin item save <klucz>` — zapisuje przedmiot z głównej ręki jako wzorzec dropu.
- `/metin item give <klucz> [gracz] [ilość]` — wydaje zapisany przedmiot.
- `/metin item list` oraz `/metin item remove <klucz>` — zarządza zapisanymi przedmiotami.
- `/metin adddrop <typ> <hit|death> <szansa> [ilość] [klucz]` — dodaje przedmiot z ręki do nagród wybranego typu.

### Spawnery i siła graczy

- `/metin spawner list` — pokazuje stan spawnerów.
- `/metin spawner spawn <spawner>` — wymusza próbę spawnu z pominięciem limitów automatycznych.
- `/metin spawner point add <spawner>` — zapisuje aktualną pozycję jako punkt spawnu.
- `/metin spawner point list <spawner>` — wyświetla punkty.
- `/metin spawner point remove <spawner> <numer>` — usuwa punkt.
- `/ma add <gracz> <wartość>` i `/ma set <gracz> <wartość>` — zmienia siłę gracza.
- `/ma get <gracz>` i `/ma reset <gracz>` — sprawdza siłę lub przywraca domyślne 10.

**Uprawnienia:** własne statystyki są domyślnie dostępne dla wszystkich. `simplemetin.admin` daje pełny dostęp, a poszczególne funkcje mają osobne permisje. [Pełna lista uprawnień →](docs/GUIDE-PL.md#komendy-i-uprawnienia)

---

## 🧩 Placeholdery

### PlaceholderAPI

Wymaga [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). SimpleMetin rejestruje dwa rozszerzenia: **`metin`** (statystyki, boosty, rankingi) oraz **`simplemetin`** (siła gracza). Liczby zawsze mają kropkę (`2.5`), niezależnie od języka systemu serwera.

#### Statystyki gracza

| Placeholder | Zwraca | Bez danych |
|---|---|---|
| `%metin_crystals_destroyed%` | Liczbę metinów zniszczonych przez gracza. | `0` |
| `%metin_total_damage%` | Faktycznie zadane obrażenia (nadwyżka ponad pozostałe HP nie jest liczona). | `0` |
| `%metin_items_received%` | Przedmioty otrzymane z dropów, łącznie z tymi, które wypadły na ziemię przy pełnym ekwipunku. | `0` |
| `%metin_money_earned%` | Pieniądze z komend nagród `eco give <gracz> <kwota>`, które wykonały się poprawnie. | `0` |
| `%metin_type_<typ>%` | Liczbę zniszczonych metinów danego typu, np. `%metin_type_metin_common%`, `%metin_type_metin_rare%`. | `0` |

#### Boosty

| Placeholder | Zwraca | Bez danych |
|---|---|---|
| `%metin_boost_multiplier%` | Łączny mnożnik dropu (globalny + personalny zgodnie z `stacking-mode`), np. `3.0`. | `1.0` (również dla graczy offline) |
| `%metin_boost_active%` | `true`, gdy łączny mnożnik jest większy niż `1.0`. | `false` |
| `%metin_personal_boost_multiplier%` | Mnożnik boosta personalnego gracza. | `1.0` |
| `%metin_personal_boost_time%` | Pozostały czas boosta personalnego: `45s`, `2m 5s`, `1h 2m 3s`. | `0s` |
| `%metin_global_boost_multiplier%` | Mnożnik boosta globalnego. | `1.0` |
| `%metin_global_boost_time%` | Pozostały czas boosta globalnego: `45s`, `2m 5s`, `1h 2m 3s`. | `0s` |

#### Rankingi (miejsca 1–100)

| Placeholder | Zwraca | Puste miejsce |
|---|---|---|
| `%metin_destroyed_top_<N>%` | Liczbę zniszczonych metinów gracza na miejscu N. | `0` |
| `%metin_destroyed_top_<N>_name%` | Nick gracza na miejscu N (zniszczone metiny). | `-` |
| `%metin_damage_top_<N>%` | Obrażenia zadane przez gracza na miejscu N. | `0` |
| `%metin_damage_top_<N>_name%` | Nick gracza na miejscu N (zadane obrażenia). | `-` |
| `%metin_items_top_<N>%` | Przedmioty otrzymane przez gracza na miejscu N. | `0` |
| `%metin_items_top_<N>_name%` | Nick gracza na miejscu N (otrzymane przedmioty). | `-` |
| `%metin_money_top_<N>%` | Pieniądze zarobione przez gracza na miejscu N. | `0` |
| `%metin_money_top_<N>_name%` | Nick gracza na miejscu N (zarobione pieniądze). | `-` |

`<N>` to liczba od `1` do `100`, np. `%metin_damage_top_1_name%`. Placeholdery rankingów działają również bez gracza (globalne hologramy, tablista). Rankingi są przechowywane w pamięci przez `settings.top-cache-seconds` (domyślnie 5 s). Miejsca spoza listy, `0` i liczby ujemne zwracają `0` / `-`; gracz bez znanego nicku jest wyświetlany jako `Unknown`.

#### Siła gracza

| Placeholder | Zwraca | Bez danych |
|---|---|---|
| `%simplemetin_damage%` | Siłę gracza (obrażenia na uderzenie, zarządzane przez `/ma`). | `10` (domyślna siła) |
| `%simplemetin_top_name_<N>%` | Nick gracza na miejscu N w top 10 siły. | `-` |
| `%simplemetin_top_value_<N>%` | Siłę gracza na miejscu N w top 10 siły. | `0` |

`<N>` to liczba od `1` do `10`. Top 10 siły jest odbudowywane przy starcie serwera i co 5 minut.

**Przykład** (plugin scoreboardu / hologramów):

```
&6&lMETIN TOP
&e1. &f%metin_destroyed_top_1_name% &7- &e%metin_destroyed_top_1%
&e2. &f%metin_destroyed_top_2_name% &7- &e%metin_destroyed_top_2%
&e3. &f%metin_destroyed_top_3_name% &7- &e%metin_destroyed_top_3%
&7Twoje metiny: &e%metin_crystals_destroyed% &7| Siła: &e%simplemetin_damage%
&7Boost: &e%metin_boost_multiplier%x &7(%metin_global_boost_time%)
```

### Placeholdery wewnętrzne

Działają we własnych plikach SimpleMetin i nie wymagają PlaceholderAPI. Placeholdery PlaceholderAPI **nie są** przetwarzane w wiadomościach ani hologramach SimpleMetin.

| Gdzie | Placeholdery |
|---|---|
| Linie hologramu — `crystals.<typ>.hologram.lines` w `config.yml` | `%hp%` aktualne HP, `%max_hp%` maksymalne HP, `%id%` ID kryształu |
| Komendy nagród — `hit-commands`, `death-commands`, wpisy `command:` w dropach i pulach | `%player%` nick nagradzanego gracza |

### Placeholdery wiadomości

Dostępne w `messages-en.yml` i `messages-pl.yml`. Każda wiadomość obsługuje też kolory `&`, HEX i MiniMessage; do większości wiadomości na czacie dodawany jest `prefix`.

| Klucz(e) wiadomości | Placeholdery |
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
| `info` | `%id%`, `%type%`, `%display_name%`, `%status%`, `%hp%`, `%max_hp%`, `%world%`, `%x%`, `%y%`, `%z%`, `%respawn%`, `%spawner%`, `%expires%` |
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

Wiadomości bez placeholderów: `prefix`, `no-permission`, `player-only`, `reloaded`, `list-empty`, `status-active`, `status-destroyed`, `status-pending`, `none`, `inventory-full`, `stats-types-header`, `boost-expired`, `global-boost-expired`, `boost-already-active`, `boost-invalid-type`, `voucher-received`, `leaderboard-updating`, `leaderboard-updated`, `spawner-list-header`, `spawner-state-enabled`, `spawner-state-disabled`, `item-hand-empty`, `items-list-empty`, `drop-invalid-section` oraz linie `help-*`.

---

## 🔧 Nowości i poprawki

### Wersja 1.1.0

**Nowe funkcje:**

- ✅ Automatyczne dopisywanie brakujących opcji globalnych i wiadomości.
- ✅ Hologramy TextDisplay z konfigurowalnym wyglądem i zasięgiem.
- ✅ Osobne pliki `messages-en.yml` oraz `messages-pl.yml`.
- ✅ Statystyki dla graczy i oddzielne uprawnienia administracyjne.
- ✅ Krótkie ID, własne nazwy, `info`, `tp` i obsługa `nearest`.
- ✅ Spawnery losowe i punktowe z limitami, czasem życia oraz ogłoszeniami.
- ✅ Zakresy ilości dropów, pule wagowe i zapis custom itemów z ręki.

**Poprawki:**

- ✅ Sprawdzenie limitów również po zakończeniu ładowania chunków.
- ✅ Anulowanie oczekujących spawnów po reloadzie i wyłączeniu pluginu.
- ✅ Ochrona błędnego YAML-a przed nadpisaniem przez aktualizator.
- ✅ Zachowanie nadmiaru przedmiotów przy pełnym ekwipunku w `/metin item give`.
- ✅ Walidacja szans dropu w `/metin adddrop`.

**Migracja:** istniejące UUID są zamieniane na krótkie ID, a stare wiadomości przenoszone do aktywnego pliku językowego. Trzeci argument `/metin spawn` oznacza teraz ID — do nadania samej nazwy użyj `auto`.

---

## 📚 Dokumentacja

Przewodnik jest dostępny po [polsku](docs/GUIDE-PL.md) i [angielsku](docs/GUIDE.md). Raport testów jest obecnie dostępny **po polsku**.

- [📖 Pełny przewodnik](docs/GUIDE-PL.md) — szczegółowe działanie pluginu i konfiguracja.
- [⚙️ Konfiguracja i języki](docs/GUIDE-PL.md#konfiguracja-języki-i-aktualizacja) — aktualizacja plików, tłumaczenia i formatowanie.
- [💎 Hologramy i BossBary](docs/GUIDE-PL.md#hologramy-i-bossbary) — wygląd, zasięg i placeholdery HP.
- [🌍 Automatyczne spawny](docs/GUIDE-PL.md#automatyczne-spawny) — strefy, punkty, limity i ogłoszenia.
- [🎁 Dropy i custom itemy](docs/GUIDE-PL.md#dropy-pule-i-własne-przedmioty) — szanse, wagi i przykłady nagród.
- [🚀 Boosty i vouchery](docs/GUIDE-PL.md#boosty-i-vouchery) — mnożniki, łączenie bonusów i aktywacja.
- [🔑 Komendy i uprawnienia](docs/GUIDE-PL.md#komendy-i-uprawnienia) — pełna lista permisji.
- [📊 Statystyki i placeholdery](docs/GUIDE-PL.md#statystyki-i-placeholdery) — wartości graczy i rankingi TOP.
- [💾 Zapis danych](docs/GUIDE-PL.md#pliki-i-zapis-danych) — pliki pluginu, autozapis i restart.
- [🧪 Raport testów](TEST_REPORT.md) — zakres weryfikacji wersji 1.1.0.

Przykładowe pliki: [config.yml](src/main/resources/config.yml) · [spawners.yml](src/main/resources/spawners.yml) · [messages-en.yml](src/main/resources/messages-en.yml) · [messages-pl.yml](src/main/resources/messages-pl.yml)

---

## 🔌 Integracje

- **PlaceholderAPI** — placeholdery `%metin_...%` i `%simplemetin_...%` do scoreboardów, hologramów i innych miejsc obsługiwanych przez zewnętrzne pluginy.
- **Komendy konsolowe** — nagrody przez pluginy ekonomii, kluczy, rang i innych systemów, z podstawieniem `%player%`.
- **Custom itemy** — zapis danych NBT i komponentów przedmiotu; jego specjalne działanie nadal wymaga źródłowego pluginu.
- **API i tagi encji** — zdarzenie `MetinCrystalDestroyedEvent` oraz tagi ułatwiające rozpoznawanie metinów.

---

**Sprawdzono na Paper 1.21.1:** 123 testy jednostkowe i 124 testy E2E zakończone powodzeniem. Szczegóły środowiska i zakres sprawdzeń znajdują się w [raporcie testów](TEST_REPORT.md).
