package omtteam.openmodularlighting.core.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * The core's bounded draw gives the same values as the JDK's default {@code RandomGenerator.nextLong(long)} on the
 * same stream of longs, and takes the same number of them. A game layer that passes its random source as a lambda
 * ({@code () -> level.random.nextLong()}) therefore gets the same delays as it did when the core took a
 * {@code RandomGenerator} and called that default method.
 * <p>
 * {@link Random} does not override {@code nextLong(long)}, so on Java 17 and newer it runs the interface's default
 * method over {@link Random#nextLong()}: exactly what a {@code RandomGenerator} lambda over the same {@code Random}
 * runs. The method is called by reflection, so this class does not name {@code java.util.random} at all: the Java 8
 * run downgrades the test classes too and would otherwise bundle stubs for it. It skips this class.
 */
@EnabledForJreRange(min = JRE.JAVA_17)
class BoundedDrawJdkTest {
    private static final long[] BOUNDS = {
            1, 2, 3, 5, 7, 8, 10, 20, 200, 400, 1000, 1L << 31, (1L << 31) + 1, 1L << 40, 3L << 40,
            (1L << 62) - 1, 1L << 62, (1L << 62) + 1, 3L << 61, (3L << 61) + 1, Long.MAX_VALUE - 1, Long.MAX_VALUE,
            // the grow light's bounds: round(20 / chance) for 0.05 and near the overflow
            Math.round(20 / 0.05), Math.round(20 / 3.3e-18)
    };

    /** {@code RandomGenerator.nextLong(long)}, as {@link Random} inherits it. */
    private static Method jdkNextLong;

    @BeforeAll
    static void findTheJdkMethod() throws NoSuchMethodException {
        jdkNextLong = Random.class.getMethod("nextLong", long.class);
        assertEquals("java.util.random.RandomGenerator", jdkNextLong.getDeclaringClass().getName(),
                "Random must inherit the default nextLong(long)");
    }

    private static long jdk(Random random, long bound) throws ReflectiveOperationException {
        return (long) jdkNextLong.invoke(random, bound);
    }

    @Test
    void sameValuesAsTheJdkDefaultOnTheSameStream() throws ReflectiveOperationException {
        for (long seed = 0; seed < 4; seed++) {
            Random ours = new Random(seed);
            Random jdks = new Random(seed);
            for (long bound : BOUNDS) {
                for (int i = 0; i < 500; i++) {
                    assertEquals(jdk(jdks, bound), BoundedDraw.nextLong(ours::nextLong, bound),
                            "seed " + seed + ", bound " + bound + ", draw " + i);
                }
                // both took the same number of longs
                assertEquals(jdks.nextLong(), ours.nextLong(), "seed " + seed + ", bound " + bound);
            }
        }
    }

    @Test
    void sameValuesForRandomBounds() throws ReflectiveOperationException {
        Random bounds = new Random(42);
        Random ours = new Random(7);
        Random jdks = new Random(7);
        for (int i = 0; i < 20_000; i++) {
            long bound = 1 + (bounds.nextLong() >>> (1 + bounds.nextInt(63)));
            assertEquals(jdk(jdks, bound), BoundedDraw.nextLong(ours::nextLong, bound), "bound " + bound);
        }
        assertEquals(jdks.nextLong(), ours.nextLong());
    }
}
