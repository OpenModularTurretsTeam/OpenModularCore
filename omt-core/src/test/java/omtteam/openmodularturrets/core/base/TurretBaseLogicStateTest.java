package omtteam.openmodularturrets.core.base;

import omtteam.omlib.core.machine.MachineMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The {@link TurretBaseLogic.State} constructor without computer control, kept for callers from before it. */
class TurretBaseLogicStateTest {
    @Test
    void stateWithoutComputerControlIsNotComputerControlled() {
        TurretBaseLogic.State state = new TurretBaseLogic.State(1200, MachineMode.ALWAYS_ON, true, true, false, true,
                10, 20, true, 3, 1);
        assertEquals(new TurretBaseLogic.State(1200, MachineMode.ALWAYS_ON, true, true, false, true, 10, 20, true, 3, 1,
                false), state);
    }
}
