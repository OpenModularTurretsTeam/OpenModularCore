package omtteam.openmodularlighting.core.math;

import java.util.function.LongSupplier;

/**
 * Uniform draws below a bound from a source of random {@code long}s, for the core's timers. Not meant for the game
 * layers: they pass their random source to the core as a {@link LongSupplier} (for example {@code () ->
 * level.random.nextLong()} or {@code world.rand::nextLong}) and the core draws from it here.
 * <p>
 * The algorithm is the one of the JDK's default {@code java.util.random.RandomGenerator.nextLong(long)}
 * ({@code jdk.internal.util.random.RandomSupport.boundedNextLong}, the same in Java 17 and 21), so the same stream of
 * {@code long}s gives the same results as that method, also on runtimes without it.
 */
public final class BoundedDraw {
    private BoundedDraw() {
    }

    /**
     * A value uniformly distributed in {@code [0, bound)}. A power-of-two bound takes the low bits of one
     * {@code nextLong()}; any other bound takes the top 63 bits modulo the bound and draws again in the rare case that
     * the result would be biased, so it can take more than one {@code long} from {@code random}.
     *
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public static long nextLong(LongSupplier random, long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long m = bound - 1;
        long r = random.getAsLong();
        if ((bound & m) == 0L) {
            // the bound is a power of 2
            return r & m;
        }
        // reject the over-represented candidates (the last, incomplete multiple of bound below 2^63)
        long u = r >>> 1;
        while (u + m - (r = u % bound) < 0L) {
            u = random.getAsLong() >>> 1;
        }
        return r;
    }
}
