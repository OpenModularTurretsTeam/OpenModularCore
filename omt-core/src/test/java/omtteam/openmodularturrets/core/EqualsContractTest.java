package omtteam.openmodularturrets.core;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import omtteam.openmodularturrets.core.base.AmmoDraw;
import omtteam.openmodularturrets.core.base.TurretBaseLogic;
import omtteam.openmodularturrets.core.settings.TurretStats;
import omtteam.openmodularturrets.core.storage.SlotCapacity;
import omtteam.openmodularturrets.core.turret.AmmoType;
import omtteam.openmodularturrets.core.turret.ProjectileMath;
import omtteam.openmodularturrets.core.turret.RayProfile;
import omtteam.openmodularturrets.core.turret.TurretMath;
import omtteam.openmodularturrets.core.world.Box;
import omtteam.openmodularturrets.core.world.Neighbour;
import omtteam.openmodularturrets.core.world.Pose;
import omtteam.openmodularturrets.core.world.Quaternion;
import omtteam.openmodularturrets.core.world.Vector3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.IntToDoubleFunction;

/**
 * equals and hashCode of the core value records and of {@link AmmoType}. The Java 8 run checks the downgraded record
 * methods too; the records with {@code float} or {@code double} components write theirs out, because the downgraded
 * ones compared those with {@code ==} (so {@code 0.0} equalled {@code -0.0} and {@code NaN} did not equal itself).
 * EqualsVerifier builds records through their canonical constructors, so values those reject are kept out:
 * {@code NULL_FIELDS} (the constructors reject their arguments, so those fields are never null) and valid tier lists
 * for {@link Neighbour.Expanders} (prefab values, which also count as non-null).
 */
class EqualsContractTest {
    @Test
    void ammoDraw() {
        EqualsVerifier.forClass(AmmoDraw.class).verify();
    }

    @Test
    void baseState() {
        EqualsVerifier.forClass(TurretBaseLogic.State.class).verify();
    }

    @Test
    void turretStats() {
        EqualsVerifier.forClass(TurretStats.class).verify();
    }

    @Test
    void ammoType() {
        EqualsVerifier.forClass(AmmoType.class).suppress(Warning.NULL_FIELDS).verify();
    }

    @Test
    void rayProfile() {
        // named classes: EqualsVerifier cannot load the hidden classes of lambdas
        EqualsVerifier.forClass(RayProfile.class)
                .withPrefabValues(IntToDoubleFunction.class, new Constant(1), new Constant(2))
                .verify();
    }

    private record Constant(double value) implements IntToDoubleFunction {
        @Override
        public double applyAsDouble(int armor) {
            return value;
        }
    }

    @Test
    void projectileSplit() {
        EqualsVerifier.forClass(ProjectileMath.Split.class).verify();
    }

    @Test
    void angles() {
        EqualsVerifier.forClass(TurretMath.Angles.class).verify();
    }

    @Test
    void lead() {
        EqualsVerifier.forClass(TurretMath.Lead.class).verify();
    }

    @Test
    void box() {
        EqualsVerifier.forClass(Box.class).verify();
    }

    @Test
    void neighbourTurret() {
        EqualsVerifier.forClass(Neighbour.Turret.class).suppress(Warning.NULL_FIELDS).verify();
    }

    @Test
    void neighbourExpanders() {
        EqualsVerifier.forClass(Neighbour.Expanders.class)
                .withPrefabValuesForField("tiers", List.of(1), List.of(2, 3))
                .verify();
    }

    @Test
    void slotCapacity() {
        EqualsVerifier.forClass(SlotCapacity.class).verify();
    }

    @Test
    void vector3() {
        EqualsVerifier.forClass(Vector3.class).verify();
    }

    @Test
    void quaternion() {
        EqualsVerifier.forClass(Quaternion.class).verify();
    }

    @Test
    void pose() {
        EqualsVerifier.forClass(Pose.class).verify();
    }
}
