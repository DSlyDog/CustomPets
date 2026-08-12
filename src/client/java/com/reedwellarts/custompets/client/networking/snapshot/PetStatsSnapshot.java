package com.reedwellarts.custompets.client.networking.snapshot;

import java.util.List;

public record PetStatsSnapshot(
        String petUuid,
        String petType,
        String name,
        int health,
        int maxHealth,
        int level,
        int xp,
        int xpToNextLevel,
        List<String> unlockedSkills,
        List<String> activeSkills,
        boolean active
) { }
