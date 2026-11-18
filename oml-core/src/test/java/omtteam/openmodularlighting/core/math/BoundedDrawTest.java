package omtteam.openmodularlighting.core.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.function.LongSupplier;

/** The bounded draw on fixed streams, so the results are pinned on every runtime (see also BoundedDrawJdkTest). */
class BoundedDrawTest {
    /** Returns the given longs in order and counts how many were taken. */
    private static final class Stream implements LongSupplier {
        private final long[] values;
        int taken;

        Stream(long... values) {
            this.values = values;
        }

        @Override
        public long getAsLong() {
            return values[taken++];
        }
    }

    @Test
    void powerOfTwoBoundsTakeTheLowBitsOfOneLong() {
        Stream stream = new Stream(-1L, 0x1234_5678_9abc_def5L, 0x8000_0000_0000_0000L);
        assertEquals(7, BoundedDraw.nextLong(stream, 8));
        assertEquals(0x5, BoundedDraw.nextLong(stream, 16));
        assertEquals(0, BoundedDraw.nextLong(stream, 1L << 62));
        assertEquals(3, stream.taken);
        assertEquals(0, BoundedDraw.nextLong(new Stream(-1L), 1));
    }

    @Test
    void otherBoundsTakeTheTopBitsModuloTheBound() {
        // (7 >>> 1) % 3 = 0, (802 >>> 1) % 400 = 1, (400_007 * 2 >>> 1) % 400 = 7
        Stream stream = new Stream(7, 802, 400_007 * 2);
        assertEquals(0, BoundedDraw.nextLong(stream, 3));
        assertEquals(1, BoundedDraw.nextLong(stream, 400));
        assertEquals(7, BoundedDraw.nextLong(stream, 400));
        assertEquals(3, stream.taken);
    }

    @Test
    void biasedCandidatesAreDrawnAgain() {
        // bound 2^62 + 1: a top-63-bits value of 2^62 + 1 or more lies in the incomplete second multiple of the
        // bound, so it is rejected and the next long is taken
        long bound = (1L << 62) + 1;
        Stream stream = new Stream(-1L, -1L, 6);
        assertEquals(3, BoundedDraw.nextLong(stream, bound));
        assertEquals(3, stream.taken);
        // the largest value that is not rejected: 2^62 (the first multiple ends at 2^62 + 1, the bound itself)
        Stream largest = new Stream((bound - 1) << 1);
        assertEquals(bound - 1, BoundedDraw.nextLong(largest, bound));
        assertEquals(1, largest.taken);
        // a value of exactly the bound would give 0, but its multiple is incomplete: drawn again
        Stream atBound = new Stream(bound << 1, 2);
        assertEquals(1, BoundedDraw.nextLong(atBound, bound));
        assertEquals(2, atBound.taken);
    }

    @Test
    void theLargestBoundRejectsOnlyTheLargestTopBits() {
        // bound 2^63 - 1: the top 63 bits are the value itself, except 2^63 - 1, the only value of the incomplete
        // second multiple, which is drawn again
        Stream stream = new Stream(-1L, -4L);
        assertEquals(Long.MAX_VALUE - 1, BoundedDraw.nextLong(stream, Long.MAX_VALUE));
        assertEquals(2, stream.taken);
        assertEquals(0, BoundedDraw.nextLong(new Stream(1), Long.MAX_VALUE));
    }

    @Test
    void staysInTheBoundAndIsUniform() {
        Random random = new Random(5);
        int[] counts = new int[5];
        int samples = 50_000;
        for (int i = 0; i < samples; i++) {
            long value = BoundedDraw.nextLong(random::nextLong, 5);
            assertTrue(value >= 0 && value < 5, "value " + value);
            counts[(int) value]++;
        }
        for (int count : counts) {
            assertTrue(Math.abs(count - samples / 5) < 500, "count " + count);
        }
    }

    @Test
    void boundMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> BoundedDraw.nextLong(new Stream(1), 0));
        assertThrows(IllegalArgumentException.class, () -> BoundedDraw.nextLong(new Stream(1), -8));
    }
}
