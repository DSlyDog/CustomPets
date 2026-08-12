package com.reedwellarts.custompets.pet.skill.skills;

import com.reedwellarts.custompets.pet.core.enums.SkillCategory;
import com.reedwellarts.custompets.pet.core.interfaces.Skill;
import com.reedwellarts.custompets.pet.data.PetData;

public class DamageDealerSkill implements Skill {

    @Override
    public void apply(PetData data) {
        data.attackPowerMultiplier *= 1.2 + (0.05 * data.level);
    }

    @Override
    public int getUnlockLevel() {
        return 1;
    }

    @Override
    public SkillCategory getSkillCategory() {
        return SkillCategory.COMBAT;
    }
}
