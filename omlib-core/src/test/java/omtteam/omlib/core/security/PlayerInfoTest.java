package omtteam.omlib.core.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInfoTest {
    private static final UUID ALICE = UUID.nameUUIDFromBytes("Alice".getBytes());

    @Test
    void uuidAndNameAreRequired() {
        assertThrows(NullPointerException.class, () -> new PlayerInfo(null, "Alice", false));
        assertThrows(NullPointerException.class, () -> new PlayerInfo(ALICE, null, true));
    }

    @Test
    void keepsItsValues() {
        PlayerInfo info = new PlayerInfo(ALICE, "Alice", true);
        assertEquals(ALICE, info.uuid());
        assertEquals("Alice", info.name());
        assertTrue(info.operator());
    }
}
