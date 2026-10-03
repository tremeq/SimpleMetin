# SimpleMetin 1.1.0 — pełny przewodnik

[English](GUIDE.md) | **Polski**

[← Wróć do README](../README-PL.md)

Plugin TremeQu do metinów, czyli kryształów z własnym HP, nagrodami, hologramami, statystykami i boostami dropu. Administrator ustawia kryształy ręcznie lub uruchamia ich automatyczne pojawianie się w strefach i ustalonych punktach.

Wymagania: **Java 21 i Paper 1.21.1** (wersja używana do kompilacji i testów). PlaceholderAPI jest opcjonalne. Plugin korzysta z API Paper; zgodność z innymi wersjami i forkami wymaga osobnego sprawdzenia.

## Instalacja i pierwsze uruchomienie

1. Wykonaj `mvn clean package` i skopiuj `target/SimpleMetin-1.1.0.jar` do `plugins/` serwera.
2. Uruchom serwer. Konfiguracja powstanie w `plugins/SimpleMetin/`.
3. Domyślny język to **angielski**. Aby włączyć polski, ustaw `language: pl` w `config.yml` i wykonaj `/metin reload`.
4. Jako administrator wykonaj `/metin spawn metin_common`. Kryształ pojawi się na Twojej pozycji z ID `common-1`.
5. Gracze mogą go uderzać bez dodatkowych uprawnień. `/metin stats` pokazuje własne statystyki także zwykłym graczom.

Przykładowe spawnery są domyślnie **wyłączone**. Włącz je po ustawieniu strefy lub punktów.

## Walka i cykl życia

Przyjęte uderzenie odejmuje HP równe sile gracza z `players.yml`. Domyślna siła wynosi 10, minimum to 1. Standardowe obrażenia broni nie wpływają na siłę uderzenia. Cooldown jest wspólny dla uderzeń danego gracza w metiny; domyślnie wynosi sekundę (`settings.hit-cooldown`).

Przy uderzeniu plugin losuje `hit-drops`, `hit-commands` i `hit-pools`. Ostatnie uderzenie dodatkowo losuje `death-drops`, `death-commands` i `death-pools`. Nagrody za zniszczenie dostaje **gracz zadający ostatni cios**, bez podziału między uczestników. Przy pełnym ekwipunku nadmiar przedmiotów wypada obok kryształu i pojawia się komunikat.

Zniszczenie uruchamia efekty, aktualizuje statystyki i wysyła `MetinCrystalDestroyedEvent`. Statystyka obrażeń liczy faktycznie odjęte HP, bez nadwyżki ostatniego ciosu.

| Utworzenie / typ | Po zniszczeniu |
|---|---|
| Ręcznie, `type: respawn` | Hologram odlicza czas; odnowienie po `respawn-time` sekundach. |
| Ręcznie, `type: one-time` | Trwałe usunięcie wpisu i encji. |
| Ze spawnera, dowolny typ | Trwałe usunięcie; kolejne kryształy tworzy spawner. |

Vanilla damage, TNT, ogień i pociski mobów nie niszczą metinu. `settings.projectile-hits: true` włącza trafienia pociskami graczy z tą samą siłą i cooldownem; obsłużony pocisk jest usuwany. Domyślnie opcja jest wyłączona.

Encje metinów i hologramów są nietrwałe (`setPersistent(false)`). Po wczytaniu chunka plugin odtwarza je z własnych danych; wyładowanie usuwa encje i zachowuje HP. Usuwane są pozostałości po starszych wersjach, także stare hologramy ArmorStand. Wpisy z niedostępnego świata lub nieznanym typem pozostają w `data.yml` i na liście jako `PENDING`; ładowanie świata lub `/metin reload` ponawia próbę ich odtworzenia.

## Konfiguracja, języki i aktualizacja

Przy starcie i `/metin reload` brakujące globalne opcje `config.yml` oraz klucze dołączonych plików `messages-en.yml` i `messages-pl.yml` są dopisywane z komentarzami. Istniejące wartości i komentarze pozostają. Aktualizator nie nadpisuje pliku z błędną składnią YAML.

