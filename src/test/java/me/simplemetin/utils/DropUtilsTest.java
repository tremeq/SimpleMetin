package me.simplemetin.utils;

import me.simplemetin.models.DropCommand;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DropUtilsTest {

    Server server;
    Player steve;

    @BeforeEach
    void setUp() {
        server = TestSupport.resetServer();
        steve = TestSupport.player("Steve");
    }

    @Test
    @DisplayName("money is not counted when the command fails (e.g. no economy plugin -> unknown command)")
    void failedCommandEarnsNothing() {
        when(server.dispatchCommand(any(), anyString())).thenReturn(false);
        long money = DropUtils.executeCommands(List.of(new DropCommand("eco give %player% 500", 100)), steve);
        assertEquals(0, money);
        verify(server).dispatchCommand(any(), eq("eco give Steve 500"));
    }

    @Test
    @DisplayName("commands: %player% replaced, chance 0 never runs, eco give amount summed")
    void commands() {
        when(server.dispatchCommand(any(), anyString())).thenReturn(true);
        long money = DropUtils.executeCommands(List.of(
                new DropCommand("eco give %player% 10", 100),
                new DropCommand("ECO GIVE %player% 5", 100),
                new DropCommand("eco give %player% abc", 100),
                new DropCommand("give %player% diamond", 100),
                new DropCommand("say never", 0)
        ), steve);

        assertEquals(15, money);
        verify(server).dispatchCommand(any(), eq("eco give Steve 10"));
        verify(server).dispatchCommand(any(), eq("give Steve diamond"));
        verify(server).dispatchCommand(any(), eq("eco give Steve abc"));
        verify(server, never()).dispatchCommand(any(), eq("say never"));
    }

    @Test
    @DisplayName("empty / null command list")
    void empty() {
        assertEquals(0, DropUtils.executeCommands(List.of(), steve));
        assertEquals(0, DropUtils.executeCommands(null, steve));
    }
}
