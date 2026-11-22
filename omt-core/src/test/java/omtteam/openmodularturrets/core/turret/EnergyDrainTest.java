package omtteam.openmodularturrets.core.turret;

import omtteam.omlib.core.energy.EnergyAccess;
import omtteam.omlib.core.energy.EnergyStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EnergyDrainTest {
    private static EnergyStore item(long amount, long maxExtract) {
        EnergyStore store = new EnergyStore(10_000, 10_000, maxExtract, () -> {
        });
        store.generate(amount);
        return store;
    }

    @Test
    void emptiesTheItemsInOrderUpToTheMaximum() {
        EnergyStore helmet = item(300, 10_000);
        EnergyStore sword = item(500, 10_000);
        EnergyStore shield = item(400, 10_000);
        assertEquals(700, EnergyDrain.drain(List.of(helmet, sword, shield), 700));
        assertEquals(0, helmet.getAmount());
        assertEquals(100, sword.getAmount());
        assertEquals(400, shield.getAmount(), "untouched once the maximum is reached");
    }

    @Test
    void drainsLessWhenTheItemsHoldLess() {
        EnergyStore boots = item(200, 10_000);
        assertEquals(200, EnergyDrain.drain(List.of(boots), 1000));
        assertEquals(0, boots.getAmount());
    }

    @Test
    void respectsTheItemsExtractionLimits() {
        EnergyStore slow = item(1000, 50);
        EnergyStore locked = item(1000, 0);
        EnergyStore last = item(1000, 10_000);
        assertEquals(500, EnergyDrain.drain(List.of(slow, locked, last), 500));
        assertEquals(950, slow.getAmount());
        assertEquals(1000, locked.getAmount(), "an item that allows no extraction keeps its energy");
        assertEquals(550, last.getAmount());
    }

    @Test
    void nothingWithoutItemsOrMaximum() {
        EnergyStore chest = item(1000, 10_000);
        assertEquals(0, EnergyDrain.drain(List.<EnergyAccess>of(), 500));
        assertEquals(0, EnergyDrain.drain(List.of(chest), 0));
        assertEquals(0, EnergyDrain.drain(List.of(chest), -5));
        assertEquals(1000, chest.getAmount());
    }

    @Test
    void targetsCarryNoDrainableEnergyUnlessTheGameLayerSaysSo() {
        assertFalse(new FakePositionTarget(0, 0, 0).hasDrainableEnergy());
    }

    @Test
    void stopsAskingItemsOnceTheMaximumIsDrained() {
        EnergyAccess untouchable = new EnergyAccess() {
            @Override
            public long getAmount() {
                return 1000;
            }

            @Override
            public long getCapacity() {
                return 1000;
            }

            @Override
            public long insert(long maxAmount, boolean simulate) {
                throw new AssertionError("not inserted into");
            }

            @Override
            public long extract(long maxAmount, boolean simulate) {
                throw new AssertionError("not asked once the maximum is drained");
            }
        };
        assertEquals(500, EnergyDrain.drain(List.of(item(500, 10_000), untouchable), 500));
    }
}
