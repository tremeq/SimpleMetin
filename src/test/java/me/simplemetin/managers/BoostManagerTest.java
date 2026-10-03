package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.bukkit.plugin.Plugin;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BoostManagerTest {

    SimpleMetin plugin;
    BoostManager boosts;
    Player steve;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        TestSupport.resetServer();
        plugin = TestSupport.plugin(dir.toFile(), TestSupport.yaml("""
                boosts:
                  bossbar:
                    enabled: false
                  global:
                    stacking-mode: 1.0
                  personal:
                    stacking-enabled: true
                """));
        boosts = new BoostManager(plugin);
        steve = TestSupport.player("Steve");
    }

    @Test
    @DisplayName("no boost -> 1.0x")
    void noBoost() {
        assertEquals(1.0, boosts.getTotalMultiplier(steve));
        assertNull(boosts.getGlobalBoost());
        assertNull(boosts.getPersonalBoost(steve.getUniqueId()));
    }

    @Test
    @DisplayName("global only -> global multiplier")
    void globalOnly() {
        boosts.activateGlobalBoost(2.5, 60);
        assertEquals(2.5, boosts.getTotalMultiplier(steve), 1e-9);
        assertTrue(boosts.getGlobalBoostTimeLeft() > 55);
    }

    @Test
    @DisplayName("personal only -> personal multiplier")
    void personalOnly() {
        boosts.activatePersonalBoost(steve, 3.0, 60);
        assertEquals(3.0, boosts.getTotalMultiplier(steve), 1e-9);
        assertTrue(boosts.getPersonalBoostTimeLeft(steve.getUniqueId()) > 55);
    }

    @Test
    @DisplayName("stacking-enabled=true: additive by default (2x+3x=4x), multiplicative with mode 2.0 (2x*3x=6x)")
    void stackingEnabled() {
        boosts.activateGlobalBoost(2.0, 60);
        boosts.activatePersonalBoost(steve, 3.0, 60);
        assertEquals(4.0, boosts.getTotalMultiplier(steve), 1e-9);
        plugin.getConfig().set("boosts.global.stacking-mode", 2.0);
        assertEquals(6.0, boosts.getTotalMultiplier(steve), 1e-9);
    }

    @Test
    @DisplayName("stacking-enabled=false -> higher of global/personal")
    void stackingDisabled() {
        plugin.getConfig().set("boosts.personal.stacking-enabled", false);
        boosts.activateGlobalBoost(2.0, 60);
        boosts.activatePersonalBoost(steve, 3.0, 60);
        assertEquals(3.0, boosts.getTotalMultiplier(steve), 1e-9);
        boosts.activateGlobalBoost(4.0, 60);
        assertEquals(4.0, boosts.getTotalMultiplier(steve), 1e-9);
    }

    @Test
    @DisplayName("expired boosts are dropped")
    void expired() {
        boosts.activateGlobalBoost(2.0, 0);
        boosts.activatePersonalBoost(steve, 2.0, 0);
        assertEquals(1.0, boosts.getTotalMultiplier(steve));
        assertNull(boosts.getGlobalBoost());
        assertNull(boosts.getPersonalBoost(steve.getUniqueId()));
    }

    @Test
    @DisplayName("stacking-mode 1.0 adds bonuses (2x+2x=3x), 2.0 multiplies (2x*2x=4x)")
    void stackingModeChangesResult() {
        boosts.activateGlobalBoost(2.0, 60);
        boosts.activatePersonalBoost(steve, 2.0, 60);

        plugin.getConfig().set("boosts.global.stacking-mode", 1.0);
        assertEquals(3.0, boosts.getTotalMultiplier(steve), 1e-9);
        plugin.getConfig().set("boosts.global.stacking-mode", 2.0);
        assertEquals(4.0, boosts.getTotalMultiplier(steve), 1e-9);
    }

    Runnable startAndCaptureTask(BoostManager manager) {
        manager.start();
        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(TestSupport.server().getScheduler(), atLeastOnce())
                .runTaskTimer(any(Plugin.class), task.capture(), anyLong(), anyLong());
        return task.getValue();
    }

    @Test
    @DisplayName("boost-expired is sent to the player when a personal boost ends")
    void expiryMessagesSent() {
        plugin.getConfig().set("messages.boost-expired", "BOOST EXPIRED");
        var task = startAndCaptureTask(boosts);
        doReturn(List.of(steve)).when(TestSupport.server()).getOnlinePlayers();

        boosts.activatePersonalBoost(steve, 2.0, 0); // expires immediately
        task.run();
        task.run();

        verify(steve, times(1)).sendMessage(TestSupport.text("BOOST EXPIRED"));
    }

    @Test
    @DisplayName("global-boost-expired is broadcast once, also with boss bars disabled")
    void globalExpiryBroadcast() {
        plugin.getConfig().set("messages.global-boost-expired", "GLOBAL EXPIRED");
        var task = startAndCaptureTask(boosts);

        boosts.activateGlobalBoost(2.0, 0);
        task.run();
        task.run();

        verify(TestSupport.server(), times(1)).broadcast(TestSupport.text("Global Drop Boost has expired"));
    }

    @Test
    @DisplayName("active boosts survive a restart (boosts.yml), expired ones are dropped")
    void boostsPersisted() {
        var alex = TestSupport.player("Alex");
        boosts.activateGlobalBoost(2.5, 600);
        boosts.activatePersonalBoost(steve, 3.0, 600);
        boosts.activatePersonalBoost(alex, 2.0, 0);
        boosts.shutdown();

        var reloaded = new BoostManager(plugin);
        assertEquals(2.5, reloaded.getGlobalBoost().multiplier);
        assertEquals(3.0, reloaded.getPersonalBoost(steve.getUniqueId()).multiplier);
        assertNull(reloaded.getPersonalBoost(alex.getUniqueId()));
        assertTrue(reloaded.getPersonalBoostTimeLeft(steve.getUniqueId()) > 590);
    }

    @Test
    @DisplayName("multiplier formatting is locale independent")
    void formatMultiplier() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"));
            assertEquals("2.5", BoostManager.formatMultiplier(2.5));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
