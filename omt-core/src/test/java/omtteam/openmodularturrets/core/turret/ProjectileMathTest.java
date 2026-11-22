package omtteam.openmodularturrets.core.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;

import omtteam.openmodularturrets.core.world.Vector3;
import org.junit.jupiter.api.Test;

class ProjectileMathTest {
    private static final double EPS = 1.0E-9;

    @Test
    void ampAddsShareOfHealthRoundedDown() {
        // (int) 20.7 = 20; 20 * (0.05 * 3) = 3.0000000000000004 -> 3
        assertEquals(13, ProjectileMath.damageWithAmp(10, 3, 0.05D, 20.7F));
        // 7 * 0.5 = 3.5 -> 3
        assertEquals(13, ProjectileMath.damageWithAmp(10, 1, 0.5D, 7F));
    }

    @Test
    void floatAmpKeepsTheFraction() {
        // (int) 7.9 = 7; 7 * 0.05 = 0.35, not rounded away
        assertEquals(10.35F, ProjectileMath.damageWithAmp(10F, 1, 0.05D, 7.9F), 1.0E-5F);
        assertEquals(2.5F, ProjectileMath.damageWithAmp(2.5F, 0, 0.5D, 100F), "no amp");
        assertEquals(13, ProjectileMath.damageWithAmp(10, 1, 0.5D, 7F), "int arguments still pick the int overload");
    }

    @Test
    void floatSplits() {
        ProjectileMath.Split grenade = ProjectileMath.grenadeSplit(5.5F);
        assertEquals(4.95F, grenade.normal(), 1.0E-5F);
        assertEquals(0.55F, grenade.armorBypass(), 1.0E-5F);
        ProjectileMath.Split plasma = ProjectileMath.plasmaSplit(3.5F);
        assertEquals(1.75F, plasma.normal());
        assertEquals(1.75F, plasma.armorBypass());
    }

    @Test
    void noAmpLeavesDamageUnchanged() {
        assertEquals(10, ProjectileMath.damageWithAmp(10, 0, 0.5D, 100F));
    }

    @Test
    void falloffIsFullUpToTheOptimalRangeThenLinearDownToTheMinimum() {
        // machine gun: range 18, optimal 0.6 (10.8 blocks), half damage at 18
        assertEquals(1.0D, ProjectileMath.falloff(0, 18, 0.6D, 0.5D));
        assertEquals(1.0D, ProjectileMath.falloff(10.8D, 18, 0.6D, 0.5D), EPS);
        assertEquals(0.75D, ProjectileMath.falloff(14.4D, 18, 0.6D, 0.5D), EPS, "half way down");
        assertEquals(0.5D, ProjectileMath.falloff(18, 18, 0.6D, 0.5D), EPS);
        assertEquals(0.5D, ProjectileMath.falloff(25, 18, 0.6D, 0.5D), "beyond the range");
    }

    @Test
    void falloffWithFullSharesIsOff() {
        assertEquals(1.0D, ProjectileMath.falloff(10, 18, 1, 1));
        assertEquals(1.0D, ProjectileMath.falloff(40, 18, 1, 1));
        assertEquals(0.5D, ProjectileMath.falloff(18.5D, 18, 1, 0.5D), "an optimal range of 1 still weakens shots beyond it");
    }

    @Test
    void falloffClampsItsInputs() {
        assertEquals(1.0D, ProjectileMath.falloff(10, 0, 0.5D, 0.5D), "no range");
        assertEquals(0.0D, ProjectileMath.falloff(20, 10, 0.5D, -1), "minimum below 0");
        assertEquals(1.0D, ProjectileMath.falloff(20, 10, 0.5D, 3), "minimum above 1");
        assertEquals(0.5D, ProjectileMath.falloff(5, 10, -1, 0), EPS, "optimal below 0 starts the drop at the turret");
        assertEquals(1.0D, ProjectileMath.falloff(9, 10, 2, 0), "optimal above 1 is the whole range");
    }

    @Test
    void splits() {
        ProjectileMath.Split grenade = ProjectileMath.grenadeSplit(10);
        assertEquals(9.0F, grenade.normal(), 1.0E-6F);
        assertEquals(1.0F, grenade.armorBypass(), 1.0E-6F);
        ProjectileMath.Split plasma = ProjectileMath.plasmaSplit(7);
        assertEquals(3.5F, plasma.normal());
        assertEquals(3.5F, plasma.armorBypass());
    }

    @Test
    void motionHasDragAndGravity() {
        Vector3 air = ProjectileMath.nextMotion(new Vector3(1, 2, 3), false, false, 0.03D);
        assertEquals(0.99, air.x(), EPS);
        assertEquals(1.98 - 0.03, air.y(), EPS);
        assertEquals(2.97, air.z(), EPS);
        Vector3 water = ProjectileMath.nextMotion(new Vector3(1, 2, 3), true, true, 0.03D);
        assertEquals(0.8, water.x(), EPS);
        assertEquals(1.6, water.y(), EPS);
    }

    @Test
    void inaccuracyIsAccuracyDeviationOverFive() {
        assertEquals(2.0F, ProjectileMath.inaccuracy(10D));
    }

    @Test
    void raySpreadGrowsWithDistanceAndSmallTargets() {
        double near = ProjectileMath.raySpread(10D, 1.8D, 0D, 10D);
        double far = ProjectileMath.raySpread(10D, 1.8D, 10D, 10D);
        double small = ProjectileMath.raySpread(10D, 0.3D, 0D, 10D);
        assertEquals(0.003D * 10D * 0.3D * 10D, near, EPS);
        assertEquals(0.003D * 10D * 0.5D * 10D, far, EPS);
        assertEquals(near * 1.5D, small, EPS);
        assertEquals(near, ProjectileMath.raySpread(10D, 0.5D, 0D, 10D), EPS, "half a block high is not small");
    }
}
