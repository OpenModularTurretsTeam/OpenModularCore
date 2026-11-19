package omtteam.openmodularturrets.core.card;

import omtteam.omlib.core.card.MemoryCardData;
import omtteam.omlib.core.card.MemoryCardRules;
import omtteam.openmodularturrets.core.base.TurretBaseLogic;

/**
 * Copying the settings of a turret base to a memory card and writing them to another base: the base part of a card.
 * <p>
 * The card data, the trust part and who may copy and paste are in OMLib's core ({@link MemoryCardData},
 * {@link MemoryCardRules}).
 */
public final class BaseCardRules {
    private BaseCardRules() {
    }

    /** The settings of a base as stored on a card. */
    public static MemoryCardData.BaseSettings capture(TurretBaseLogic logic) {
        var targeting = logic.getTargetingSettings();
        return new MemoryCardData.BaseSettings(logic.getMode(), logic.isMultiTargeting(), targeting.isTargetPlayers(),
                targeting.isTargetMobs(), targeting.isTargetPassive(), targeting.getRange());
    }

    /**
     * Writes card settings to a base, without permission checks (the caller checked
     * {@link MemoryCardRules#canPaste}). The range is clamped to what the target base can reach.
     */
    public static void apply(MemoryCardData.BaseSettings settings, TurretBaseLogic logic) {
        logic.setMode(settings.mode());
        logic.setMultiTargeting(settings.multiTargeting());
        logic.setTargetPlayers(settings.targetPlayers());
        logic.setTargetMobs(settings.targetMobs());
        logic.setTargetPassive(settings.targetPassive());
        logic.updateMaxRange();
        logic.setRange(Math.min(settings.range(), logic.getTargetingSettings().getMaxRange()));
    }
}
