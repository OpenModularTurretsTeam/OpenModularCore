package omtteam.omlib.core.card;

import omtteam.omlib.core.security.AccessLevel;
import omtteam.omlib.core.security.AccessProfile;
import omtteam.omlib.core.security.PlayerInfo;
import omtteam.omlib.core.security.SecurityEnvironment;
import omtteam.omlib.core.security.TrustManager;
import omtteam.omlib.core.security.TrustResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The trust part of the memory card rules: who may copy and paste, and how a trust list is captured and written. The
 * base part is tested in OMT's core ({@code BaseCardRulesTest}).
 */
class MemoryCardRulesTest {
    private static final PlayerInfo OWNER = player("Owner");
    private static final PlayerInfo OTHER_OWNER = player("OtherOwner");
    private static final PlayerInfo ADMIN = player("Admin");
    private static final PlayerInfo SETTINGS = player("Settings");
    private static final PlayerInfo OPENER = player("Opener");
    private static final PlayerInfo STRANGER = player("Stranger");

    private static final SecurityEnvironment NO_TEAMS = new SecurityEnvironment() {
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

    private static PlayerInfo player(String name) {
        return new PlayerInfo(UUID.nameUUIDFromBytes(name.getBytes()), name, false);
    }

    /** The trust list of a turret base (the default access profile, as a base builds it). */
    private static TrustManager base(PlayerInfo owner) {
        TrustManager trust = new TrustManager(NO_TEAMS, () -> {
        });
        trust.setOwner(owner.identity());
        return trust;
    }

    private static void trust(TrustManager trust, PlayerInfo owner, PlayerInfo player, AccessLevel level) {
        assertEquals(TrustResult.SUCCESS, trust.addTrustedPlayer(owner, player.identity()));
        assertEquals(TrustResult.SUCCESS, trust.setAccessLevel(owner, player.uuid(), level));
    }

    @Test
    void trustListSurvivesACopyAndPaste() {
        TrustManager source = base(OWNER);
        trust(source, OWNER, ADMIN, AccessLevel.ADMIN);
        trust(source, OWNER, OPENER, AccessLevel.OPEN_GUI);
        TrustManager target = base(OTHER_OWNER);

        MemoryCardRules.applyTrust(MemoryCardRules.captureTrust(source), target);

        assertEquals(AccessLevel.ADMIN, target.getTrustedPlayer(ADMIN.uuid()).orElseThrow().getAccessLevel());
        assertEquals(AccessLevel.OPEN_GUI, target.getTrustedPlayer(OPENER.uuid()).orElseThrow().getAccessLevel());
    }

    @Test
    void pastingKeepsTheOwnerOfTheTargetAndDropsItFromTheList() {
        TrustManager source = base(OWNER);
        trust(source, OWNER, OTHER_OWNER, AccessLevel.ADMIN);
        TrustManager target = base(OTHER_OWNER);

        MemoryCardRules.applyTrust(MemoryCardRules.captureTrust(source), target);

        assertEquals(OTHER_OWNER.identity(), target.getOwner());
        assertTrue(target.getTrustedPlayers().isEmpty());
    }

    @Test
    void pastingReplacesTheExistingTrustList() {
        TrustManager source = base(OWNER);
        trust(source, OWNER, ADMIN, AccessLevel.ADMIN);
        TrustManager target = base(OWNER);
        trust(target, OWNER, SETTINGS, AccessLevel.CHANGE_SETTINGS);

        MemoryCardRules.applyTrust(MemoryCardRules.captureTrust(source), target);

        assertTrue(target.getTrustedPlayer(SETTINGS.uuid()).isEmpty());
        assertTrue(target.getTrustedPlayer(ADMIN.uuid()).isPresent());
    }

    @Test
    void capturedTrustListIsACopy() {
        TrustManager source = base(OWNER);
        trust(source, OWNER, ADMIN, AccessLevel.ADMIN);
        MemoryCardData.TrustSettings captured = MemoryCardRules.captureTrust(source);

        source.removeTrustedPlayer(OWNER, ADMIN.uuid());

        assertEquals(1, captured.trustedPlayers().size());
    }

    @Test
    void onlyAdminsCopyAndPaste() {
        TrustManager trust = base(OWNER);
        trust(trust, OWNER, ADMIN, AccessLevel.ADMIN);
        trust(trust, OWNER, SETTINGS, AccessLevel.CHANGE_SETTINGS);
        trust(trust, OWNER, OPENER, AccessLevel.OPEN_GUI);

        assertTrue(MemoryCardRules.canCopy(trust, OWNER));
        assertTrue(MemoryCardRules.canPaste(trust, OWNER));
        assertTrue(MemoryCardRules.canCopy(trust, ADMIN));
        assertTrue(MemoryCardRules.canPaste(trust, ADMIN));
        for (PlayerInfo player : List.of(SETTINGS, OPENER, STRANGER)) {
            assertFalse(MemoryCardRules.canCopy(trust, player), player.name());
            assertFalse(MemoryCardRules.canPaste(trust, player), player.name());
        }
    }

    @Test
    void trustListsAreClampedToTheLevelsOfTheTarget() {
        TrustManager source = base(OWNER);
        trust(source, OWNER, SETTINGS, AccessLevel.CHANGE_SETTINGS);
        trust(source, OWNER, ADMIN, AccessLevel.ADMIN);
        TrustManager trap = new TrustManager(NO_TEAMS, () -> { }, AccessProfile.TRUSTED_AND_ADMIN);
        trap.setOwner(OTHER_OWNER.identity());

        MemoryCardRules.applyTrust(MemoryCardRules.captureTrust(source), trap);

        assertEquals(AccessLevel.NONE, trap.getTrustedPlayer(SETTINGS.uuid()).orElseThrow().getAccessLevel());
        assertEquals(AccessLevel.ADMIN, trap.getTrustedPlayer(ADMIN.uuid()).orElseThrow().getAccessLevel());
    }

    @Test
    void trapAdminsCopyAndPasteLikeBaseAdmins() {
        TrustManager trap = new TrustManager(NO_TEAMS, () -> { }, AccessProfile.TRUSTED_AND_ADMIN);
        trap.setOwner(OWNER.identity());
        assertEquals(TrustResult.SUCCESS, trap.addTrustedPlayer(OWNER, ADMIN.identity()));
        assertEquals(TrustResult.SUCCESS, trap.setAccessLevel(OWNER, ADMIN.uuid(), AccessLevel.ADMIN));
        assertEquals(TrustResult.SUCCESS, trap.addTrustedPlayer(OWNER, OPENER.identity()));

        assertTrue(MemoryCardRules.canCopy(trap, OWNER));
        assertTrue(MemoryCardRules.canPaste(trap, ADMIN));
        assertFalse(MemoryCardRules.canCopy(trap, OPENER));
        assertFalse(MemoryCardRules.canPaste(trap, STRANGER));
    }

    /** Only static rules: the constructor is private (called here so the coverage threshold counts it). */
    @Test
    void cannotBeInstantiatedFromOutside() throws ReflectiveOperationException {
        Constructor<MemoryCardRules> constructor = MemoryCardRules.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
