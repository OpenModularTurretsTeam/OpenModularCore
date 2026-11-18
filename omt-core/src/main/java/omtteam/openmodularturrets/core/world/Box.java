package omtteam.openmodularturrets.core.world;

import omtteam.omlib.core.world.BlockPosition;

/**
 * Axis-aligned box (in world coordinates, or a contraption's plot coordinates, see {@link Pose}).
 */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    /**
     * The cube 3.x used around a block: from {@code pos - radius} to {@code pos + radius} on every axis
     * (block corner coordinates, as 3.x built it with Minecraft's {@code AxisAlignedBB}).
     */
    public static Box around(BlockPosition pos, double radius) {
        return new Box(pos.x() - radius, pos.y() - radius, pos.z() - radius,
                pos.x() + radius, pos.y() + radius, pos.z() + radius);
    }

    /** Whether the point is inside or on the border of the box. */
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    // equals and hashCode written out: the downgraded Java 8 classes must compare doubles like a record does
    // (Double.compare), which JvmDowngrader's generated record methods do not; hashCode computes
    // what a record's does (0-seeded 31 * h + the component's hash, nothing boxed)
    @Override
    public boolean equals(Object o) {
        return o instanceof Box other
                && Double.compare(minX, other.minX) == 0
                && Double.compare(minY, other.minY) == 0
                && Double.compare(minZ, other.minZ) == 0
                && Double.compare(maxX, other.maxX) == 0
                && Double.compare(maxY, other.maxY) == 0
                && Double.compare(maxZ, other.maxZ) == 0;
    }

    @Override
    public int hashCode() {
        int h = Double.hashCode(minX);
        h = 31 * h + Double.hashCode(minY);
        h = 31 * h + Double.hashCode(minZ);
        h = 31 * h + Double.hashCode(maxX);
        h = 31 * h + Double.hashCode(maxY);
        return 31 * h + Double.hashCode(maxZ);
    }
}
