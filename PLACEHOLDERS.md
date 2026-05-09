# SimpleMetin PlaceholderAPI

## Wymagania
- PlaceholderAPI zainstalowane na serwerze

## Placeholdery Statystyk

### Podstawowe statystyki gracza:
- `%metin_crystals_destroyed%` - Liczba zniszczonych kryształów
- `%metin_total_damage%` - Łączne obrażenia zadane kryształom
- `%metin_items_received%` - Liczba otrzymanych przedmiotów
- `%metin_money_earned%` - Zarobiona waluta

### Statystyki per-crystal:
- `%metin_type_<crystal_id>%` - Liczba zniszczonych kryształów danego typu
  - Przykład: `%metin_type_metin_common%`
  - Przykład: `%metin_type_metin_rare%`

## Placeholdery Boostów

### Status boostów:
- `%metin_boost_multiplier%` - Aktualny mnożnik dropów (np. "2.5")
- `%metin_boost_active%` - Czy gracz ma aktywny boost ("true"/"false")

### Personal boost:
- `%metin_personal_boost_multiplier%` - Mnożnik personal boosta
- `%metin_personal_boost_time%` - Pozostały czas personal boosta (format: "5m 30s")

### Global boost:
- `%metin_global_boost_multiplier%` - Mnożnik global boosta
- `%metin_global_boost_time%` - Pozostały czas global boosta

## Przykłady Użycia

### Scoreboard (z DeluxeScoreboard / FeatherBoard):
```yaml
lines:
  - "&e&lSTATYSTYKI"
  - "&7Zniszczono: &e%metin_crystals_destroyed%"
  - "&7Obrażenia: &e%metin_total_damage%"
  - "&7Boost: &e%metin_boost_multiplier%x"
  - "&7Czas: &e%metin_personal_boost_time%"
```

### TAB (z TAB plugin):
```yaml
tabprefix: "&7[&e%metin_boost_multiplier%x&7] "
```

### Chat (z EssentialsChat):
```yaml
format: '{DISPLAYNAME}&7: &f{MESSAGE} &8[&e%metin_crystals_destroyed% &7crystals&8]'
```

### Hologram (z DecentHolograms):
```yaml
lines:
  - "&e&lTOP PLAYER"
  - "&7Destroyed: &e%metin_crystals_destroyed%"
  - "&7Boost: &e%metin_boost_multiplier%x"
```

## Format Czasu

Czas boostów jest automatycznie formatowany:
- Powyżej godziny: `2h 30m 15s`
- Powyżej minuty: `45m 30s`
- Poniżej minuty: `45s`
- Brak boostu: `0s`
