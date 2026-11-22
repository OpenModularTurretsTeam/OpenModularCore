package omtteam.openmodularturrets.core.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** {@link TurretStats.Builder}: the default stats of turrets added through the turret API. */
class TurretStatsTest {
    @Test
    void builderStartsAtTheCommonDefaults() {
        TurretStats stats = TurretStats.builder(24, 20, 6, 500).build();
        assertEquals(new TurretStats(true, 24, 20, 6, 500, 10, 4, 0.05D, 0.1D, 2, 0.2D, 0.08D, 0.1D, 0.05D), stats);
        assertEquals(1.0D, stats.optimalRange(), "no falloff");
        assertEquals(1.0D, stats.falloffDamage(), "no falloff");
        assertEquals(0.0D, stats.turnRate(), "turns instantly");
        assertEquals(0.0D, stats.effectStrength(), "no effect");
    }

    @Test
    void constructorWithoutEffectStrengthHasNoEffect() {
        assertEquals(new TurretStats(true, 24, 20, 6, 500, 10, 4, 0.05D, 0.1D, 2, 0.2D, 0.08D, 0.1D, 0.05D, 0.6D, 0.5D,
                        12, 0),
                new TurretStats(true, 24, 20, 6, 500, 10, 4, 0.05D, 0.1D, 2, 0.2D, 0.08D, 0.1D, 0.05D, 0.6D, 0.5D, 12));
    }

    @Test
    void shortConstructorHasNoFalloffAndTurnsInstantly() {
        assertEquals(new TurretStats(true, 24, 20, 6, 500, 10, 4, 0.05D, 0.1D, 2, 0.2D, 0.08D, 0.1D, 0.05D, 1, 1, 0, 0),
                new TurretStats(true, 24, 20, 6, 500, 10, 4, 0.05D, 0.1D, 2, 0.2D, 0.08D, 0.1D, 0.05D));
    }

    @Test
    void builderSetsEveryValue() {
        TurretStats stats = TurretStats.builder(24, 20, 6, 500)
                .enabled(false)
                .accuracyDeviation(15)
                .maxSimultaneous(2)
                .damageAmp(0.07D)
                .fireRateUpgrade(0.15D)
                .rangeUpgrade(3)
                .accuracyUpgrade(0.25D)
                .efficiencyUpgrade(0.1D)
                .recycler(0.2D, 0.3D)
                .falloff(0.6D, 0.5D)
                .turnRate(12)
                .effectStrength(1.5D)
                .build();
        assertEquals(new TurretStats(false, 24, 20, 6, 500, 15, 2, 0.07D, 0.15D, 3, 0.25D, 0.1D, 0.2D, 0.3D,
                0.6D, 0.5D, 12, 1.5D), stats);
    }

    @Test
    void accessorsReturnTheirOwnValue() {
        TurretStats stats = new TurretStats(false, 24, 20, 6, 500, 15, 2, 0.07D, 0.15D, 3, 0.25D, 0.1D, 0.2D, 0.3D,
                0.6D, 0.5D, 12, 1.5D);
        assertFalse(stats.enabled());
        assertEquals(24, stats.baseRange());
        assertEquals(20, stats.baseFireRate());
        assertEquals(6, stats.baseDamage());
        assertEquals(500, stats.powerUsage());
        assertEquals(15, stats.baseAccuracyDeviation());
        assertEquals(2, stats.maxSimultaneous());
        assertEquals(0.07D, stats.damageAmp());
        assertEquals(0.15D, stats.fireRateUpgrade());
        assertEquals(3, stats.rangeUpgrade());
        assertEquals(0.25D, stats.accuracyUpgrade());
        assertEquals(0.1D, stats.efficiencyUpgrade());
        assertEquals(0.2D, stats.recyclerNegateChance());
        assertEquals(0.3D, stats.recyclerAddChance());
        assertEquals(0.6D, stats.optimalRange());
        assertEquals(0.5D, stats.falloffDamage());
        assertEquals(12, stats.turnRate());
        assertEquals(1.5D, stats.effectStrength());
    }
}
