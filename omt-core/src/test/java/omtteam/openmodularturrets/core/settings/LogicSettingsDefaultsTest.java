package omtteam.openmodularturrets.core.settings;

import omtteam.openmodularturrets.core.base.FakeSettings;
import omtteam.openmodularturrets.core.item.AddonType;
import omtteam.openmodularturrets.core.item.TurretBaseUpgradeType;
import omtteam.openmodularturrets.core.turret.TurretType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A game layer that does not switch upgrades or addons off gets all of them enabled. */
class LogicSettingsDefaultsTest {
    /** Settings that only implement the abstract methods, so the defaults apply. */
    private static final class WithoutSwitches implements LogicSettings {
        private final LogicSettings delegate = new FakeSettings();

        @Override public TurretStats turret(TurretType type) { return delegate.turret(type); }
        @Override public BaseTierStats baseTier(int tier) { return delegate.baseTier(tier); }
        @Override public int expanderStacks(int tier) { return delegate.expanderStacks(tier); }
        @Override public int solarPanelGeneration() { return delegate.solarPanelGeneration(); }
        @Override public int redstoneReactorGeneration() { return delegate.redstoneReactorGeneration(); }
        @Override public boolean turretsNeedAmmo() { return delegate.turretsNeedAmmo(); }
        @Override public boolean dropTurretsOverMaxSimultaneous() { return delegate.dropTurretsOverMaxSimultaneous(); }
        @Override public boolean unownedBasesActive() { return delegate.unownedBasesActive(); }
        @Override public boolean turretsConcealWithoutAddon() { return delegate.turretsConcealWithoutAddon(); }
        @Override public boolean globalCanTargetPlayers() { return delegate.globalCanTargetPlayers(); }
        @Override public boolean globalCanTargetNeutrals() { return delegate.globalCanTargetNeutrals(); }
        @Override public boolean globalCanTargetMobs() { return delegate.globalCanTargetMobs(); }
        @Override public Set<String> blockedEntities() { return delegate.blockedEntities(); }
        @Override public Set<String> forcedHostileEntities() { return delegate.forcedHostileEntities(); }
        @Override public Set<String> forcedNeutralEntities() { return delegate.forcedNeutralEntities(); }
        @Override public int turretWarningDistance() { return delegate.turretWarningDistance(); }
        @Override public int targetSearchTicks() { return delegate.targetSearchTicks(); }
    }

    @Test
    void everyUpgradeAndAddonIsEnabledByDefault() {
        LogicSettings settings = new WithoutSwitches();
        for (TurretBaseUpgradeType type : TurretBaseUpgradeType.values()) {
            assertTrue(settings.upgradeEnabled(type), type::name);
        }
        for (AddonType type : AddonType.values()) {
            assertTrue(settings.addonEnabled(type), type::name);
        }
    }

    @Test
    void aimToleranceIsFiveDegreesByDefault() {
        assertEquals(5.0D, new WithoutSwitches().aimTolerance());
    }
}