Sekcja `crystals` należy do administratora: istniejące typy, dropy i hologramy nie są zastępowane przykładami z JAR-a, a usunięte typy nie wracają. Nowe ustawienia wewnątrz własnych typów dopisuje się samodzielnie; kod używa wartości domyślnych. Definicje spawnerów także należą do administratora; przykładowy `spawners.yml` jest kopiowany tylko przy braku pliku. `items.yml` zawiera zapisane przedmioty, bez automatycznego uzupełniania.

Przy migracji z 1.0.0 stara sekcja `messages`, tytuły BossBarów boostów oraz nazwa i lore vouchera przechodzą z `config.yml` do aktywnego pliku językowego. Własne teksty zachowują treść; migracja ich nie tłumaczy.

```yaml
language: pl  # en -> messages-en.yml, pl -> messages-pl.yml
```

Możesz utworzyć np. `messages-de.yml` i ustawić `language: de`. Brakujące wiadomości własnego języka korzystają z angielskich wartości domyślnych w pamięci. Automatyczne dopisywanie dotyczy dołączonych języków EN/PL.

Obsługiwane są kody `&`, kolory `&#RRGGBB` i MiniMessage, np. `<gradient:gold:yellow>Metin</gradient>`. Pusty tekst `""` wyłącza wiadomość; lista tekstów tworzy wiele linii. Placeholdery dla wiadomości opisują komentarze w plikach językowych. Nazwy typów, hologramy i teksty dropów pozostają przy definicjach w `config.yml`.

`/metin reload` odświeża konfigurację, język, zapisane przedmioty, tabele dropów, hologramy, BossBary, boosty i spawnery. Nowe `max-hp` zachowuje pełne zdrowie pełnego kryształu; uszkodzony zachowuje HP ograniczone nowym maksimum. Timery spawnerów zostają, a oczekujące wyszukiwania lokalizacji z poprzedniej konfiguracji są anulowane.

### Ustawienia globalne

| Opcja `settings.*` | Domyślna wartość | Znaczenie |
|---|---|---|
| `save-interval` | 300 | Sekundy między zapisami kryształów, statystyk i boostów. |
| `hologram-update-interval` | 20 | Ticki między aktualizacjami hologramów. |
| `actionbar-enabled` | true | Komunikaty HP i cooldownu na ActionBar. |
| `bossbar-enabled` | true | BossBary kryształów. |
| `bossbar-range` | 30.0 | Zasięg BossBara w blokach. |
| `hit-cooldown` | 1.0 | Sekundy między uderzeniami; 0 wyłącza cooldown. |
| `leaderboard-update-interval` | 6000 | Ticki między eksportami rankingów (6000 = 5 minut). |
| `top-cache-seconds` | 5 | Cache TOP statystyk; 0 przelicza każde zapytanie. |
| `projectile-hits` | false | Trafienia pociskami graczy. |
| `nearest-range` | 10.0 | Zasięg szukania najbliższego metinu. |

Typy definiuje sekcja `crystals.<typ>`: `display-name`, `type`, `max-hp`, `respawn-time`, `show-actionbar`, `show-bossbar`, `hologram` oraz nagrody. `death-effects` zawiera `particle`, `sound`, `volume` i `pitch`. Stare nazwy cząsteczek, np. `EXPLOSION_HUGE`, mają obsługiwane odpowiedniki (`EXPLOSION_EMITTER`). Dawne pole `damage-per-hit` jest ignorowane — siła pochodzi z danych gracza.

## Hologramy i BossBary

Cały hologram korzysta z **jednej encji TextDisplay**, niezależnie od liczby linii. Tekst jest wysyłany ponownie po zmianie HP lub odliczania. Opcje pod `crystals.<typ>.hologram`:

| Opcja | Znaczenie |
|---|---|
| `enabled` | Włącza hologram i odliczanie respawnu. |
| `lines` | Linie z placeholderami `%hp%`, `%max_hp%`, `%id%`. |
| `height` | Wysokość nad kryształem, domyślnie 2.3 bloku. |
| `range` | Zasięg przekazany do TextDisplay, np. 32 bloki; rendering zależy też od klienta. |
| `background` | `"#AARRGGBB"`, `"#RRGGBB"` albo `"default"`; `"#00000000"` oznacza przezroczystość. |
| `shadow` | Cień tekstu, domyślnie true. |
| `see-through` | Widoczność przez bloki, domyślnie false. |

