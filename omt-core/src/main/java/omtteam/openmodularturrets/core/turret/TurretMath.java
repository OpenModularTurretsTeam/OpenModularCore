package omtteam.openmodularturrets.core.turret;

import omtteam.omlib.core.world.BlockPosition;
import omtteam.omlib.core.world.Face;
import omtteam.openmodularturrets.core.target.TargetEntity;
import omtteam.openmodularturrets.core.world.Vector3;

import java.util.Objects;

/**
 * Aiming maths shared by the server (where to shoot) and the renderer (how to rotate the model), so both agree.
 * <p>
 * Every mounting face has a right-handed local frame: {@code up} is the face direction (away from the base),
 * {@code forward} is where a turret with yaw 0 points, {@code right = up × forward}. Yaw turns around {@code up}
 * (0° = forward, 90° = right); pitch is the elevation above the mounting plane (90° = straight away from the base).
 */
public final class TurretMath {
    /** The aim point is lowered by 0.1 blocks. */
    private static final double AIM_DROP = 0.1D;
    /**
     * Lowest local pitch a turret can aim at; anything lower would point into its own base (the base block covers
     * up to about -45° below the mounting plane). New; the 3.x turrets had no effective limits.
     */
    public static final float MIN_PITCH = -45.0F;

    private TurretMath() {
    }

    /** Local axes of a mounting face, in world coordinates. */
    public record Frame(Vector3 right, Vector3 up, Vector3 forward) {
        /** Expresses a world-space vector in this frame: x = right, y = up, z = forward. */
        public Vector3 toLocal(Vector3 world) {
            return new Vector3(world.dot(right), world.dot(up), world.dot(forward));
        }

        /** Inverse of {@link #toLocal}. */
        public Vector3 toWorld(Vector3 local) {
            return right.scale(local.x()).add(up.scale(local.y())).add(forward.scale(local.z()));
        }
    }

    /** Local yaw and pitch in degrees. */
    public record Angles(float yaw, float pitch) {
        // equals and hashCode written out: the downgraded Java 8 classes must compare floats like a record does
        // (Float.compare), which JvmDowngrader's generated record methods do not; hashCode computes
        // what a record's does (0-seeded 31 * h + the component's hash, nothing boxed)
        @Override
        public boolean equals(Object o) {
            return o instanceof Angles other
                    && Float.compare(yaw, other.yaw) == 0
                    && Float.compare(pitch, other.pitch) == 0;
        }

        @Override
        public int hashCode() {
            int h = Float.hashCode(yaw);
            return 31 * h + Float.hashCode(pitch);
        }
    }

    /**
     * Turrets on top of a base face +Z (south) at yaw 0, turrets hanging below face -Z, turrets on side faces face
     * world up at yaw 0 (towards the sky).
     */
    public static Frame frame(Face face) {
        Vector3 up = Vector3.of(face);
        Vector3 forward = switch (face) {
            case UP -> new Vector3(0, 0, 1);
            case DOWN -> new Vector3(0, 0, -1);
            default -> new Vector3(0, 1, 0);
        };
        return new Frame(up.cross(forward), up, forward);
    }

    /** Yaw/pitch that point a turret on {@code face} along {@code direction} (world space, any length). */
    public static Angles anglesFor(Face face, Vector3 direction) {
        Vector3 local = frame(face).toLocal(direction);
        double horizontal = Math.sqrt(local.x() * local.x() + local.z() * local.z());
        float yaw = (float) Math.toDegrees(Math.atan2(local.x(), local.z()));
        float pitch = (float) Math.toDegrees(Math.atan2(local.y(), horizontal));
        return new Angles(yaw, pitch);
    }

    /** Whether a turret on {@code face} can turn towards {@code direction} without pointing into its base. */
    public static boolean canAim(Face face, Vector3 direction) {
        return anglesFor(face, direction).pitch() >= MIN_PITCH;
    }

    /** {@code angles} with the pitch limited to {@link #MIN_PITCH}. */
    public static Angles limit(Angles angles) {
        return angles.pitch() >= MIN_PITCH ? angles : new Angles(angles.yaw(), MIN_PITCH);
    }

