package omtteam.omlib.core;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import omtteam.omlib.core.camo.CamoLight;
import omtteam.omlib.core.security.AccessLevel;
import omtteam.omlib.core.security.AccessProfile;
import omtteam.omlib.core.security.PlayerIdentity;
import omtteam.omlib.core.security.PlayerInfo;
import omtteam.omlib.core.world.BlockPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * equals and hashCode of the core value records. EqualsVerifier builds records through their canonical constructors,
 * so values those reject are kept out: {@code NULL_FIELDS} (the constructors reject their arguments, so those fields
 * are never null), light levels inside {@code [0, 15]} for {@link CamoLight} (the constructor clamps) and sorted level
 * lists with {@code NONE} and {@code ADMIN} for {@link AccessProfile} (prefab values, which also count as non-null).
 */
class EqualsContractTest {
    @Test
    void blockPosition() {
        EqualsVerifier.forClass(BlockPosition.class).verify();
    }

    @Test
    void camoLight() {
        EqualsVerifier.forClass(CamoLight.class)
                .withPrefabValuesForField("value", 1, 2)
                .withPrefabValuesForField("opacity", 1, 2)
                .verify();
    }

    @Test
    void accessProfile() {
        EqualsVerifier.forClass(AccessProfile.class)
                .withPrefabValuesForField("levels", List.of(AccessLevel.NONE, AccessLevel.ADMIN),
                        List.of(AccessLevel.NONE, AccessLevel.OPEN_GUI, AccessLevel.ADMIN))
                .verify();
    }

    @Test
    void playerIdentity() {
        EqualsVerifier.forClass(PlayerIdentity.class).suppress(Warning.NULL_FIELDS).verify();
    }

    @Test
    void playerInfo() {
        EqualsVerifier.forClass(PlayerInfo.class).suppress(Warning.NULL_FIELDS).verify();
    }
}
