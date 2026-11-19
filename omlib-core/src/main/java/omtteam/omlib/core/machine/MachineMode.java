package omtteam.omlib.core.machine;

/**
 * Redstone control mode of a machine. Ported from OMLib {@code EnumMachineMode} / {@code TileEntityMachine}.
 */
public enum MachineMode {
    ALWAYS_ON("always_on"),
    ALWAYS_OFF("always_off"),
    /** Active while <b>not</b> powered (default). */
    INVERTED("inverted"),
    /** Active while powered. */
    NONINVERTED("noninverted");

    private final String serializedName;

    MachineMode(String serializedName) {
        this.serializedName = serializedName;
    }

    /** The saved value of the mode; the game layer also builds its language keys from it. */
    public String getSerializedName() {
        return serializedName;
    }

    /**
     * @param redstone whether the machine receives a redstone signal
     * @return whether a machine in this mode runs, given the signal
     */
    public boolean isActive(boolean redstone) {
        return switch (this) {
            case ALWAYS_ON -> true;
            case ALWAYS_OFF -> false;
            case INVERTED -> !redstone;
            case NONINVERTED -> redstone;
        };
    }

    /** The next mode in GUI cycling order. */
    public MachineMode next() {
        MachineMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * The mode with this ordinal, or {@code fallback} if there is none (out of range). 1.12.2 saves and sends the mode
     * as its ordinal; the released OMT 4.0.1 for 1.12.2 calls this method.
     */
    public static MachineMode byOrdinal(int ordinal, MachineMode fallback) {
        MachineMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    /** The mode with this serialized name, or {@code fallback} if there is none (unknown or missing saved value). */
    public static MachineMode bySerializedName(String name, MachineMode fallback) {
        for (MachineMode mode : values()) {
            if (mode.serializedName.equals(name)) return mode;
        }
        return fallback;
    }
}
