package omtteam.openmodularlighting.core.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import omtteam.openmodularlighting.core.beam.BeamMode;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.function.LongSupplier;

class FormulaTest {
    @Test
    void coalAddsCarbonTimeSecondsAndConesUseItUpTwiceAsFast() {
        int stored = CarbonFuel.ticks(1600, 300);
        assertEquals(300 * 20, stored);
        assertEquals(300 * 20, CarbonFuel.runTicks(stored, BeamMode.STRAIGHT));
        assertEquals(300 * 10, CarbonFuel.runTicks(stored, BeamMode.NARROW_CONE));
        assertEquals(300 * 10, CarbonFuel.runTicks(stored, BeamMode.WIDE_CONE));
        assertEquals(1, CarbonFuel.drain(BeamMode.STRAIGHT));
        assertEquals(2, CarbonFuel.drain(BeamMode.WIDE_CONE));
        assertEquals(1, CarbonFuel.runTicks(1, BeamMode.NARROW_CONE)); // rounded up
        assertEquals(0, CarbonFuel.runTicks(0, BeamMode.NARROW_CONE));
        // the largest stored time (ticks caps at Integer.MAX_VALUE) must not overflow when rounded up
        assertEquals(Integer.MAX_VALUE / 2 + 1, CarbonFuel.runTicks(Integer.MAX_VALUE, BeamMode.WIDE_CONE));
    }

    @Test
    void carbonFuelUsesIntegerDivisionLikeTheOldMod() {
        // 300 * 100 / 1600 = 18 (not 18.75), then * 20
        assertEquals(360, CarbonFuel.ticks(100, 300));
        assertEquals(0, CarbonFuel.ticks(100, 1));
        assertEquals(0, CarbonFuel.ticks(1600, 0));
    }

    @Test
    void hugeBurnTimesAreCappedInsteadOfOverflowing() {
        // 1000 * 3_000_000 does not fit in an int
        assertEquals(1000L * 3_000_000 / 1600 * 20, CarbonFuel.ticks(3_000_000, 1000));
        assertEquals(Integer.MAX_VALUE, CarbonFuel.ticks(Integer.MAX_VALUE, 1000));
    }

    @Test
    void growDelayStaysInTheOldBounds() {
        LongSupplier random = new Random(1)::nextLong;
        long sum = 0;
        int samples = 20_000;
        for (int i = 0; i < samples; i++) {
            long delay = GrowChance.nextDelay(0.05, random);
            assertTrue(delay >= 200 && delay < 600, "delay " + delay);
            sum += delay;
        }
        double mean = (double) sum / samples;
        assertTrue(Math.abs(mean - 400) < 5, "mean " + mean); // 20 / chance ticks = one growth per 20 s
    }

    @Test
    void growDelayAtFullChance() {
        LongSupplier random = new Random(2)::nextLong;
        for (int i = 0; i < 1000; i++) {
            long delay = GrowChance.nextDelay(1, random);
            assertTrue(delay >= 10 && delay < 30, "delay " + delay);
        }
    }

    @Test
    void nextGrowTickIsNowPlusTheDelayAndSaturates() {
        long delay = GrowChance.nextDelay(1, new Random(4)::nextLong);
        assertEquals(1000 + delay, GrowChance.nextGrowTick(1000, 1, new Random(4)::nextLong));
        assertEquals(Long.MAX_VALUE, GrowChance.nextGrowTick(Long.MAX_VALUE - 5, 1, new Random(4)::nextLong));
    }

