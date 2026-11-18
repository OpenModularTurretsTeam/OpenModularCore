package omtteam.openmodularlighting.core.formula;

import omtteam.openmodularlighting.core.math.BoundedDraw;

import java.util.function.LongSupplier;

/**
 * Timing of the grow light's growth ticks (1.7.10 {@code RandomUtil.getRandomTickTimeoutFromFloatChance}).
 */
public final class GrowChance {
    private GrowChance() {
    }

    /**
     * Ticks until the next growth tick: {@code random(round(20 / chance)) + round(0.5 × 20 / chance)}, i.e. uniform in
     * {@code [10 / chance, 30 / chance)} with a mean of {@code 20 / chance} ticks, one growth tick per {@code 1 / chance}
     * seconds on average. With {@code chance <= 0} the crop never grows ({@link Long#MAX_VALUE}). A chance so small
     * that the delay does not fit in a {@code long} (below about {@code 3.25e-18}) saturates at {@link Long#MAX_VALUE}
     * instead of overflowing, so the crop practically never grows.
     *
     * @param random source of uniformly distributed {@code long}s (for example the world's random number generator)
     */
    public static long nextDelay(double chance, LongSupplier random) {
        if (!(chance > 0)) return Long.MAX_VALUE;
        long bound = Math.max(1, Math.round(20 / chance));
        long base = Math.round(0.5 * 20 / chance);
        // both parts are at least 0, so a negative sum is an overflow
        long delay = BoundedDraw.nextLong(random, bound) + base;
        return delay < 0 ? Long.MAX_VALUE : delay;
    }

    /** The game time of the next growth tick, saturating instead of overflowing. */
    public static long nextGrowTick(long now, double chance, LongSupplier random) {
        long delay = nextDelay(chance, random);
        return delay > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + delay;
    }
}
