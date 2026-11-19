package omtteam.openmodularturrets.core.base;

import omtteam.omlib.core.card.MemoryCardData;
import omtteam.omlib.core.card.MemoryCardRules;
import omtteam.omlib.core.machine.MachineMode;
import omtteam.omlib.core.security.AccessLevel;
import omtteam.omlib.core.security.PlayerInfo;
import omtteam.omlib.core.security.SecurityEnvironment;
import omtteam.omlib.core.security.TrustManager;
import omtteam.omlib.core.security.TrustResult;
import omtteam.omlib.core.world.Face;
import omtteam.openmodularturrets.core.card.BaseCardRules;
import omtteam.openmodularturrets.core.item.TurretBaseUpgradeType;
import omtteam.openmodularturrets.core.turret.TurretType;
import omtteam.openmodularturrets.core.world.Neighbour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The base part of the memory card rules ({@link BaseCardRules}); lives next to the base logic tests to share their
 * fakes. The card data and the trust part are tested in OMLib's core ({@code MemoryCardDataTest},
 * {@code MemoryCardRulesTest}).
 */
class BaseCardRulesTest {
    private static final PlayerInfo OWNER = player("Owner");
    private static final PlayerInfo OTHER_OWNER = player("OtherOwner");
    private static final PlayerInfo ADMIN = player("Admin");
    private static final PlayerInfo OPENER = player("Opener");

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

    private FakeSettings settings;
    private FakeWorld world;

    private static PlayerInfo player(String name) {
        return new PlayerInfo(UUID.nameUUIDFromBytes(name.getBytes()), name, false);
    }

    @BeforeEach
    void setUp() {
        settings = new FakeSettings();
        world = new FakeWorld();
    }

    private TurretBaseLogic base(PlayerInfo owner) {
        TurretBaseLogic logic = new TurretBaseLogic(4, settings, new TrustManager(NO_TEAMS, () -> {
        }), world, new Random(1)::nextLong, changed -> {
        });
        logic.getTrustManager().setOwner(owner.identity());
        return logic;
    }

    private static void trust(TurretBaseLogic logic, PlayerInfo owner, PlayerInfo player, AccessLevel level) {
        assertEquals(TrustResult.SUCCESS, logic.getTrustManager().addTrustedPlayer(owner, player.identity()));
        assertEquals(TrustResult.SUCCESS, logic.getTrustManager().setAccessLevel(owner, player.uuid(), level));
    }

    /** A whole base card: the base part from here, the trust part from OMLib's {@link MemoryCardRules}. */
    @Test
    void settingsSurviveACopyAndPaste() {
        TurretBaseLogic source = base(OWNER);
        source.setMode(MachineMode.NONINVERTED);
        source.setMultiTargeting(true);
        source.setTargetPlayers(true);
        source.setTargetMobs(false);
        source.setTargetPassive(true);
        source.updateNeighbours(Map.of(Face.UP, new Neighbour.Turret(TurretType.MACHINE_GUN_TURRET))); // max 18
        source.setRange(3);
        trust(source, OWNER, ADMIN, AccessLevel.ADMIN);
        trust(source, OWNER, OPENER, AccessLevel.OPEN_GUI);

        TurretBaseLogic target = base(OTHER_OWNER);
        target.updateNeighbours(Map.of(Face.UP, new Neighbour.Turret(TurretType.MACHINE_GUN_TURRET)));
        BaseCardRules.apply(BaseCardRules.capture(source), target);
        MemoryCardRules.applyTrust(MemoryCardRules.captureTrust(source.getTrustManager()), target.getTrustManager());

        assertEquals(MachineMode.NONINVERTED, target.getMode());
        assertTrue(target.isMultiTargeting());
        assertTrue(target.getTargetingSettings().isTargetPlayers());
        assertFalse(target.getTargetingSettings().isTargetMobs());
        assertTrue(target.getTargetingSettings().isTargetPassive());
        assertEquals(3, target.getTargetingSettings().getRange(), "a range below the maximum is kept as it is");
        assertEquals(AccessLevel.ADMIN, target.getTrustManager().getTrustedPlayer(ADMIN.uuid()).orElseThrow().getAccessLevel());
        assertEquals(AccessLevel.OPEN_GUI, target.getTrustManager().getTrustedPlayer(OPENER.uuid()).orElseThrow().getAccessLevel());
    }

