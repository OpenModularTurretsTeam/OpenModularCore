package omtteam.openmodularturrets.core.turret;

import omtteam.omlib.core.energy.EnergyAccess;

import java.util.List;

/**
 * The energy drain turret's shot (issue 324): it destroys energy in the items a player wears and holds. The
 * Minecraft layer passes the energy of those items as {@link EnergyAccess} views, in the order armour, main hand,
 * offhand; the drained energy goes nowhere (user decision).
 */
public final class EnergyDrain {
    private EnergyDrain() {
    }

    /**
     * Extracts up to {@code max} energy in total from the items, emptying each in turn before the next.
     *
     * @param max {@link omtteam.openmodularturrets.core.settings.TurretStats#effectStrength}; 0 or less drains
     *            nothing
     * @return the energy destroyed
     */
    public static long drain(List<EnergyAccess> items, long max) {
        long drained = 0;
        for (EnergyAccess item : items) {
            if (drained >= max) break;
            drained += item.extract(max - drained, false);
        }
        return drained;
    }
}
