package com.reedwellarts.custompets.pet.data;

import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

public class PetData {

    public static final int MAX_LEVEL = 100;

    public String name = "";
    public int xp = 0;
    public int xpToNextLevel = 30;
    public int level = 1;
    public int health = 20;
    public int maxHealth = 20;
    public int inventoryRows = 1;

    public double maxHealthMultiplier = 1;
    public double attackPowerMultiplier = 1;
    public boolean isRideable = false;
    public boolean isFlyable = false;
    public boolean hasInventory = false;
    public boolean isGuardian = false;
    public boolean isAngel = false;

    public void writeCustomData(WriteView view){
        view.putString("PetName", name);
        view.putInt("PetXp", xp);
        view.putInt("PetXpToNextLevel", xpToNextLevel);
        view.putInt("PetLevel", level);
        view.putInt("PetHealth", health);
        view.putInt("PetMaxHealth", maxHealth);
        view.putInt("PetInventoryRows", inventoryRows);
    }

    public void readCustomData(ReadView view){
        this.name = view.getString("PetName", "");
        this.xp = view.getInt("PetXp", 0);
        this.xpToNextLevel = view.getInt("PetXpToNextLevel", 0);
        this.level = view.getInt("PetLevel", 1);
        this.health = view.getInt("PetHealth", 20);
        this.maxHealth = view.getInt("PetMaxHealth", 20);
        this.inventoryRows = view.getInt("PetInventoryRows", 1);
    }
}