    @Test
    void rangeIsClampedToWhatTheTargetCanReach() {
        TurretBaseLogic target = base(OWNER);
        target.updateNeighbours(Map.of(Face.UP, new Neighbour.Turret(TurretType.MACHINE_GUN_TURRET))); // max 18

        BaseCardRules.apply(new MemoryCardData.BaseSettings(MachineMode.INVERTED, false, false, true, false, 10), target);
        assertEquals(10, target.getTargetingSettings().getRange(), "a range the target can reach is pasted as it is");

        BaseCardRules.apply(new MemoryCardData.BaseSettings(MachineMode.INVERTED, false, false, true, false, 99), target);
        assertEquals(18, target.getTargetingSettings().getRange());
    }

    /** A paste right after a config change clamps to the new maximum, not to the one of the last update. */
    @Test
    void rangeIsClampedToTheCurrentConfig() {
        TurretBaseLogic target = base(OWNER);
        target.updateNeighbours(Map.of(Face.UP, new Neighbour.Turret(TurretType.MACHINE_GUN_TURRET))); // max 18
        settings.turrets.put(TurretType.MACHINE_GUN_TURRET,
                FakeSettings.stats(10, 8, 2, 100, 30, 4, 0.06D, 0.1D, 2, 0.2D, 0.08D, 0.1D));

        BaseCardRules.apply(new MemoryCardData.BaseSettings(MachineMode.INVERTED, false, false, true, false, 99), target);
        assertEquals(10, target.getTargetingSettings().getMaxRange());
        assertEquals(10, target.getTargetingSettings().getRange());
    }

    /** A paste right after the maximum grew is clamped to the new maximum, so a range above the old one is kept. */
    @Test
    void rangeIsClampedToARaisedMaximum() {
        TurretBaseLogic target = base(OWNER);
        target.updateNeighbours(Map.of(Face.UP, new Neighbour.Turret(TurretType.MACHINE_GUN_TURRET))); // max 18
        settings.turrets.put(TurretType.MACHINE_GUN_TURRET,
                FakeSettings.stats(30, 8, 2, 100, 30, 4, 0.06D, 0.1D, 2, 0.2D, 0.08D, 0.1D));

        BaseCardRules.apply(new MemoryCardData.BaseSettings(MachineMode.INVERTED, false, false, true, false, 25), target);
        assertEquals(30, target.getTargetingSettings().getMaxRange());
        assertEquals(25, target.getTargetingSettings().getRange());
    }

    /** A card may be pasted before the base counted new range upgrades (the max range is updated every 5 ticks). */
    @Test
    void rangeFollowsUpgradesTheBaseHasNotCountedYet() {
        TurretBaseLogic target = base(OWNER);
        target.updateNeighbours(Map.of(Face.UP, new Neighbour.Turret(TurretType.MACHINE_GUN_TURRET))); // max 18
        world.upgradeSlots.set(0, FakeSlot.upgrade(TurretBaseUpgradeType.RANGE, 2));

        BaseCardRules.apply(new MemoryCardData.BaseSettings(MachineMode.INVERTED, false, false, true, false, 30), target);
        assertEquals(22, target.getTargetingSettings().getRange(), "18 + 2 upgrades * 2, not the old 18");
    }

    /** The settings of a fresh base form a valid base card (the record's checks accept them). */
    @Test
    void captureOfAFreshBaseMakesAValidBaseCard() {
        TurretBaseLogic logic = base(OWNER);
        MemoryCardData.BaseSettings captured = BaseCardRules.capture(logic);

        assertEquals(logic.getMode(), captured.mode());
        assertEquals(logic.getTargetingSettings().getRange(), captured.range());
        MemoryCardData card = new MemoryCardData(MemoryCardData.Source.TURRET_BASE, captured,
                MemoryCardRules.captureTrust(logic.getTrustManager()), null);
        assertEquals(captured, card.base());
    }

    /** Only static rules: the constructor is private (called here so the coverage threshold counts it). */
    @Test
    void cannotBeInstantiatedFromOutside() throws ReflectiveOperationException {
        Constructor<BaseCardRules> constructor = BaseCardRules.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
