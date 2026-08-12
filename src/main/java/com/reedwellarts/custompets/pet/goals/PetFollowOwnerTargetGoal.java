package com.reedwellarts.custompets.pet.goals;

import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PetFollowOwnerTargetGoal<T extends MobEntity & OwnablePet> extends Goal {

    private @Nullable LivingEntity ownerTarget;
    private T pet;

    public PetFollowOwnerTargetGoal(T pet){
        this.pet = pet;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        PlayerEntity owner = pet.getOwnerPlayer();
        if (owner == null) return false;
        ownerTarget = owner.getAttacking();
        return ownerTarget != null;
    }

    @Override
    public void start() {
        pet.setTarget(ownerTarget);
    }

    @Override
    public boolean shouldContinue() {
        return pet.getTarget() != null && pet.getTarget().isAlive();
    }
}
