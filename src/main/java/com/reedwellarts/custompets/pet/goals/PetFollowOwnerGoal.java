package com.reedwellarts.custompets.pet.goals;

import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public class PetFollowOwnerGoal<T extends MobEntity & OwnablePet> extends Goal {
    private final T pet;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;
    private PlayerEntity owner;

    public PetFollowOwnerGoal(T pet, double speed, float startDistance, float stopDistance){
        this.pet = pet;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (pet.isPetSitting()) return false;

        owner = pet.getOwnerPlayer();

        if (owner == null || owner.isSpectator()) return false;

        return pet.squaredDistanceTo(owner) > (startDistance * startDistance);
    }

    @Override
    public boolean shouldContinue() {
        if (pet.isPetSitting()) return false;

        return owner != null && owner.isAlive()
                && pet.squaredDistanceTo(owner) > (stopDistance * stopDistance);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (owner == null) return;

        pet.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, owner.getEyePos());

        if (pet.age % 10 == 0){
            pet.getNavigation().startMovingTo(owner, speed);
        }
    }

    @Override
    public void stop() {
        owner = null;
        pet.getNavigation().stop();
    }
}
