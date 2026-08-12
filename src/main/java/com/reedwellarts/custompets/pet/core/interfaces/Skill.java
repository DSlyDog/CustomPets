package com.reedwellarts.custompets.pet.core.interfaces;

import com.reedwellarts.custompets.pet.core.enums.SkillCategory;
import com.reedwellarts.custompets.pet.data.PetData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import org.jetbrains.annotations.NotNull;

public interface Skill {

    void apply(PetData data);

    @NotNull int getUnlockLevel();
    @NotNull SkillCategory getSkillCategory();
}
