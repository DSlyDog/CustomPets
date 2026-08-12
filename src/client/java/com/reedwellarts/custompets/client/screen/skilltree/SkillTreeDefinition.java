package com.reedwellarts.custompets.client.screen.skilltree;

import com.reedwellarts.custompets.pet.skill.SkillRegistry;
import com.reedwellarts.custompets.pet.skill.skills.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Set;

public final class SkillTreeDefinition {

    private static final int NODE_OFFSET = 50;

    public static final List<PetSkillNode> ALL = List.of(
            new PetSkillNode(
                    SkillRegistry.MOUNT,
                    new ItemStack(Items.SADDLE),
                    Text.literal("§bMount"),
                    Text.literal("Make your pet a rideable mount"),
                    0, 0,
                    Set.of(),
                    false,
                    new MountSkill().getUnlockLevel()
            ),
            new PetSkillNode(
                    SkillRegistry.FLYING_MOUNT,
                    new ItemStack(Items.ELYTRA),
                    Text.literal("§bFlying Mount"),
                    Text.literal("Make your pet a flyable mount"),
                    NODE_OFFSET, 0,
                    Set.of(SkillRegistry.MOUNT),
                    false,
                    new FlyingMountSkill().getUnlockLevel()
            ),
            new PetSkillNode(
                    SkillRegistry.DAMAGE_DEALER,
                    new ItemStack(Items.DIAMOND_SWORD),
                    Text.literal("§bDamage Dealer"),
                    Text.literal("Give your pet a damage buff\nthat increases each level"),
                    0, NODE_OFFSET,
                    Set.of(),
                    false,
                    new DamageDealerSkill().getUnlockLevel()
            ),
            new PetSkillNode(
                    SkillRegistry.BACKPACK_BUDDY,
                    new ItemStack(Items.CHEST),
                    Text.literal("§bBackpack Buddy"),
                    Text.literal("Give your pet a backpack to store items"),
                    0, NODE_OFFSET * 2,
                    Set.of(),
                    false,
                    new BackpackBuddySkill().getUnlockLevel()
            ),
            new PetSkillNode(
                    SkillRegistry.GUARDIAN,
                    new ItemStack(Items.SHIELD),
                    Text.literal("§bGuardian"),
                    Text.literal("Your pet protects you by taking\nhalf the damage dealt to you"),
                    NODE_OFFSET, NODE_OFFSET,
                    Set.of(SkillRegistry.DAMAGE_DEALER),
                    false,
                    new GuardianSkill().getUnlockLevel()
            ),
            new PetSkillNode(
                    SkillRegistry.ANGEL,
                    new ItemStack(Items.GOLDEN_APPLE),
                    Text.literal("§bAngel"),
                    Text.literal("Your pet sacrifices itself to\nrestore your health and\nsave you from a killing blow" +
                            "\nThis can trigger once every 5 minutes"),
                    NODE_OFFSET * 2, NODE_OFFSET,
                    Set.of(SkillRegistry.GUARDIAN),
                    false,
                    new AngelSkill().getUnlockLevel()
            )
    );

    private SkillTreeDefinition() {

    }
}