BossBar kryształu pokazuje nazwę i HP graczom w zasięgu. Kontrolują go opcje globalne i `show-bossbar` danego typu. ActionBar wyświetla HP i cooldown; ma przełącznik globalny oraz `show-actionbar` danego typu. Tytuł odliczania znajduje się w wiadomości `respawn-countdown`.

## Krótkie ID

`/metin spawn metin_common` tworzy `common-1`, następny `common-2` itd. Wolne numery mogą być ponownie używane. Własne ID mają 1–32 znaki: małe litery, cyfry, `_`, `-`. Argumenty `auto` i `nearest` są zarezerwowane w komendzie spawn.

```text
/metin spawn metin_common boss &cBoss areny
/metin spawn metin_common auto &eMetin przy spawnie
/metin info boss
/metin tp boss
/metin remove nearest
```

Stare UUID są przy wczytywaniu zamieniane na krótkie ID z zachowaniem pozycji, HP i respawnu. `/metin list` sortuje numery naturalnie. `nearest` szuka najbliższego metinu w `settings.nearest-range`. `/metin info` bez argumentu także używa najbliższego metinu, nie celownika.

**Zmiana składni z 1.0.0:** trzeci argument `spawn` oznacza teraz ID. Aby podać samą nazwę, wstaw przed nią `auto`.

## Automatyczne spawny

Ustawienia wspólne w `config.yml`:

```yaml
auto-spawn:
  enabled: true
  max-alive-total: 10
  min-players-online: 1
```

Limit globalny obejmuje tylko kryształy ze spawnerów, także w wyładowanych chunkach; nie obejmuje ręcznie ustawionych. `max-alive-total: 0` znosi limit globalny. Limit pojedynczego spawnera to `max-alive`; tu `0` blokuje automatyczny spawn.

Próby odbywają się co `interval` sekund, także z zakresem, np. `"600-900"`. Po osiągnięciu limitu następna próba czeka kolejny interwał. Brak wymaganej liczby graczy lub globalne wyłączenie wstrzymuje próby. Limity są ponownie sprawdzane po zakończeniu ładowania chunka, więc równoległe wyszukiwania miejsc ich nie przekraczają.

Przykład `spawners.yml`:

```yaml
spawners:
  las:
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

`mode: random` wybiera miejsca w kole albo prostokącie. Dla prostokąta ustaw `shape: rectangle` i `min-x`, `min-z`, `max-x`, `max-z`. Chunki są ładowane asynchronicznie, a sprawdzenie gruntu i tworzenie encji odbywa się na głównym wątku.

`min-y`/`max-y` ograniczają wysokość najwyższego bloku powierzchni — **nie wyszukują jaskiń pod dachem Netheru**. Miejsce wymaga stałego gruntu i wolnej przestrzeni nad nim. `types` to wagi losowania istniejących typów, a `min-distance` oznacza odstęp od innych metinów.

`lifetime` liczy sekundy od utworzenia. Kryształ znika po tym czasie również wtedy, gdy został uszkodzony; `0` wyłącza znikanie. Termin zapisuje się w `data.yml`, więc upływ czasu offline też się liczy. Wygaszanie istniejących kryształów działa również po wyłączeniu automatycznych spawnów.

Dla ustalonych miejsc wybierz `mode: points`. Ustaw bezpieczeństwo tych miejsc samodzielnie; tryb nie wyszukuje gruntu. Punkty zapisują się w liście `points` jako `"world x y z"`:

```text
/metin spawner point add arena
/metin spawner point list arena
/metin spawner point remove arena 1
```

Komenda centruje punkt w bloku. Zajęte miejsca są pomijane; minimalny odstęp w trybie punktów to 1.5 bloku nawet przy `min-distance: 0`.

`announce.warning` zapowiada próbę pojawienia się, ale nie gwarantuje spawnu, jeśli później zabraknie miejsca. `announce.range: 0` wysyła ogłoszenie wszystkim; wartość dodatnia ogranicza je do okolicy kryształu. Zapowiedź przed spawnem trafia do wszystkich, ponieważ miejsce nie jest jeszcze znane. Teksty to `spawner-warning`, `spawner-spawned`, `spawner-despawned`, `spawner-destroyed` w plikach wiadomości.

`/metin spawner spawn <nazwa>` wymusza próbę niezależnie od timera, włączenia i limitów automatycznych. Wciąż wymaga poprawnego typu i wolnego miejsca. Po restarcie timer próby jest liczony od nowa; `spawn-on-start` włącza natychmiastową pierwszą próbę.

## Dropy, pule i własne przedmioty

Każdy wpis w `hit-drops` i `death-drops` jest losowany osobno. `hit-commands` i `death-commands` nadal obsługują komendy z własną szansą. Ilość jest stała albo losowana z przedziału z końcami włącznie.

```yaml
death-drops:
  diamenty:
    item: DIAMOND
    amount: "1-3"
    chance: 25.0
    name: "&bDiament metinu"
    lore: ["&7Nagroda z kryształu"]
    custom-model-data: 1001
  miecz:
    item: DIAMOND_SWORD
    chance: 5.0
    enchantments:
      sharpness: 3
      unbreaking: 2
  waluta:
    command: "eco give %player% 100"
    chance: 50.0
