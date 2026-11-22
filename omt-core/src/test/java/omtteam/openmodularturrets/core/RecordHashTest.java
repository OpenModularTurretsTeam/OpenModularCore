package omtteam.openmodularturrets.core;

import omtteam.openmodularturrets.core.settings.TurretStats;
import omtteam.openmodularturrets.core.turret.ProjectileMath;
import omtteam.openmodularturrets.core.turret.RayProfile;
import omtteam.openmodularturrets.core.turret.TurretMath;
import omtteam.openmodularturrets.core.world.Box;
import omtteam.openmodularturrets.core.world.Quaternion;
import omtteam.openmodularturrets.core.world.Vector3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The records that write out equals and hashCode (see {@link EqualsContractTest}) hash as a record does on Java 17,
 * on both runs, so writing them out changed no hash value on the modern versions.
 */
class RecordHashTest {
    /** What a record's generated hashCode computes on Java 17: 0-seeded, {@code 31 * h} plus each component's hash. */
    private static int recordHash(Object... components) {
        int h = 0;
        for (Object component : components) {
            h = 31 * h + Objects.hashCode(component);
        }
        return h;
    }

    /** The Java 8 run skips this: the downgraded records hash differently (JvmDowngrader uses {@code Objects.hash}). */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void theHelperComputesTheJdkRecordHash() {
        record Sample(boolean flag, int count, float share, double length, Object other) {
        }
        assertEquals(new Sample(true, 7, 1.5F, -2.25, "x").hashCode(), recordHash(true, 7, 1.5F, -2.25, "x"));
    }

    @Test
    void vector3() {
        assertEquals(recordHash(1.5, -2.0, 3.25), new Vector3(1.5, -2, 3.25).hashCode());
    }

    @Test
    void quaternion() {
        assertEquals(recordHash(0.1, 0.2, 0.3, 0.9), new Quaternion(0.1, 0.2, 0.3, 0.9).hashCode());
    }

    @Test
    void box() {
        assertEquals(recordHash(1.0, 2.0, 3.0, 4.5, 5.5, 6.5), new Box(1, 2, 3, 4.5, 5.5, 6.5).hashCode());
    }

    @Test
    void turretStats() {
        TurretStats stats = new TurretStats(true, 12, 15, 4, 100, 3.5, 6, 0.25, 0.5, 2, 0.75, 0.125, 0.2, 0.3,
                0.6, 0.4, 12.5, 2.25);
        assertEquals(recordHash(true, 12, 15, 4, 100, 3.5, 6, 0.25, 0.5, 2, 0.75, 0.125, 0.2, 0.3, 0.6, 0.4, 12.5,
                        2.25),
                stats.hashCode());
    }

    @Test
    void projectileSplit() {
        assertEquals(recordHash(0.6F, 0.4F), new ProjectileMath.Split(0.6F, 0.4F).hashCode());
    }

    @Test
    void angles() {
        assertEquals(recordHash(10F, -20F), new TurretMath.Angles(10F, -20F).hashCode());
    }

    @Test
    void lead() {
        Vector3 direction = new Vector3(1, 2, 3);
        assertEquals(recordHash(direction, 0.75F), new TurretMath.Lead(direction, 0.75F).hashCode());
    }

    @Test
    void rayProfile() {
        RayProfile arc = RayProfile.ARC;
        assertEquals(recordHash(arc.armorModifier(), 0.6F, 0.4F, true), arc.hashCode());
    }
}
