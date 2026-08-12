package com.reedwellarts.custompets.pet.skill;

import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class PetSkillState {
    public static final int MAX_ACTIVE = 4;

    private final Set<Identifier> unlocked = new HashSet<>();
    private  final Set<Identifier> active = new HashSet<>();

    public boolean isUnlocked(Identifier id){
        return unlocked.contains(id);
    }

    public boolean isActive(Identifier id){
        return active.contains(id);
    }

    public Set<Identifier> getUnlocked() {
        return Set.copyOf(unlocked);
    }

    public Set<Identifier> getActive() {
        return Set.copyOf(active);
    }

    public boolean unlock(Identifier id){
        return unlocked.add(id);
    }

    public boolean activate(Identifier id){
        if (!unlocked.contains(id)) return false;
        if (isActive(id)) return true;
        if (active.size() >= MAX_ACTIVE) return false;
        return active.add(id);
    }

    public boolean deactivate(Identifier id){
        return active.remove(id);
    }

    public void clear(){
        unlocked.clear();
        active.clear();
    }
}