    @Test
    void zeroChanceNeverGrows() {
        LongSupplier random = new Random(3)::nextLong;
        assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(0, random));
        assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(Double.NaN, random));
        assertEquals(Long.MAX_VALUE, GrowChance.nextGrowTick(123_456L, 0, random));
        // a light that never grows takes no number from the world's random source
        LongSupplier noDraw = () -> {
            throw new AssertionError("drew a random number");
        };
        for (double chance : new double[]{0, -0.0, -0.5, Double.NaN, Double.NEGATIVE_INFINITY}) {
            assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(chance, noDraw), "chance " + chance);
        }
    }

    /**
     * A source whose next bounded draw below a bound larger than {@code draw} (and not a power of two) is exactly
     * {@code draw}: the draw takes the top 63 bits modulo the bound, and a value below the bound is never rejected.
     */
    private static LongSupplier drawing(long draw) {
        return () -> draw << 1;
    }

    /** The smallest chance whose largest delay still fits in a long: {@code 30 / Long.MAX_VALUE}, about 3.25e-18. */
    private static final double SMALLEST_FITTING_CHANCE = 30.0 / Long.MAX_VALUE;

    @Test
    void tinyChanceSaturatesInsteadOfOverflowing() {
        // 20 / chance and 10 / chance both round to Long.MAX_VALUE: every draw saturates
        LongSupplier random = new Random(5)::nextLong;
        for (double chance : new double[]{1e-19, Double.MIN_VALUE}) {
            for (int i = 0; i < 1000; i++) {
                assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(chance, random), "chance " + chance);
            }
            assertEquals(Long.MAX_VALUE, GrowChance.nextGrowTick(123_456L, chance, random));
            assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(chance, drawing(0)));
        }
    }

    @Test
    void growDelayJustBelowTheSmallestFittingChance() {
        // bound 6148914691236517888, base 3074457345618258944: draws up to Long.MAX_VALUE - base fit exactly,
        // larger ones saturate
        double chance = Math.nextDown(SMALLEST_FITTING_CHANCE);
        long base = 3_074_457_345_618_258_944L;
        long bound = 6_148_914_691_236_517_888L;
        assertEquals(base, GrowChance.nextDelay(chance, drawing(0)));
        assertEquals(Long.MAX_VALUE - 1, GrowChance.nextDelay(chance, drawing(Long.MAX_VALUE - base - 1)));
        assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(chance, drawing(Long.MAX_VALUE - base)));
        assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(chance, drawing(Long.MAX_VALUE - base + 1)));
        assertEquals(Long.MAX_VALUE, GrowChance.nextDelay(chance, drawing(bound - 1)));
        assertEquals(Long.MAX_VALUE, GrowChance.nextGrowTick(1000, chance, drawing(bound - 1)));
        assertEquals(1000 + base, GrowChance.nextGrowTick(1000, chance, drawing(0)));
    }

    @Test
    void growDelayAtAndAboveTheSmallestFittingChanceIsUnchanged() {
        // at the boundary the largest delay is 512 below Long.MAX_VALUE: nothing saturates
        long base = 3_074_457_345_618_258_432L;
        long bound = 6_148_914_691_236_516_864L;
        for (double chance : new double[]{SMALLEST_FITTING_CHANCE, Math.nextUp(SMALLEST_FITTING_CHANCE)}) {
            assertEquals(base, GrowChance.nextDelay(chance, drawing(0)));
            assertEquals(base + bound - 1, GrowChance.nextDelay(chance, drawing(bound - 1)));
            assertEquals(Long.MAX_VALUE - 512, GrowChance.nextDelay(chance, drawing(bound - 1)));
        }
        // a bit further away
        assertEquals(9_090_909_090_909_090_815L, GrowChance.nextDelay(3.3e-18, drawing(6_060_606_060_606_060_543L)));
        assertEquals(3_030_303_030_303_030_272L, GrowChance.nextDelay(3.3e-18, drawing(0)));
    }

    @Test
    void growDelayAtVeryHighChanceIsZero() {
        // round(20 / 40) = 1, round(10 / 40) = 0: a growth tick on every tick, not a saturated delay
        assertEquals(0, GrowChance.nextDelay(40, drawing(0)));
        assertEquals(0, GrowChance.nextDelay(Double.POSITIVE_INFINITY, new Random(6)::nextLong));
        assertEquals(1000, GrowChance.nextGrowTick(1000, 40, drawing(0)));
    }

    @Test
    void floodlightCostDoublesInConeModes() {
        assertEquals(20, EnergyCost.floodlight(20, BeamMode.STRAIGHT));
        assertEquals(40, EnergyCost.floodlight(20, BeamMode.NARROW_CONE));
        assertEquals(40, EnergyCost.floodlight(20, BeamMode.WIDE_CONE));
    }
}
