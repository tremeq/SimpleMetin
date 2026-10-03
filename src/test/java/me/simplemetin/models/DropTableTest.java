package me.simplemetin.models;

import me.simplemetin.testutil.TestSupport;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DropTableTest {

    static final String CRYSTAL = """
            death-drops:
              always:
                item: DIAMOND
                amount: "2-4"
                chance: 100
              never:
                item: DIRT
                chance: 0
              half:
                item: GOLD_INGOT
                chance: 50
              special:
                custom-item: legendary
                chance: 100
              missing:
                custom-item: does_not_exist
                chance: 100
              bad:
                item: NOT_A_MATERIAL
            death-commands:
              money:
                command: "eco give %player% 100"
                chance: 100
            death-pools:
              bonus:
                rolls: "2"
                chance: 100
                unique: true
                entries:
                  a: {item: EMERALD, weight: 1}
                  b: {item: APPLE, weight: 1}
                  c: {command: "say jackpot", weight: 1}
              heavy:
                rolls: 1
                chance: 100
                entries:
                  common: {item: COAL, weight: 99}
                  rare: {item: NETHERITE_INGOT, weight: 1}
                  zero: {item: STONE, weight: 0}
            """;

    DropTable table;
    final Function<String, ItemStack> customItems = key -> {
        if (!key.equals("legendary")) return null;
        var item = mock(ItemStack.class);
        when(item.clone()).thenReturn(item);
        when(item.getType()).thenReturn(Material.NETHERITE_SWORD);
        return item;
    };

    @BeforeEach
    void setUp() {
        TestSupport.resetServer();
        table = DropTable.load(TestSupport.yaml(CRYSTAL), "death", TestSupport.LOGGER);
    }

    @Test
    @DisplayName("IntRange: \"1-3\", 5, invalid -> fallback, roll stays in range")
    void intRange() {
        assertEquals(new IntRange(1, 3), IntRange.parse("1-3", null));
        assertEquals(IntRange.of(5), IntRange.parse(5, null));
        assertEquals(IntRange.of(5), IntRange.parse("5", null));
        assertEquals(new IntRange(1, 3), IntRange.parse(" 3 - 1 ", null), "swapped bounds are fixed");
        assertEquals(IntRange.of(7), IntRange.parse("abc", IntRange.of(7)));
        var random = new Random(1);
        for (int i = 0; i < 200; i++) {
            int v = new IntRange(2, 4).roll(random);
            assertTrue(v >= 2 && v <= 4);
        }
        assertEquals("1-3", new IntRange(1, 3).toString());
    }

    @Test
    @DisplayName("loading: independent entries + commands, pools; invalid materials and zero-weight entries skipped")
    void loading() {
        assertEquals(6, table.getIndependent().size(), "always, never, half, special, missing, money");
        assertTrue(TestSupport.logged("Invalid drop item 'NOT_A_MATERIAL'"));
        assertEquals(2, table.getPools().size());
        assertEquals(2, table.getPools().get(1).entries().size(), "weight 0 entry removed");
        assertTrue(DropTable.load(null, "death", TestSupport.LOGGER).isEmpty());
        assertTrue(DropTable.load(TestSupport.yaml("x: 1"), "hit", TestSupport.LOGGER).isEmpty());
    }

    @Test
    @DisplayName("roll: chance 100 always, 0 never, amount range, custom item, command, unique pool picks 2 different")
    void roll() {
        var random = new Random(42);
        for (int i = 0; i < 50; i++) {
            var result = table.roll(random, 1.0, customItems, TestSupport.LOGGER);
            var types = result.items().stream().map(ItemStack::getType).toList();

            assertTrue(types.contains(Material.DIAMOND));
            assertFalse(types.contains(Material.DIRT));
            assertTrue(types.contains(Material.NETHERITE_SWORD), "custom item from items.yml");
            assertTrue(result.commands().contains("eco give %player% 100"));

            var diamonds = result.items().stream().filter(it -> it.getType() == Material.DIAMOND).findFirst().orElseThrow();
            assertTrue(diamonds.getAmount() >= 2 && diamonds.getAmount() <= 4, "amount " + diamonds.getAmount());

            long bonusPicks = types.stream().filter(t -> t == Material.EMERALD || t == Material.APPLE).count()
                    + result.commands().stream().filter(c -> c.equals("say jackpot")).count();
            assertEquals(2, bonusPicks, "rolls: 2 with unique: true");
            assertEquals(1, types.stream().filter(t -> t == Material.COAL || t == Material.NETHERITE_INGOT).count());
        }
        assertTrue(TestSupport.logged("custom item 'does_not_exist' not found"));
    }

    @Test
    @DisplayName("weights: 99:1 pool gives the common entry ~99% of the time")
    void weights() {
        var random = new Random(7);
        Map<Material, Integer> counts = new HashMap<>();
        for (int i = 0; i < 5000; i++) {
            for (var item : table.roll(random, 1.0, customItems, TestSupport.LOGGER).items()) {
                counts.merge(item.getType(), 1, Integer::sum);
            }
        }
        int coal = counts.getOrDefault(Material.COAL, 0);
        int netherite = counts.getOrDefault(Material.NETHERITE_INGOT, 0);
        assertEquals(5000, coal + netherite);
        assertTrue(netherite > 15 && netherite < 110, "~1% rare: " + netherite);
    }

    @Test
    @DisplayName("boost multiplies chances (50% x 2 = 100%)")
    void boost() {
        var random = new Random(3);
        int withBoost = 0;
        int without = 0;
        for (int i = 0; i < 300; i++) {
            if (table.roll(random, 2.0, customItems, TestSupport.LOGGER).items().stream().anyMatch(it -> it.getType() == Material.GOLD_INGOT)) withBoost++;
            if (table.roll(random, 1.0, customItems, TestSupport.LOGGER).items().stream().anyMatch(it -> it.getType() == Material.GOLD_INGOT)) without++;
        }
        assertEquals(300, withBoost);
        assertTrue(without > 100 && without < 200, "~50%: " + without);
    }
}
