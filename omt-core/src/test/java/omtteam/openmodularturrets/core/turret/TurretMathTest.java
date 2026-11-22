package omtteam.openmodularturrets.core.turret;

import omtteam.omlib.core.world.BlockPosition;
import omtteam.omlib.core.world.Face;
import omtteam.openmodularturrets.core.world.Vector3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurretMathTest {
    private static final double EPS = 1e-6;

    private static void assertVector(Vector3 expected, Vector3 actual) {
        assertEquals(expected.x(), actual.x(), EPS, "x");
        assertEquals(expected.y(), actual.y(), EPS, "y");
        assertEquals(expected.z(), actual.z(), EPS, "z");
    }

    @ParameterizedTest
    @EnumSource(Face.class)
    void framesAreRightHandedAndOrthonormal(Face face) {
        TurretMath.Frame frame = TurretMath.frame(face);
        assertVector(Vector3.of(face), frame.up());
        assertEquals(1, frame.right().length(), EPS);
        assertEquals(1, frame.forward().length(), EPS);
        assertEquals(0, frame.right().dot(frame.up()), EPS);
        assertEquals(0, frame.forward().dot(frame.up()), EPS);
        assertVector(frame.forward(), frame.right().cross(frame.up()));
    }

    @ParameterizedTest
    @EnumSource(Face.class)
    void anglesAndDirectionRoundTrip(Face face) {
        Vector3 target = new Vector3(3, 2, -5);
        TurretMath.Angles angles = TurretMath.anglesFor(face, target);
        assertVector(target.scale(1 / target.length()), TurretMath.direction(face, angles));
    }

    @Test
    void straightAwayFromTheBaseIsPitch90() {
        assertEquals(90, TurretMath.anglesFor(Face.UP, new Vector3(0, 5, 0)).pitch(), EPS);
        assertEquals(90, TurretMath.anglesFor(Face.NORTH, new Vector3(0, 0, -5)).pitch(), EPS);
        assertEquals(-90, TurretMath.anglesFor(Face.DOWN, new Vector3(0, 5, 0)).pitch(), EPS, "into the base");
    }

    @Test
    void turretsCannotAimIntoTheirBase() {
        assertTrue(TurretMath.canAim(Face.UP, new Vector3(5, -1, 0)), "slightly below the mounting plane");
        assertFalse(TurretMath.canAim(Face.UP, new Vector3(1, -5, 0)));
        assertFalse(TurretMath.canAim(Face.EAST, new Vector3(-5, 0, 0)), "straight through the base");
        assertEquals(TurretMath.MIN_PITCH, TurretMath.anglesFor(Face.UP, new Vector3(5, -5, 0)).pitch());
        assertTrue(TurretMath.canAim(Face.UP, new Vector3(5, -5, 0)), "exactly at the lowest pitch");
        assertEquals(TurretMath.MIN_PITCH, TurretMath.limit(new TurretMath.Angles(30, -80)).pitch(), EPS);
        assertEquals(30, TurretMath.limit(new TurretMath.Angles(30, -80)).yaw(), EPS);
    }

    @Test
    void yawOnTopOfABase() {
        assertEquals(0, TurretMath.anglesFor(Face.UP, new Vector3(0, 0, 1)).yaw(), EPS);
        assertEquals(90, TurretMath.anglesFor(Face.UP, new Vector3(1, 0, 0)).yaw(), EPS, "right of forward");
        assertEquals(-90, TurretMath.anglesFor(Face.UP, new Vector3(-1, 0, 0)).yaw(), EPS);
        assertEquals(0, TurretMath.anglesFor(Face.UP, new Vector3(0, 0, 1)).pitch(), EPS);
    }

    @Test
    void leadFollowsTheTarget() {
        // 6 blocks at speed 3 take a little over 2 ticks (air drag); the target moves 0.5 blocks/tick sideways
        TurretMath.Lead lead = TurretMath.lead(new Vector3(0, 0, 6), new Vector3(0.5, 0, 0), 3.0, 0);
        double time = ProjectileMath.flightTime(6, 3.0);
        assertTrue(time > 2.0 && time < 2.1, "was " + time);
        assertEquals(0.5 * time, lead.direction().x(), EPS);
        assertEquals(-0.1, lead.direction().y(), EPS);
        assertEquals(Math.sqrt(36 + 0.25 * time * time) / 6, lead.speedFactor(), 1e-5);
    }

    @Test
    void leadAimsAboveTheTargetByTheDrop() {
        Vector3 offset = new Vector3(0, 0, 30);
        TurretMath.Lead straight = TurretMath.lead(offset, Vector3.ZERO, 3.0, 0);
        TurretMath.Lead lobbed = TurretMath.lead(offset, Vector3.ZERO, 3.0, 0.03);
        double time = ProjectileMath.flightTime(30, 3.0);
        assertEquals(ProjectileMath.dropAfter(time, 0.03), lobbed.direction().y() - straight.direction().y(), EPS);
        assertTrue(lobbed.direction().y() - straight.direction().y() > 1.0);
    }

    @Test
    void dropMatchesTheEntityPhysics() {
        // tick 1 moves without any fall yet, then the fall velocity builds up: 0.03, 0.0597, ...
        assertEquals(0.0, ProjectileMath.dropAfter(1, 0.03), EPS);
        assertEquals(0.03, ProjectileMath.dropAfter(2, 0.03), EPS);
        // a partial last tick counts with its share: half of tick 2's fall
        assertEquals(0.015, ProjectileMath.dropAfter(1.5, 0.03), EPS);
        assertEquals(0.03 + 0.0597, ProjectileMath.dropAfter(3, 0.03), EPS);
        assertEquals(0.0, ProjectileMath.dropAfter(40, 0), EPS);
    }

    @Test
    void flightTimeIsCappedAtTheProjectileLifetime() {
        assertEquals(ProjectileMath.MAX_AGE, ProjectileMath.flightTime(500, 3.0), EPS);
    }

    @Test
    void stationaryTargetsNeedNoCorrection() {
        TurretMath.Lead lead = TurretMath.lead(new Vector3(2, 3, 6), Vector3.ZERO, 3.0, 0);
        assertEquals(1.0F, lead.speedFactor(), 1e-6);
    }

    @Test
    void anyOffsetFromTheCentreIsLed() {
        // 1e-6 blocks is the smallest offset that still gets the lead (and its 0.1 lower aim point)
        TurretMath.Lead lead = TurretMath.lead(new Vector3(1.0E-6, 0, 0), Vector3.ZERO, 3.0, 0);
        assertEquals(-0.1, lead.direction().y(), EPS);
    }

    @Test
    void aTargetAtTheTurretCentreNeedsNoLead() {
        TurretMath.Lead lead = TurretMath.lead(Vector3.ZERO, new Vector3(0.5, 0, 0), 3.0, 0.03);
        assertEquals(Vector3.ZERO, lead.direction());
        assertEquals(1.0F, lead.speedFactor());
    }

    @Test
    void leadBelowTheCutoffAimsAtTheOffset() {
        Vector3 offset = new Vector3(0, 0, 1.0E-7);
        TurretMath.Lead lead = TurretMath.lead(offset, new Vector3(0.5, 0, 0), 3.0, 0.03);
        assertEquals(offset, lead.direction(), "no lead and no drop for a target at the muzzle");
        assertEquals(1.0F, lead.speedFactor());
    }

    @Test
    void angleDifferenceTakesTheShortWay() {
        assertEquals(20, TurretMath.angleDifference(350, 10), EPS);
        assertEquals(-20, TurretMath.angleDifference(10, 350), EPS);
        assertEquals(-180, TurretMath.angleDifference(0, 180), EPS);
    }

    @Test
    void turnTowardsStepsEachAxisByAtMostTheRate() {
        TurretMath.Angles turned = TurretMath.turnTowards(new TurretMath.Angles(0, 0), new TurretMath.Angles(90, -20), 10);
        assertEquals(10, turned.yaw(), EPS);
        assertEquals(-10, turned.pitch(), EPS);
        TurretMath.Angles pitchDone = TurretMath.turnTowards(new TurretMath.Angles(0, 0), new TurretMath.Angles(90, 4), 10);
        assertEquals(10, pitchDone.yaw(), EPS);
        assertEquals(4, pitchDone.pitch(), EPS, "an axis within reach arrives");
    }

    @Test
    void turnTowardsArrivesExactlyWithinReach() {
        TurretMath.Angles to = new TurretMath.Angles(33.3F, 12.1F);
        assertEquals(to, TurretMath.turnTowards(new TurretMath.Angles(30, 10), to, 5));
    }

    @Test
    void turnTowardsTakesTheShorterWayRound() {
        TurretMath.Angles turned = TurretMath.turnTowards(new TurretMath.Angles(170, 0), new TurretMath.Angles(-170, 0), 5);
        assertEquals(175, turned.yaw(), EPS);
        TurretMath.Angles wrapped = TurretMath.turnTowards(new TurretMath.Angles(178, 0), new TurretMath.Angles(-150, 0), 5);
        assertEquals(-177, wrapped.yaw(), EPS, "the yaw stays in [-180, 180)");
    }

    @Test
    void turnTowardsWithoutRateIsInstant() {
        TurretMath.Angles to = new TurretMath.Angles(-120, 60);
        assertEquals(to, TurretMath.turnTowards(new TurretMath.Angles(0, 0), to, 0));
        assertEquals(to, TurretMath.turnTowards(new TurretMath.Angles(0, 0), to, -3));
    }

    @Test
    void aimErrorIsTheAngleBetweenTheDirections() {
        assertEquals(0, TurretMath.aimError(new TurretMath.Angles(40, 10), new TurretMath.Angles(40, 10)), 1e-4);
        assertEquals(30, TurretMath.aimError(new TurretMath.Angles(0, 0), new TurretMath.Angles(30, 0)), 1e-4);
        assertEquals(30, TurretMath.aimError(new TurretMath.Angles(0, 0), new TurretMath.Angles(0, -30)), 1e-4);
        assertEquals(180, TurretMath.aimError(new TurretMath.Angles(0, 0), new TurretMath.Angles(180, 0)), 1e-4);
        assertEquals(0, TurretMath.aimError(new TurretMath.Angles(0, 90), new TurretMath.Angles(120, 90)), 1e-4,
                "straight up, the yaw does not matter");
        assertEquals(20, TurretMath.aimError(new TurretMath.Angles(170, 0), new TurretMath.Angles(-170, 0)), 1e-4);
    }

    /** The turret block at the origin: its centre is at (0.5, 0.5, 0.5). */
    private static final BlockPosition TURRET = new BlockPosition(0, 0, 0);

    @Test
    void pushIsHorizontalAwayFromTheTurretWithALift() {
        Vector3 push = TurretMath.impulse(TURRET, new FakePositionTarget(3.5, 5, 4.5), 0.5);
        assertVector(new Vector3(0.3, TurretMath.MAX_PUSH_LIFT, 0.4), push);
    }

    @Test
    void weakPushLiftsOnlyAsMuchAsItPushes() {
        Vector3 push = TurretMath.impulse(TURRET, new FakePositionTarget(-4.5, 0, 0.5), 0.25);
        assertVector(new Vector3(-0.25, 0.25, 0), push);
    }

    @Test
    void pushStraightAboveOnlyLifts() {
        assertVector(new Vector3(0, 0.3, 0), TurretMath.impulse(TURRET, new FakePositionTarget(0.5, 6, 0.5), 0.3));
    }

    @Test
    void pullIsHorizontalTowardsTheTurret() {
        Vector3 pull = TurretMath.impulse(TURRET, new FakePositionTarget(6.5, -3, 8.5), -0.5);
        assertVector(new Vector3(-0.3, 0, -0.4), pull);
    }

    @Test
    void pullStopsShortOfTheTurret() {
        assertVector(new Vector3(0, 0, -0.25), TurretMath.impulse(TURRET, new FakePositionTarget(0.5, 0, 1.75), -2));
        assertVector(new Vector3(0, 0, 0), TurretMath.impulse(TURRET, new FakePositionTarget(0.5, 0, 1.25), -2));
        assertVector(new Vector3(0, 0, 0), TurretMath.impulse(TURRET, new FakePositionTarget(0.5, 4, 0.5), -2));
    }

    @Test
    void noStrengthNoImpulse() {
        assertVector(new Vector3(0, 0, 0), TurretMath.impulse(TURRET, new FakePositionTarget(5.5, 0, 0.5), 0));
    }

    @Test
    void theSmallestDirectionStillPushesSideways() {
        double x = 0.5 + TurretMath.MIN_DIRECTION;
        assertEquals(new Vector3(0.3, 0.3, 0), TurretMath.impulse(TURRET, new FakePositionTarget(x, 0, 0.5), 0.3));
    }

    @Test
    void zeroStrengthIsExactlyNoImpulse() {
        Vector3 none = new Vector3(0, 0, 0);
        assertEquals(none, TurretMath.impulse(TURRET, new FakePositionTarget(5.5, 0, 0.5), -0.0D));
        assertEquals(none, TurretMath.impulse(TURRET, new FakePositionTarget(1.5, 0, 0.5), -2),
                "a target where the pull stops is not moved");
    }
}
