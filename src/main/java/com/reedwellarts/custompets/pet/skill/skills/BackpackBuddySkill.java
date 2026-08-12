package com.reedwellarts.custompets.pet.skill.skills;

import com.reedwellarts.custompets.pet.core.enums.SkillCategory;
import com.reedwellarts.custompets.pet.core.interfaces.Skill;
import com.reedwellarts.custompets.pet.data.PetData;

public class BackpackBuddySkill implements Skill {
    @Override
    public void apply(PetData data) {
        data.hasInventory = true;
    }

    @Override
    public int getUnlockLevel() {
        return 10;
    }

    @Override
    public SkillCategory getSkillCategory() {
        return SkillCategory.PASSIVE;
    }
}
