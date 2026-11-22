package omtteam.openmodularturrets.core.turret;

import omtteam.omlib.core.security.PlayerIdentity;
import omtteam.omlib.core.security.PlayerInfo;
import omtteam.openmodularturrets.core.target.EntityCategory;
import omtteam.openmodularturrets.core.target.TargetEntity;

import java.util.Optional;

/** A target that only has a position, for the maths that only reads where it is. */
final class FakePositionTarget extends TargetEntity {
    private final double x;
    private final double y;
    private final double z;

    FakePositionTarget(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int getId() {
        return 1;
    }

    @Override
    public String getTypeId() {
        return "minecraft:zombie";
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public double getEyeX() {
        return x;
    }

    @Override
    public double getEyeY() {
        return y + 1.62;
    }

    @Override
    public double getEyeZ() {
        return z;
    }

    @Override
    public float getHealth() {
        return 20;
    }

    @Override
    public float getMaxHealth() {
        return 20;
    }

    @Override
    public int getArmor() {
        return 0;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public Optional<PlayerInfo> getPlayer() {
        return Optional.empty();
    }

    @Override
    public boolean isCreativeOrSpectator() {
        return false;
    }

    @Override
    public Optional<PlayerIdentity> getOwner() {
        return Optional.empty();
    }

    @Override
    public boolean isTamedHorse() {
        return false;
    }

    @Override
    public boolean hasSlowness() {
        return false;
    }

    @Override
    public EntityCategory getCategory() {
        return EntityCategory.MONSTER;
    }
}
