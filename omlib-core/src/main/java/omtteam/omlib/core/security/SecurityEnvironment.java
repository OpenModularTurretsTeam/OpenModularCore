package omtteam.omlib.core.security;

import java.util.Optional;
import java.util.UUID;

/**
 * What the permission system needs to know about the world and the config. Implemented by the Minecraft layer.
 */
public interface SecurityEnvironment {
    /**
     * The scoreboard team of a player, looked up by name (works for offline players too).
     */
    Optional<String> getTeam(UUID uuid, String name);

    /** Operators may open every owned block and are never treated as strangers. */
    boolean operatorsCanAccessOwnedBlocks();

    /** Trusted players are hurt too when something of the owner accidentally hits them. */
    boolean damageTrustedPlayers();

    /**
     * Whether the owner shares ownership with the player (OMLib {@code /omlib share}); a shared owner counts as owner
     * in every permission decision. Without sharing, nobody is a shared owner.
     */
    default boolean isSharedOwner(PlayerIdentity owner, PlayerIdentity player) {
        return false;
    }

    /**
     * OMLib config {@code offlineModeSupport}: players also match a stored owner or trust list entry by name (ignoring
     * case), as in 3.x. Off by default: the UUID alone is the identity.
     */
    default boolean offlineModeSupport() {
        return false;
    }
}
