package com.reedwellarts.custompets.pet.skill.skills;

import com.reedwellarts.custompets.pet.core.enums.SkillCategory;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import com.reedwellarts.custompets.pet.core.interfaces.Skill;
import com.reedwellarts.custompets.pet.data.PetData;
import net.minecraft.entity.mob.MobEntity;

public class FlyingMountSkill implements Skill {

    @Override
    public void apply(PetData data) {
        data.isRideable = true;
        data.isFlyable = true;
    }

    @Override
    public int getUnlockLevel() {
        return 70;
    }

    @Override
    public SkillCategory getSkillCategory() {
        return SkillCategory.PASSIVE;
    }
}
