package com.reedwellarts.custompets.pet.data;

import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.pet.core.enums.SkillCategory;
import com.reedwellarts.custompets.pet.skill.PetSkillState;
import com.reedwellarts.custompets.pet.core.interfaces.Skill;
import com.reedwellarts.custompets.pet.skill.SkillRegistry;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class PetDataManager {

    private PetData data = new PetData();

    private final PetSkillState skillState = new PetSkillState();
    private final Set<Integer> invLevels = Set.of(18, 26, 34, 42, 50);

    public boolean unlockSkill(Identifier id){
        Skill skill = SkillRegistry.create(id);
        CustomPets.LOGGER.info("level: {}", data.level);
        if (data.level >= skill.getUnlockLevel()) {
            boolean changed = skillState.unlock(id);
            CustomPets.LOGGER.info("Changed: {}", changed);
            if (changed) update();
            return changed;
        }
        return false;
    }

    public boolean activateSkill(Identifier id){
        boolean changed = skillState.activate(id);
        if (changed) update();
        return changed;
    }

    public boolean deactivateSkill(Identifier id){
        boolean changed = skillState.deactivate(id);
        if (changed) update();
        return changed;
    }

    public PetSkillState getSkillState(){
        return skillState;
    }

    public void addXp(int amount){
        data.xp += amount;

        if (data.xp >= data.xpToNextLevel){
            data.xp -= data.xpToNextLevel;
            levelUp();
        }
    }

    public void setName(String name){
        data.name = name;
    }

    public void setHealth(int health){
        data.health = health;
    }

    public int getHealth(){
        return data.health;
    }

    public void setMaxHealth(int maxHealth){
        data.maxHealth = maxHealth;
    }

    public double getMaxHealthMultiplier(){
        return data.maxHealthMultiplier;
    }

    public double getAttackPowerMultiplier(){
        return data.attackPowerMultiplier;
    }

    public boolean isRideable(){
        return data.isRideable;
    }

    public boolean hasInventory(){
        return data.hasInventory;
    }

    public boolean isFlyable() {
        return data.isFlyable;
    }

    private void update(){
        String name = data.name;
        int health = data.health;
        int maxHealth = data.maxHealth;
        int xp = data.xp;
        int xpToNextLevel = data.xpToNextLevel;
        int level = data.level;
        int inventoryRows = data.inventoryRows;

        data = new PetData();
        data.name = name;
        data.health = health;
        data.maxHealth = maxHealth;
        data.xp = xp;
        data.xpToNextLevel = xpToNextLevel;
        data.level = level;
        data.inventoryRows = inventoryRows;

        for (Identifier id : skillState.getActive()){
            Skill skill = SkillRegistry.create(id);
            if (skill != null) skill.apply(data);
            CustomPets.LOGGER.info("Skill {} not null? {}", id.toString(), skill != null);
        }
    }

    public void awardXp(int amount, SkillCategory source){
        if (data.level == PetData.MAX_LEVEL) return;
        data.xp += applyWeight((int) (amount * 1.3 * data.level), source);
        levelUp();
    }

    private int applyWeight(int amount, SkillCategory source){
        int combatCount = 0;
        int passiveCount = 0;
        for (Identifier id : skillState.getActive()){
            Skill skill = SkillRegistry.create(id);
            if (skill.getSkillCategory() == SkillCategory.COMBAT) combatCount++;
            if (skill.getSkillCategory() == SkillCategory.PASSIVE) passiveCount++;
        }
        int total = combatCount + passiveCount;
        float combatWeight = 1.0f + (total > 0 ? (float) combatCount / total : 0.5f);
        float passiveWeight = 1.0f + (total > 0 ? (float) passiveCount / total : 0.5f);

        if (total > 0 && source == SkillCategory.COMBAT) return (int) (amount * combatWeight);
        if (total > 0 && source == SkillCategory.PASSIVE) return (int) (amount * passiveWeight);

        return amount;
    }

    public int getInventoryRows(){
        return data.inventoryRows;
    }

    private void levelUp(){
        while (data.xp >= data.xpToNextLevel && data.level < PetData.MAX_LEVEL){
            data.level++;
            data.xp -= data.xpToNextLevel;
            data.xpToNextLevel = Math.min(25 + (data.level * data.level * 5), 30000);
            if (invLevels.contains(data.level) && data.inventoryRows < 6){
                data.inventoryRows += 1;
            }
        }

        if (data.level == PetData.MAX_LEVEL){
            data.xp = data.xpToNextLevel;
        }

        for (Map.Entry<Identifier, Supplier<Skill>> entry : SkillRegistry.REGISTRY.entrySet()){
            this.unlockSkill(entry.getKey());
        }

        update();
    }

    public PetData getData(){
        return data;
    }
}
