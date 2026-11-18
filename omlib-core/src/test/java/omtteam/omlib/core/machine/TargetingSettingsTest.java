package omtteam.omlib.core.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetingSettingsTest {
    @Test
    void defaultsTargetOnlyMobs() {
        TargetingSettings settings = TargetingSettings.defaults();
        assertFalse(settings.isTargetPlayers());
        assertTrue(settings.isTargetMobs());
        assertFalse(settings.isTargetPassive());
        assertFalse(new TargetingSettings(false, false, false, 0, 0).isTargetMobs());
        assertEquals(0, settings.getRange());
        assertEquals(0, settings.getMaxRange());
    }

    @Test
    void constructorClampsNegativeValues() {
        TargetingSettings settings = new TargetingSettings(false, true, false, -3, -1);
        assertEquals(0, settings.getMaxRange());
        assertEquals(0, settings.getRange());
    }

    @Test
    void constructorLimitsTheRangeToTheMaximum() {
        assertEquals(10, new TargetingSettings(false, true, false, 25, 10).getRange());
        assertEquals(0, new TargetingSettings(false, true, false, -3, 10).getRange(), "a negative range is 0");
    }

    @Test
    void negativeMaxRangeCountsAsZero() {
        TargetingSettings settings = new TargetingSettings(false, true, false, 5, 10);
        settings.setMaxRange(-1);
        assertEquals(0, settings.getMaxRange());
        assertEquals(0, settings.getRange(), "the range is never negative");
    }

    @Test
    void loweringTheMaxRangeLowersTheRange() {
        TargetingSettings settings = new TargetingSettings(false, true, false, 8, 10);
        settings.setMaxRange(6);
        assertEquals(6, settings.getMaxRange());
        assertEquals(6, settings.getRange());
        settings.setMaxRange(20);
        assertEquals(20, settings.getMaxRange());
        assertEquals(6, settings.getRange(), "raising the maximum keeps the range");
    }

    @Test
    void rangeOutsideTheLimitsIsIgnored() {
        TargetingSettings settings = new TargetingSettings(false, true, false, 5, 10);
        settings.setRange(11);
        settings.setRange(-1);
        assertEquals(5, settings.getRange());
        settings.setRange(10);
        assertEquals(10, settings.getRange());
    }

    @Test
    void rangeEqualToTheMaximumIsAccepted() {
        TargetingSettings settings = new TargetingSettings(false, true, false, 0, 10);
        settings.setRange(10);
        assertEquals(10, settings.getRange());
        settings.setRange(0);
        assertEquals(0, settings.getRange());
    }

    @Test
    void targetSwitchesChangeOnlyTheirOwnFlag() {
        TargetingSettings settings = new TargetingSettings(false, false, false, 0, 0);
        settings.setTargetPlayers(true);
        assertTrue(settings.isTargetPlayers());
        assertFalse(settings.isTargetMobs());
        assertFalse(settings.isTargetPassive());
        settings.setTargetMobs(true);
        assertTrue(settings.isTargetMobs());
        assertFalse(settings.isTargetPassive());
        settings.setTargetPassive(true);
        assertTrue(settings.isTargetPassive());
        settings.setTargetPlayers(false);
        settings.setTargetMobs(false);
        settings.setTargetPassive(false);
        assertFalse(settings.isTargetPlayers());
        assertFalse(settings.isTargetMobs());
        assertFalse(settings.isTargetPassive());
    }
}
