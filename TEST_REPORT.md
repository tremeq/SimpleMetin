# SimpleMetin 1.1.0 — raport zakończenia prac

Data: 3 października 2026.

## Zakres przejęty z rozmowy Claude

Zakończono weryfikację siedmiu uzgodnionych funkcji: uzupełniania konfiguracji, hologramów TextDisplay, wiadomości EN/PL z wyborem języka, statystyk gracza i osobnych uprawnień, krótkich ID, automatycznych spawnów z ogłoszeniami oraz rozszerzonych dropów (zakresy ilości, pule wagowe, custom itemy).

Zachowano ustalenia użytkownika: domyślnie angielski, kolory `&` i MiniMessage, ID typu `common-1`, oba tryby spawnerów i znikanie kryształów po konfigurowalnym czasie. README został przepisany pod wersję 1.1.0, z przykładami konfiguracji, komendami, uprawnieniami i opisem migracji.

## Dodatkowe poprawki przy dokończeniu

- Spawny ponownie sprawdzają limity i liczbę graczy po zakończeniu ładowania chunka. Dwa oczekujące spawny nie przekraczają wspólnego limitu.
- Reload i zamknięcie pluginu unieważniają oczekujące spawny. Zakończenie ładowania poza głównym wątkiem przekazuje operacje świata do schedulera.
- Automatyczny aktualizator konfiguracji nie zastępuje błędnego YAML-a pustym/defaultowym plikiem ani istniejącej wartości skalarnej sekcją.
- `/metin item give` przy pełnym ekwipunku upuszcza nadmiar przy graczu zamiast go gubić.
- `/metin adddrop` odrzuca szanse spoza 0–100, `NaN` i nieskończoność przed zapisem.
- Poprawiono komentarze dotyczące czasu życia kryształów i ograniczeń wyszukiwania powierzchni w Netherze.

## Wyniki

| Sprawdzenie | Wynik |
|---|---|
| `mvn -o -B clean package` na Java 21 | BUILD SUCCESS |
| JUnit / Mockito | 123 testy, 0 niepowodzeń, 0 błędów, 0 pominiętych |
| E2E: Paper 1.21.1 build 133 + PlaceholderAPI 2.11.6 | 124/124 PASS |
| `git diff --check` | Bez błędów whitespace |
| Serwer testowy po zakończeniu | Zatrzymany |

Testy jednostkowe obejmują także opóźnione zakończenie ładowania chunków, zmianę konfiguracji w trakcie spawnu, koniec pracy serwera, sprawdzanie liczby graczy po opóźnieniu, wykonanie operacji świata na głównym wątku oraz zachowanie wymuszonego spawnu administratora.

E2E sprawdza walkę i cooldown, pociski i ochronę przed eksplozjami, dropy i NBT, vouchery, boosty i ich wygasanie, uprawnienia gracza bez OP, krótkie ID i teleportację, migrację configu i starych hologramów, TextDisplay, spawnery losowe i punktowe, znikanie i ogłoszenia, reload, cykl chunków, restart i odzyskanie po awarii.

Dodane w tej sesji sprawdzenia E2E potwierdzają przełączenie na polski przez reload, zachowanie własnego tłumaczenia po kolejnym reloadzie oraz wydanie custom itemu na ziemię przy pełnym ekwipunku. Zestaw zwraca teraz niezerowy kod zakończenia również przy nieudanej asercji.

Po E2E zmieniono jedynie dokumentację i dwa komentarze w `spawners.yml`, następnie wykonano czysty build. Porównano zawartość plików `.class` w końcowym JAR-ze z JAR-em sprawdzonym przez E2E — kod wykonywalny jest identyczny.

## Artefakty i ponowne uruchomienie

- JAR: [target/SimpleMetin-1.1.0.jar](target/SimpleMetin-1.1.0.jar).
- Raporty jednostkowe: `target/surefire-reports/`.
- Serwer testowy: `E:\Pliki Minecraft\test\simplemetin-server`.
- Zestaw E2E: `E:\Pliki Minecraft\test\simplemetin-bot\e2e.js`.
- Log E2E: `E:\Pliki Minecraft\test\simplemetin-bot\e2e-codex-1.1.log`.
- Wyniki poszczególnych asercji: `E:\Pliki Minecraft\test\simplemetin-bot\results.json`.
- Kopia wcześniejszego zestawu: `E:\Pliki Minecraft\test\simplemetin-bot\e2e-before-codex.js`.

Zestaw E2E pozostaje poza repozytorium w środowisku przygotowanym przez Claude. Uruchamia się przez `node e2e.js` z jego katalogu, po podmianie JAR-a w `plugins/` zatrzymanego serwera testowego. Czyści światy i dane pluginu **tego serwera testowego** przed uruchomieniem; nie należy kierować go na serwer produkcyjny.

## Granice weryfikacji

Wyniki dotyczą Java 21 i Paper 1.21.1 z PlaceholderAPI. Nie wykonywano testów obciążeniowych ani osobnej certyfikacji innych wersji Minecrafta, Purpura, ItemsAdder i pluginów ekonomii. Zapis custom itemów sprawdzono na przedmiocie z własnymi danymi; specjalne zachowania innych pluginów wymagają testu z nimi.

Liczenie pieniędzy nadal opiera się na komendach `eco give`, a nie potwierdzeniu salda przez Vault. Modyfikacje nie zostały zacommitowane ani wdrożone na serwer produkcyjny.