```

`chance` to procent 0–100. Boost mnoży szanse, maksymalnie do 100%; nie mnoży ilości przedmiotów.

Pule `hit-pools` / `death-pools` wybierają wpisy według względnych wag:

```yaml
death-pools:
  bonus:
    chance: 50
    rolls: "1-2"
    unique: true
    entries:
      zwykly:
        item: GOLD_INGOT
        amount: "2-4"
        weight: 9
      rzadki:
        custom-item: miecz_eventowy
        weight: 1
```

Najpierw losowana jest szansa całej puli, następnie liczba wyborów `rolls`. Wpis jest wybierany według `weight`, bez używania jego `chance`. `unique: true` zapobiega powtarzaniu wpisu w jednym rozliczeniu puli; po wyczerpaniu wpisów losowanie kończy się. Boost zwiększa szansę uruchomienia puli, bez zmiany wag i liczby wyborów.

Trzymając przedmiot w głównej ręce, można zapisać jego NBT/komponenty:

```text
/metin item save miecz_eventowy
/metin item give miecz_eventowy
/metin item give miecz_eventowy Steve 1
/metin item list
/metin item remove miecz_eventowy
/metin adddrop metin_rare death 25 1-3 miecz_eventowy
```

`item save` zapisuje kopię wzorca z ilością 1 bez zabierania przedmiotu. `adddrop` jednocześnie zapisuje wzorzec i dodaje `custom-item` do konfiguracji; działa od razu. Składnia: `/metin adddrop <typ> <hit|death> <szansa> [ilość] [klucz]`. Ponowne użycie istniejącego klucza zastępuje wzorzec dla wszystkich jego odwołań. Klucze mają 1–32 znaki `a-z`, `0-9`, `_`, `-`.

`item give` wydaje maksymalnie jeden stos danego przedmiotu. Przy pełnym ekwipunku upuszcza nadmiar przy graczu; nie podbija statystyk dropów. Zapis danych custom itemu nie zastępuje pluginu potrzebnego do jego specjalnego działania.

## Boosty i vouchery

Boost globalny działa dla wszystkich, personalny dla jednej osoby. Mają osobne BossBary konfigurowane w `boosts.bossbar`; tytuły znajdują się w plikach wiadomości.

```text
/metin boost global 2.0 600
/metin boost player 2.0 600 Steve
/metin givevoucher Steve 2.0 600
```

Czas podawany jest w sekundach. Voucher aktywuje się prawym kliknięciem i jest rozpoznawany po PersistentDataContainer, nie po nazwie. Materiał, połysk i custom model data są w `boosts.personal.voucher`; nazwa i lore w pliku wiadomości. Aktywny boost personalny blokuje zużycie kolejnego vouchera; komenda administratora zastępuje go z powiadomieniem.

| Ustawienia | Globalny 2× + personalny 2× |
|---|---|
| `boosts.personal.stacking-enabled: false` | 2×, większy z mnożników. |
| `stacking-enabled: true`, `boosts.global.stacking-mode: 1.0` | 3×: `1 + (global - 1) + (personal - 1)`; domyślny tryb. |
| `stacking-enabled: true`, `boosts.global.stacking-mode: 2.0` | 4×: iloczyn mnożników. |

Boosty zapisują się w `boosts.yml` i wracają po restarcie z zachowaniem terminu wygaśnięcia. Czas offline również się liczy. Wiadomości o końcu działają także przy wyłączonych BossBarach.

## Komendy i uprawnienia

`simplemetin.admin` daje dostęp do wszystkich komend (domyślnie OP). `simplemetin.stats` jest domyślnie dostępne dla każdego. Pozostałe uprawnienia poniżej są domyślnie dla OP. Pomoc i TAB uwzględniają uprawnienia.

| Komenda | Uprawnienie |
|---|---|
| `/metin stats` | `simplemetin.stats` |
| `/metin stats <gracz>` | `simplemetin.stats` i `simplemetin.stats.others` (własny nick nie wymaga `others`) |
| `/metin spawn <typ> [id\|auto] [nazwa]` | `simplemetin.spawn` |
| `/metin remove <id\|nearest>` | `simplemetin.remove` |
| `/metin list` | `simplemetin.list` |
| `/metin info [id\|nearest]` | `simplemetin.info` |
| `/metin tp <id>` | `simplemetin.tp` |
| `/metin respawn <id\|nearest>` | `simplemetin.respawn` |
| `/metin reload` | `simplemetin.reload` |
| `/metin boost global <mnożnik> <sekundy>` | `simplemetin.boost` |
| `/metin boost player <mnożnik> <sekundy> <gracz>` | `simplemetin.boost` |
| `/metin givevoucher <gracz> <mnożnik> <sekundy>` | `simplemetin.voucher` |
| `/metin updateleaderboard` (alias `refreshleaderboard`) | `simplemetin.leaderboard` |
| `/metin spawner list\|spawn\|point ...` | `simplemetin.spawner` |
| `/metin item save\|give\|remove\|list ...` | `simplemetin.items` |
| `/metin adddrop <typ> <hit\|death> <szansa> [ilość] [klucz]` | `simplemetin.items` |
| `/ma add <gracz> <wartość>` | `simplemetin.strength` |
| `/ma set <gracz> <wartość>` | `simplemetin.strength` |
| `/ma get <gracz>` | `simplemetin.strength` |
| `/ma reset <gracz>` | `simplemetin.strength` |

`/ma` to alias `/metinadmin`. Zmienia siłę, nie statystyki walki. `set` wymaga minimum 1; `add` może być ujemne, ale wynik nie spadnie poniżej 1. `reset` przywraca 10. Obsługiwani są też znani serwerowi gracze offline. Komendy wyszukujące gracza wymagają pełnego nicku.

## Statystyki i placeholdery

`/metin stats` pokazuje zniszczenia, obrażenia, przedmioty, pieniądze, siłę, pozycje rankingowe i zniszczenia według typu. Pozycje spoza TOP 100 pokazują brak miejsca w rankingu.

Pieniądze są szacowane na podstawie `eco give <gracz> <całkowita kwota>`, jeśli Bukkit zgłosi powodzenie obsługi komendy. Plugin nie weryfikuje salda i nie integruje się z Vault. Ułamkowe kwoty i inne komendy ekonomii nie są liczone. Przykładowe komendy `eco give` wymagają osobnego pluginu ekonomii.

PlaceholderAPI rejestruje dwa identyfikatory:

| Placeholder | Znaczenie |
|---|---|
| `%metin_crystals_destroyed%` | Liczba zniszczeń. |
| `%metin_total_damage%` | Faktycznie zadane obrażenia. |
| `%metin_items_received%` | Przedmioty z nagród, także upuszczone przy pełnym ekwipunku. |
| `%metin_money_earned%` | Kwoty rozpoznane z komend ekonomii. |
| `%metin_type_<typ>%` | Zniszczenia typu, np. `%metin_type_metin_common%`. |
| `%metin_boost_multiplier%` | Efektywny mnożnik. |
| `%metin_boost_active%` | `true` albo `false`. |
| `%metin_personal_boost_multiplier%` | Mnożnik personalny. |
| `%metin_personal_boost_time%` | Pozostały czas boosta personalnego. |
| `%metin_global_boost_multiplier%` | Mnożnik globalny. |
| `%metin_global_boost_time%` | Pozostały czas boosta globalnego. |
| `%metin_<kategoria>_top_<N>%` | Wynik N-tego gracza; kategorie: `destroyed`, `damage`, `items`, `money`; pozycje 1–100. |
| `%metin_<kategoria>_top_<N>_name%` | Nick na danej pozycji. |
| `%simplemetin_damage%` | Siła gracza. |
| `%simplemetin_top_name_<N>%` | Nick w TOP 10 siły. |
| `%simplemetin_top_value_<N>%` | Siła w TOP 10. |

Przykład rankingu: `&61. &f%metin_destroyed_top_1_name% &7- &e%metin_destroyed_top_1%`.

Placeholdery TOP działają też bez kontekstu gracza. TOP statystyk korzysta z `settings.top-cache-seconds` (5 sekund). TOP siły jest budowany z danych w pamięci przy uruchomieniu i co 5 minut. Eksporty `leaderboards.yml` i `statistics.yml` są odświeżane co `settings.leaderboard-update-interval` ticków oraz komendą.

## Pliki i zapis danych

| Plik w `plugins/SimpleMetin/` | Zawartość |
|---|---|
| `config.yml` | Opcje globalne, typy, dropy, hologramy i boosty. |
| `messages-en.yml`, `messages-pl.yml` | Teksty interfejsu, tytuły boostów i teksty voucherów. |
| `spawners.yml` | Strefy, punkty, interwały, limity i ogłoszenia. |
| `items.yml` | Wzorce przedmiotów w Base64 oraz czytelne opisy. |
| `data.yml` | ID, pozycja, HP, stan zniszczenia, respawn, spawner i termin zniknięcia. |
| `players.yml` | Siła graczy. |
| `stats.yml` | Statystyki walki i nagród. |
| `boosts.yml` | Aktywne boosty i terminy wygaśnięcia. |
| `leaderboards.yml` | Eksport TOP 100 kategorii statystyk. |
| `statistics.yml` | Czytelny eksport wszystkich statystyk. |

`data.yml`, `stats.yml` i `boosts.yml` zapisują się co `settings.save-interval` sekund (300) oraz przy wyłączeniu. Siła zapisuje się przy zmianach i wyjściu gracza. Nagłe zamknięcie procesu może utracić zmiany od ostatniego zapisu. Dane są przechowywane w RAM; autozapis nie usuwa ich z pamięci ani nie wczytuje ponownie z dysku.

`/metin reload` służy do konfiguracji. Ręczną edycję plików stanu wykonuj przy wyłączonym serwerze, aby bieżące dane w pamięci nie nadpisały zmian.

## Architektura i integracje

- `CrystalManager` obsługuje walkę, encje, dropy i respawn; `HologramManager` zarządza TextDisplay, a `DataHandler` zapisuje stan.
- `SpawnerManager` obsługuje strefy, punkty, limity, wygasanie i ogłoszenia.
- `DropTable` i `DropEntry` losują nagrody, `CustomItemManager` przechowuje wzorce przedmiotów.
- `MessageManager`, `TextUtils` i `ConfigUpdater` odpowiadają za języki, formatowanie i uzupełnianie konfiguracji.
- `StatsManager` śledzi statystyki, `PlayerStatsManager` siłę, a `BoostManager` boosty.
- `CrystalListener` przechwytuje obrażenia i cykl życia chunków; `BoostVoucherListener` obsługuje vouchery.

Tagi encji to `simplemetin`, `metin_<configId>` (np. `metin_metin_common`) i `simplemetin_hologram` dla TextDisplay. `MetinCrystalDestroyedEvent` udostępnia kryształ, gracza i wylosowane nagrody; nie jest mechanizmem podziału dropów między graczy.

## Weryfikacja

Testy uruchamia `mvn test`, pełny build `mvn clean package`. Raporty JUnit są w `target/surefire-reports/`. Osobny zestaw E2E na rzeczywistym Paper 1.21.1 sprawdza walkę, dropy, boosty, uprawnienia, języki, migrację, TextDisplay, spawnery, reload, restart i odtworzenie po awarii. Wyniki oraz lokalizacje zestawu opisuje [TEST_REPORT.md](../TEST_REPORT.md).

Pozostałe starsze pliki opisowe w repozytorium mogą odnosić się do 1.0.0; ten przewodnik opisuje wersję 1.1.0.
