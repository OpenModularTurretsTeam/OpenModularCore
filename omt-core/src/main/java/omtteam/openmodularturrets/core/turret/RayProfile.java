package omtteam.openmodularturrets.core.turret;

import java.util.Objects;
import java.util.function.IntToDoubleFunction;

/**
 * Damage rules of a ray turret (3.x {@code LaserTurretTileEntity}, {@code RailGunTurretTileEntity},
 * {@code ArcTurretTileEntity}), applied by {@link RayMath}.
 *
 * @param armorModifier damage modifier for the directly hit entity by its armor (3.x {@code getDamageModifier})
 * @param normalShare   share of the damage dealt as normal damage
 * @param bypassShare   share of the damage dealt as armor-piercing damage
 * @param arcChain      whether a hit jumps on to a nearby entity ({@link RayMath#ARC_HOPS})
 */
public record RayProfile(IntToDoubleFunction armorModifier, float normalShare, float bypassShare, boolean arcChain) {
    /** {@code (30 - armor) / 20 + 0.1}: 1.6x without armor down to 0.6x at 20 armor; only normal damage. */
    public static final RayProfile LASER = new RayProfile(armor -> (30 - armor) / 20F + 0.1F, 1F, 0F, false);
    /** {@code armor / 20 + 0.6}: 0.6x without armor up to 1.6x at 20 armor; only armor-piercing damage. */
    public static final RayProfile RAILGUN = new RayProfile(armor -> armor / 20F + 0.6F, 0F, 1F, false);
    /** Ignores armor; 60 % normal and 40 % armor-piercing damage; jumps on to a nearby entity. */
    public static final RayProfile ARC = new RayProfile(armor -> 1F, 0.6F, 0.4F, true);

    // equals and hashCode written out: the downgraded Java 8 classes must compare floats like a record does
    // (Float.compare), which JvmDowngrader's generated record methods do not; hashCode computes
    // what a record's does (0-seeded 31 * h + the component's hash, nothing boxed)
    @Override
    public boolean equals(Object o) {
        return o instanceof RayProfile other
                && Objects.equals(armorModifier, other.armorModifier)
                && Float.compare(normalShare, other.normalShare) == 0
                && Float.compare(bypassShare, other.bypassShare) == 0
                && arcChain == other.arcChain;
    }

    @Override
    public int hashCode() {
        int h = Objects.hashCode(armorModifier);
        h = 31 * h + Float.hashCode(normalShare);
        h = 31 * h + Float.hashCode(bypassShare);
        return 31 * h + Boolean.hashCode(arcChain);
    }
}
