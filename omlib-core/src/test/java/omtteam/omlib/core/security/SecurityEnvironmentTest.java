package omtteam.omlib.core.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SecurityEnvironmentTest {
    /** An environment that implements only the required methods. */
    private static final SecurityEnvironment MINIMAL = new SecurityEnvironment() {
        @Override
        public Optional<String> getTeam(UUID uuid, String name) {
            return Optional.empty();
        }

        @Override
        public boolean operatorsCanAccessOwnedBlocks() {
            return false;
        }

        @Override
        public boolean damageTrustedPlayers() {
            return false;
        }
    };

    @Test
    void withoutOverridesNobodySharesOwnershipAndTheUuidIsTheIdentity() {
        PlayerIdentity owner = new PlayerIdentity(UUID.nameUUIDFromBytes("Owner".getBytes()), "Owner");
        assertFalse(MINIMAL.isSharedOwner(owner, owner));
        assertFalse(MINIMAL.offlineModeSupport());
    }
}
