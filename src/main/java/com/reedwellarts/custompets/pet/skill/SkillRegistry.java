package com.reedwellarts.custompets.pet.skill;

import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.pet.core.interfaces.Skill;
import com.reedwellarts.custompets.pet.skill.skills.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SkillRegistry {

    public static final Map<Identifier, Supplier<Skill>> REGISTRY = new HashMap<>();

    public static final Identifier MOUNT = register("mount", MountSkill::new);
    public static final Identifier DAMAGE_DEALER = register("damage_dealer", DamageDealerSkill::new);
    public static final Identifier FLYING_MOUNT = register("flying_mount", FlyingMountSkill::new);
    public static final Identifier BACKPACK_BUDDY = register("backpack_buddy", BackpackBuddySkill::new);
    public static final Identifier GUARDIAN = register("guardian", GuardianSkill::new);
    public static final Identifier ANGEL = register("angel", AngelSkill::new);

    private static Identifier register(String path, Supplier<Skill> factory){
        Identifier id = Identifier.of(CustomPets.MOD_ID, path);
        REGISTRY.put(id, factory);
        return id;
    }

    public static @Nullable Skill create(Identifier id) {
        Supplier<Skill> factory = REGISTRY.get(id);
        return factory == null ? null : factory.get();
    }
}
