package omtteam.openmodularturrets.core.settings;

/**
 * Configurable stats of a turret type (3.x {@code TurretSetting}).
 *
 * @param baseFireRate          ticks between shots
 * @param baseAccuracyDeviation inaccuracy in percent
 * @param maxSimultaneous       maximum number of turrets of this type per base; only checked when placing, unless
 *                              {@link LogicSettings#dropTurretsOverMaxSimultaneous} drops attached turrets over it
 * @param rangeUpgrade          range added per range upgrade
 * @param optimalRange          share of the turret's range (with upgrades) up to which it deals full damage, 0–1;
 *                              see {@link omtteam.openmodularturrets.core.turret.ProjectileMath#falloff}
 * @param falloffDamage         share of the damage dealt at the end of the range and beyond, 0–1; 1 with an
 *                              {@code optimalRange} of 1 turns the falloff off
 * @param turnRate              degrees the turret turns per tick (yaw and pitch each); 0 turns instantly
 */
public record TurretStats(boolean enabled, int baseRange, int baseFireRate, int baseDamage, int powerUsage,
                          double baseAccuracyDeviation, int maxSimultaneous, double damageAmp,
                          double fireRateUpgrade, int rangeUpgrade, double accuracyUpgrade, double efficiencyUpgrade,
                          double recyclerNegateChance, double recyclerAddChance, double optimalRange,
                          double falloffDamage, double turnRate) {

    /** Optimal range share without falloff. */
    public static final double NO_FALLOFF_OPTIMAL_RANGE = 1.0D;
    /** Damage share at the end of the range without falloff. */
    public static final double NO_FALLOFF_DAMAGE = 1.0D;
    /** Turn rate of a turret that turns instantly. */
    public static final double INSTANT_TURN = 0.0D;

    /**
     * Stats without range falloff and with instant turning (the stats before core 2.2.0).
     */
    public TurretStats(boolean enabled, int baseRange, int baseFireRate, int baseDamage, int powerUsage,
                       double baseAccuracyDeviation, int maxSimultaneous, double damageAmp,
                       double fireRateUpgrade, int rangeUpgrade, double accuracyUpgrade, double efficiencyUpgrade,
                       double recyclerNegateChance, double recyclerAddChance) {
        this(enabled, baseRange, baseFireRate, baseDamage, powerUsage, baseAccuracyDeviation, maxSimultaneous,
                damageAmp, fireRateUpgrade, rangeUpgrade, accuracyUpgrade, efficiencyUpgrade, recyclerNegateChance,
                recyclerAddChance, NO_FALLOFF_OPTIMAL_RANGE, NO_FALLOFF_DAMAGE, INSTANT_TURN);
    }

    /**
     * Stats with the given range, fire rate, damage and energy per shot; the other values start at those most
     * built-in turrets use and can be changed on the builder.
     */
    public static Builder builder(int baseRange, int baseFireRate, int baseDamage, int powerUsage) {
        return new Builder(baseRange, baseFireRate, baseDamage, powerUsage);
    }

    /** Builds {@link TurretStats}, e.g. the default stats of a turret added through the turret API. */
    public static final class Builder {
        private final int baseRange;
        private final int baseFireRate;
        private final int baseDamage;
        private final int powerUsage;
        private boolean enabled = true;
        private double baseAccuracyDeviation = 10;
        private int maxSimultaneous = 4;
        private double damageAmp = 0.05D;
        private double fireRateUpgrade = 0.1D;
        private int rangeUpgrade = 2;
        private double accuracyUpgrade = 0.2D;
        private double efficiencyUpgrade = 0.08D;
        private double recyclerNegateChance = 0.1D;
        private double recyclerAddChance = 0.05D;
        private double optimalRange = NO_FALLOFF_OPTIMAL_RANGE;
        private double falloffDamage = NO_FALLOFF_DAMAGE;
        private double turnRate = INSTANT_TURN;

        private Builder(int baseRange, int baseFireRate, int baseDamage, int powerUsage) {
            this.baseRange = baseRange;
            this.baseFireRate = baseFireRate;
            this.baseDamage = baseDamage;
            this.powerUsage = powerUsage;
        }

        /** Default true. */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /** Inaccuracy in percent, default 10. */
        public Builder accuracyDeviation(double percent) {
            this.baseAccuracyDeviation = percent;
            return this;
        }

        /** Default 4. */
        public Builder maxSimultaneous(int max) {
            this.maxSimultaneous = max;
            return this;
        }

        /** Default 0.05. */
        public Builder damageAmp(double damageAmp) {
            this.damageAmp = damageAmp;
            return this;
        }

        /** Default 0.1. */
        public Builder fireRateUpgrade(double bonus) {
            this.fireRateUpgrade = bonus;
            return this;
        }

        /** Default 2. */
        public Builder rangeUpgrade(int blocks) {
            this.rangeUpgrade = blocks;
            return this;
        }

        /** Default 0.2. */
        public Builder accuracyUpgrade(double bonus) {
            this.accuracyUpgrade = bonus;
            return this;
        }

        /** Default 0.08. */
        public Builder efficiencyUpgrade(double saving) {
            this.efficiencyUpgrade = saving;
            return this;
        }

        /** Defaults 0.1 and 0.05. */
        public Builder recycler(double negateChance, double addChance) {
            this.recyclerNegateChance = negateChance;
            this.recyclerAddChance = addChance;
            return this;
        }

        /**
         * Range falloff: full damage up to {@code optimalShare} of the range, then linearly less down to
         * {@code damageAtMaxRange} at the end of the range. Default 1 and 1 (no falloff).
         */
        public Builder falloff(double optimalShare, double damageAtMaxRange) {
            this.optimalRange = optimalShare;
            this.falloffDamage = damageAtMaxRange;
            return this;
        }

        /** Degrees the turret turns per tick, default 0 (turns instantly). */
        public Builder turnRate(double degreesPerTick) {
            this.turnRate = degreesPerTick;
            return this;
        }

        /** Creates the stats; values that were not set keep their defaults. */
        public TurretStats build() {
            return new TurretStats(enabled, baseRange, baseFireRate, baseDamage, powerUsage, baseAccuracyDeviation,
                    maxSimultaneous, damageAmp, fireRateUpgrade, rangeUpgrade, accuracyUpgrade, efficiencyUpgrade,
                    recyclerNegateChance, recyclerAddChance, optimalRange, falloffDamage, turnRate);
        }
    }

    // equals and hashCode written out: the downgraded Java 8 classes must compare doubles like a record does
    // (Double.compare), which JvmDowngrader's generated record methods do not; hashCode computes
    // what a record's does (0-seeded 31 * h + the component's hash, nothing boxed)
    @Override
    public boolean equals(Object o) {
        return o instanceof TurretStats other
                && enabled == other.enabled
                && baseRange == other.baseRange
                && baseFireRate == other.baseFireRate
                && baseDamage == other.baseDamage
                && powerUsage == other.powerUsage
                && Double.compare(baseAccuracyDeviation, other.baseAccuracyDeviation) == 0
                && maxSimultaneous == other.maxSimultaneous
                && Double.compare(damageAmp, other.damageAmp) == 0
                && Double.compare(fireRateUpgrade, other.fireRateUpgrade) == 0
                && rangeUpgrade == other.rangeUpgrade
                && Double.compare(accuracyUpgrade, other.accuracyUpgrade) == 0
                && Double.compare(efficiencyUpgrade, other.efficiencyUpgrade) == 0
                && Double.compare(recyclerNegateChance, other.recyclerNegateChance) == 0
                && Double.compare(recyclerAddChance, other.recyclerAddChance) == 0
                && Double.compare(optimalRange, other.optimalRange) == 0
                && Double.compare(falloffDamage, other.falloffDamage) == 0
                && Double.compare(turnRate, other.turnRate) == 0;
    }

    @Override
    public int hashCode() {
        int h = Boolean.hashCode(enabled);
        h = 31 * h + baseRange;
        h = 31 * h + baseFireRate;
        h = 31 * h + baseDamage;
        h = 31 * h + powerUsage;
        h = 31 * h + Double.hashCode(baseAccuracyDeviation);
        h = 31 * h + maxSimultaneous;
        h = 31 * h + Double.hashCode(damageAmp);
        h = 31 * h + Double.hashCode(fireRateUpgrade);
        h = 31 * h + rangeUpgrade;
        h = 31 * h + Double.hashCode(accuracyUpgrade);
        h = 31 * h + Double.hashCode(efficiencyUpgrade);
        h = 31 * h + Double.hashCode(recyclerNegateChance);
        h = 31 * h + Double.hashCode(recyclerAddChance);
        h = 31 * h + Double.hashCode(optimalRange);
        h = 31 * h + Double.hashCode(falloffDamage);
        return 31 * h + Double.hashCode(turnRate);
    }
}
