package omtteam.omlib.core.card;

import omtteam.omlib.core.security.PlayerInfo;
import omtteam.omlib.core.security.TrustManager;

/**
 * Copying settings of a turret base or camouflage trap to a memory card and writing them to another one, and who may
 * do that.
 * <p>
 * The card holds the trust list and the redstone mode, which are admin settings, and copying reads them, so both
 * directions need the admin access level (the owner always has it) on bases and on camouflage traps. The camouflage
 * part of a card has its own rules, see {@link omtteam.omlib.core.camo.CamoPermissions}; the base settings are read
 * and written by OMT's {@code BaseCardRules}.
 * <p>
 * It is in OMLib's core, not OMT's, because the released OMT 4.0.1 for 1.12.2 calls it here (decided by the user,
 * 2026-11-19).
 */
public final class MemoryCardRules {
    private MemoryCardRules() {
    }

    /** Reading the settings of a base or trap onto a card. */
    public static boolean canCopy(TrustManager trust, PlayerInfo player) {
        return trust.isAdmin(player);
    }

    /** Writing the settings of a card to a base or trap. */
    public static boolean canPaste(TrustManager trust, PlayerInfo player) {
        return trust.isAdmin(player);
    }

    /** The trust list of a base or trap as stored on a card. */
    public static MemoryCardData.TrustSettings captureTrust(TrustManager trust) {
        return new MemoryCardData.TrustSettings(trust.getTrustedPlayers());
    }

    /**
     * Writes a trust list to a base or trap, without permission checks. It replaces the existing list, keeps the owner
     * of the target (the owner is never part of the list) and clamps the levels to the ones the target uses.
     */
    public static void applyTrust(MemoryCardData.TrustSettings settings, TrustManager trust) {
        trust.load(trust.getOwner(), settings.trustedPlayers());
    }
}
