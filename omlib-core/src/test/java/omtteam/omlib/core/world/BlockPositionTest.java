package omtteam.omlib.core.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockPositionTest {
    @Test
    void keepsItsCoordinates() {
        BlockPosition pos = new BlockPosition(3, -7, 11);
        assertEquals(3, pos.x());
        assertEquals(-7, pos.y());
        assertEquals(11, pos.z());
    }

    @Test
    void relativeStepsOneBlockTowardsTheFace() {
        BlockPosition pos = new BlockPosition(3, -7, 11);
        assertEquals(new BlockPosition(3, -8, 11), pos.relative(Face.DOWN));
        assertEquals(new BlockPosition(3, -6, 11), pos.relative(Face.UP));
        assertEquals(new BlockPosition(3, -7, 10), pos.relative(Face.NORTH));
        assertEquals(new BlockPosition(3, -7, 12), pos.relative(Face.SOUTH));
        assertEquals(new BlockPosition(2, -7, 11), pos.relative(Face.WEST));
        assertEquals(new BlockPosition(4, -7, 11), pos.relative(Face.EAST));
    }
}
