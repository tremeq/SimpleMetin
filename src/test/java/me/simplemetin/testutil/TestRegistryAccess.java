package me.simplemetin.testutil;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Registered through META-INF/services so that org.bukkit.Registry can initialise without a server.
 * The item registry hands out ItemTypes whose stacks remember their amount, which is all the plugin reads.
 */
public class TestRegistryAccess implements RegistryAccess {

    private final Map<Object, Registry<?>> registries = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings({"unchecked", "removal"})
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return (Registry<T>) registries.computeIfAbsent(type, k -> mock(Registry.class));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> key) {
        return (Registry<T>) registries.computeIfAbsent(key, k -> k == RegistryKey.ITEM ? itemRegistry() : mock(Registry.class));
    }

    private static Registry<?> itemRegistry() {
        return mock(Registry.class, invocation -> {
            if (invocation.getMethod().getName().startsWith("get")) {
                Material material = null;
                if (invocation.getArguments().length > 0 && invocation.getArgument(0) instanceof NamespacedKey key) {
                    material = Material.getMaterial(key.getKey().toUpperCase(java.util.Locale.ROOT));
                }
                return itemType(material);
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    // ItemType's static fields cast registry entries to ItemType.Typed, so the mock must implement it
    private static ItemType itemType(Material material) {
        ItemType.Typed<?> type = mock(ItemType.Typed.class);
        when(type.createItemStack(Mockito.anyInt())).thenAnswer(inv -> {
            int amount = inv.getArgument(0);
            ItemStack stack = mock(ItemStack.class);
            when(stack.getAmount()).thenReturn(amount);
            when(stack.getType()).thenReturn(material);
            when(stack.clone()).thenReturn(stack);
            return stack;
        });
        return type;
    }
}
