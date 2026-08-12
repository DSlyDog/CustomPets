package com.reedwellarts.custompets.client.screen.skilltree;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Set;

public record PetSkillNode(
    Identifier id,
    ItemStack item,
    Text title,
    Text description,
    int x,
    int y,
    Set<Identifier> parents,
    boolean unlocked,
    int unlockLevel
) { }