    /** World direction (unit length) of a turret on {@code face} with the given angles. */
    public static Vector3 direction(Face face, Angles angles) {
        double yaw = Math.toRadians(angles.yaw());
        double pitch = Math.toRadians(angles.pitch());
        Vector3 local = new Vector3(Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        return frame(face).toWorld(local);
    }

    /** Where to shoot and how fast, see {@link #lead}. */
    public record Lead(Vector3 direction, float speedFactor) {
        // equals and hashCode written out: the downgraded Java 8 classes must compare floats like a record does
        // (Float.compare), which JvmDowngrader's generated record methods do not; hashCode computes
        // what a record's does (0-seeded 31 * h + the component's hash, nothing boxed)
        @Override
        public boolean equals(Object o) {
            return o instanceof Lead other
                    && Objects.equals(direction, other.direction)
                    && Float.compare(speedFactor, other.speedFactor) == 0;
        }

        @Override
        public int hashCode() {
            int h = Objects.hashCode(direction);
            return 31 * h + Float.hashCode(speedFactor);
        }
    }

    /**
     * 3.x {@code ProjectileTurret.doTargetedShot}, now with the real flight physics: the flight time follows from
     * the distance and the speed (with air drag), the aim point moves along the target's velocity for that time and
     * is raised by how far the projectile drops during it. The aim point is then lowered by 0.1 and the projectile
     * speed scaled so it arrives in time.
     *
     * @param offset   target aim point minus turret centre
     * @param velocity target movement per tick
     * @param speed    launch speed in blocks per tick
     * @param gravity  downward acceleration of the projectile per tick
     */
    public static Lead lead(Vector3 offset, Vector3 velocity, double speed, double gravity) {
        double distance = offset.length();
        if (distance < 1.0E-6) return new Lead(offset, 1.0F);
        double time = ProjectileMath.flightTime(distance, speed);
        Vector3 adjusted = offset.add(velocity.scale(time));
        float speedFactor = (float) (adjusted.length() / distance);
        Vector3 aim = adjusted.add(new Vector3(0, ProjectileMath.dropAfter(time, gravity) - AIM_DROP, 0));
        return new Lead(aim, speedFactor);
    }

    /**
     * One tick of a turret with a limited turn rate (issue 539): yaw (the shorter way round) and pitch each move at
     * most {@code maxDegrees} towards {@code to}; once within reach the result is {@code to}. A {@code maxDegrees} of
     * 0 or less turns instantly. The yaw stays in [-180, 180).
     *
     * @param maxDegrees {@link omtteam.openmodularturrets.core.settings.TurretStats#turnRate}
     */
    public static Angles turnTowards(Angles from, Angles to, float maxDegrees) {
        if (maxDegrees <= 0.0F) return to;
        float yawStep = angleDifference(from.yaw(), to.yaw());
        float pitchStep = to.pitch() - from.pitch();
        if (Math.abs(yawStep) <= maxDegrees && Math.abs(pitchStep) <= maxDegrees) return to;
        float yaw = Math.abs(yawStep) <= maxDegrees ? to.yaw()
                : from.yaw() + Math.copySign(maxDegrees, yawStep);
        float pitch = Math.abs(pitchStep) <= maxDegrees ? to.pitch()
                : from.pitch() + Math.copySign(maxDegrees, pitchStep);
        return new Angles(angleDifference(0.0F, yaw), pitch);
    }

    /**
     * Angle in degrees between the directions of two aims on the same face, 0–180: how far a turret aiming at
     * {@code a} points off {@code b}.
     */
    public static double aimError(Angles a, Angles b) {
        double yawA = Math.toRadians(a.yaw());
        double pitchA = Math.toRadians(a.pitch());
        double yawB = Math.toRadians(b.yaw());
        double pitchB = Math.toRadians(b.pitch());
        double cos = Math.cos(pitchA) * Math.cos(pitchB) * Math.cos(yawA - yawB) + Math.sin(pitchA) * Math.sin(pitchB);
        return Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, cos))));
    }

    /** Most upward speed a push adds, in blocks per tick (vanilla knockback lifts by 0.4). */
    public static final double MAX_PUSH_LIFT = 0.4D;
    /** A pull ends this far (horizontally) from the turret's centre, so it never carries the target past it. */
    public static final double PULL_STOP_DISTANCE = 1.0D;
    /** Horizontal distances below this (about 1e-6, a power of two so that it is exact) have no direction. */
    static final double MIN_DIRECTION = 0x1p-20;

    /**
     * The velocity change of one shot of the repelling or tractor turret (issue 324), in blocks per tick, which the
     * Minecraft layer adds to the target's motion. A positive {@code strength} pushes the target horizontally away
     * from the turret's centre and lifts it by {@code min(strength, MAX_PUSH_LIFT)}, as knockback does; a negative
     * one pulls it horizontally towards the turret, at most as far as {@link #PULL_STOP_DISTANCE} from it. A target
     * straight above or below the turret is only lifted by a push and not pulled.
     *
     * @param turret   the turret's block, in the base's coordinates like the target
     * @param strength {@link omtteam.openmodularturrets.core.settings.TurretStats#effectStrength}, negative to pull
     */
    public static Vector3 impulse(BlockPosition turret, TargetEntity target, double strength) {
        double dx = target.getX() - (turret.x() + 0.5D);
        double dz = target.getZ() - (turret.z() + 0.5D);
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (strength > 0.0D) {
            double lift = Math.min(strength, MAX_PUSH_LIFT);
            if (distance < MIN_DIRECTION) return new Vector3(0.0D, lift, 0.0D);
            return new Vector3(dx / distance * strength, lift, dz / distance * strength);
        }
        double speed = Math.min(-strength, distance - PULL_STOP_DISTANCE);
        if (speed <= 0.0D) return new Vector3(0.0D, 0.0D, 0.0D);
        return new Vector3(-dx / distance * speed, 0.0D, -dz / distance * speed);
    }

    /**
     * Shortest signed difference {@code to - from} in degrees, in [-180, 180).
     */
    public static float angleDifference(float from, float to) {
        float difference = (to - from) % 360.0F;
        if (difference >= 180.0F) difference -= 360.0F;
        if (difference < -180.0F) difference += 360.0F;
        return difference;
    }
}
