package com.reedwellarts.custompets.event_handlers.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.event_handlers.RespawnEventHandler;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import com.reedwellarts.custompets.pet.data.PetData;
import com.reedwellarts.custompets.pet.entities.PetFoxEntity;
import com.reedwellarts.custompets.pet.skill.PetSkillState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class PetTrackingState extends PersistentState {

    public enum PetStatus {
        ACTIVE, STORED, DEAD;

        public static PetStatus fromString(String s) {
            try {
                return PetStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                return STORED;
            }
        }
    }

    public static class Entry {
        public UUID ownerUuid;
        public UUID petUuid;
        public String petTypeId;
        public long respawnAt;
        public @Nullable String customNameJson;
        public @Nullable String petVariant;
        public NbtCompound petNbt;
        public PetStatus status = PetStatus.STORED;
        public CachedStats cachedStats = new CachedStats();

        public static class CachedStats{
            public String name = "";
            public int health;
            public int maxHealth;
            public int level;
            public int xp;
            public int xpToNextLevel;
            public List<String> unlockedSkills = new ArrayList<>();
            public List<String> activeSkills = new ArrayList<>();

            public static final Codec<CachedStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("name", "").forGetter(s -> s.name),
                    Codec.INT.optionalFieldOf("health", 0).forGetter(s -> s.health),
                    Codec.INT.optionalFieldOf("max_health", 0).forGetter(s -> s.maxHealth),
                    Codec.INT.optionalFieldOf("level", 1).forGetter(s -> s.level),
                    Codec.INT.optionalFieldOf("xp", 0).forGetter(s -> s.xp),
                    Codec.INT.optionalFieldOf("xp_to_next_level", 0).forGetter(s -> s.xpToNextLevel),
                    Codec.STRING.listOf().optionalFieldOf("unlocked_skills", List.of()).forGetter(s -> s.unlockedSkills),
                    Codec.STRING.listOf().optionalFieldOf("active_skills", List.of()).forGetter(s -> s.activeSkills)
            ).apply(instance, (name, health, maxHealth, level, xp, xpToNextLevel, unlocked, active) -> {
                CachedStats s = new CachedStats();
                s.name = name;
                s.health = health;
                s.maxHealth = maxHealth;
                s.level = level;
                s.xp = xp;
                s.xpToNextLevel = xpToNextLevel;
                s.unlockedSkills = new ArrayList<>(unlocked);
                s.activeSkills = new ArrayList<>(active);
                return s;
            }));
        }

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Uuids.CODEC.fieldOf("owner_uuid").forGetter(e -> e.ownerUuid),
                Uuids.CODEC.fieldOf("pet_uuid").forGetter(e -> e.petUuid),
                Codec.STRING.fieldOf("pet_type_id").forGetter(e -> e.petTypeId),
                Codec.LONG.fieldOf("respawn_at").forGetter(e -> e.respawnAt),
                Codec.STRING.optionalFieldOf("custom_name_json", "")
                        .forGetter(e -> e.customNameJson == null ? "" : e.customNameJson),
                Codec.STRING.optionalFieldOf("pet_variant", "")
                        .forGetter(e -> e.petVariant == null ? "" : e.petVariant),
                NbtCompound.CODEC.fieldOf("pet_nbt").forGetter(e -> e.petNbt),
                Codec.STRING.optionalFieldOf("status", PetStatus.STORED.name())
                        .forGetter(e -> e.status.name()),
                CachedStats.CODEC.optionalFieldOf("cached_stats", new CachedStats())
                        .forGetter(e -> e.cachedStats)
        ).apply(instance, (ownerUuid, petUuid, petTypeId, respawnAt, customNameJson, petVariant, petNbt, status, cachedStats) -> {
            Entry e = new Entry();
            e.ownerUuid = ownerUuid;
            e.petUuid = petUuid;
            e.petTypeId = petTypeId;
            e.respawnAt = respawnAt;
            e.customNameJson = customNameJson.isEmpty() ? null : customNameJson;
            e.petVariant = petVariant.isEmpty() ? null : petVariant;
            e.petNbt = petNbt.isEmpty() ? null : petNbt;
            e.status = PetStatus.fromString(status);
            e.cachedStats = cachedStats;
            return e;
        }));
    }

    private static final String SAVE_ID = CustomPets.MOD_ID + "_links";
    private static final Codec<PetTrackingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(state -> state.entries)
    ).apply(instance, list -> {
        PetTrackingState state = new PetTrackingState();
        state.entries.addAll(list);
        return state;
    }));
    private static final PersistentStateType<PetTrackingState> TYPE = new PersistentStateType<>(
            SAVE_ID,
            PetTrackingState::new,
            CODEC,
            null
    );

    private static final int MAX_TAMED_PETS = 20;
    private static final int MAX_ACTIVE_PETS = 4;
    private static final long DEATH_RESPAWN_DELAY_TICKS = 20L * 30L;

    private final List<Entry> entries = new ArrayList<>();
    private final Map<UUID, List<OwnablePet>> playerPetEntities = new HashMap<>();

    public static PetTrackingState get(ServerWorld world){
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public List<Entry> entries(){
        return entries;
    }

    public List<Entry> getPlayerPets(UUID playerUuid){
        return entries.stream()
                .filter(e -> e.ownerUuid.equals(playerUuid))
                .collect(Collectors.toList());
    }

    public List<Entry> getActivePets(UUID playerUuid){
        return entries.stream()
                .filter(e -> e.ownerUuid.equals(playerUuid) && e.status == PetStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public boolean isActivePet(UUID ownerUuid, UUID petUuid){
        Entry entry = entries.stream()
                .filter(e -> e.ownerUuid.equals(ownerUuid) && e.petUuid.equals(petUuid) && e.status == PetStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        return entry != null;
    }

    public @Nullable Entry getPlayerPet(UUID playerUuid, UUID petUuid){
        return entries.stream()
                .filter(e -> e.ownerUuid.equals(playerUuid) && e.petUuid.equals(petUuid))
                .findFirst()
                .orElse(null);
    }

    public @Nullable Entry getEntry(UUID petUuid){
        return entries.stream()
                .filter(e -> e.petUuid.equals(petUuid))
                .findFirst()
                .orElse(null);
    }

    public boolean playerAtMaxPets(UUID playerUuid){
        return getPlayerPets(playerUuid).size() >= MAX_TAMED_PETS;
    }

    public boolean playerAtMaxActive(UUID playerUuid){
        return getActivePets(playerUuid).size() >= MAX_ACTIVE_PETS;
    }

    public void addPetEntity(UUID playerUuid, OwnablePet pet){
        this.playerPetEntities.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(pet);
    }

    public void removePetEntity(UUID playerUuid, UUID petUuid){
        List<OwnablePet> playerPets = playerPetEntities.get(playerUuid);
        if (playerPets == null) return;
        playerPets.removeIf(p -> ((LivingEntity) p).getUuid().equals(petUuid));
    }

    public List<OwnablePet> getPlayerPetEntities(UUID playerUuid){
        return this.playerPetEntities.computeIfAbsent(playerUuid, k -> new ArrayList<>());
    }

    public void registerLoadedPet(OwnablePet pet) {
        LivingEntity living = (LivingEntity) pet;
        UUID ownerUuid = pet.getDelegate().getPetOwnerUuid();
        if (ownerUuid == null) return;

        Entry entry = getEntry(living.getUuid());
        if (entry == null || entry.status != PetStatus.ACTIVE) return;

        List<OwnablePet> tracked = playerPetEntities.computeIfAbsent(ownerUuid, k -> new ArrayList<>());
        boolean alreadyTracked = tracked.stream()
                .anyMatch(p -> ((LivingEntity) p).getUuid().equals(living.getUuid()));
        if (!alreadyTracked){
            tracked.add(pet);
        }
    }

    public boolean tamePet(LivingEntity pet, UUID ownerUuid){
        if (playerAtMaxPets(ownerUuid) || playerAtMaxActive(ownerUuid)) return false;

        Entry entry = new Entry();
        entry.ownerUuid = ownerUuid;
        entry.petUuid = pet.getUuid();
        entry.petTypeId = EntityType.getId(pet.getType()).toString();
        entry.status = PetStatus.ACTIVE;
        syncEntryFromEntity(entry, pet);

        entries.add(entry);
        addPetEntity(ownerUuid, (OwnablePet) pet);
        markDirty();
        return true;
    }

    public @Nullable Entry markDead(UUID petUuid, LivingEntity pet, ServerWorld world){
        Entry entry = getEntry(petUuid);
        if (entry == null) return null;

        entry.status = PetStatus.DEAD;
        entry.respawnAt = world.getTime() + DEATH_RESPAWN_DELAY_TICKS;
        syncEntryFromEntity(entry, pet);
        removePetEntity(entry.ownerUuid, petUuid);
        markDirty();
        return entry;
    }

    public List<Entry> getDueRespawns(long now){
        return entries.stream()
                .filter(e -> e.status == PetStatus.DEAD && e.respawnAt <= now)
                .collect(Collectors.toList());
    }

    public void completeRespawn(Entry entry, LivingEntity newEntity){
        entry.petUuid = newEntity.getUuid();
        entry.status = PetStatus.ACTIVE;
        addPetEntity(entry.ownerUuid, (OwnablePet) newEntity);
        markDirty();
    }

    public void storePet(UUID petUuid, ServerWorld world){
        Entry entry = getEntry(petUuid);
        if (entry == null) return;

        Entity livingEntity = world.getEntity(petUuid);
        if (livingEntity instanceof LivingEntity living){
            syncEntryFromEntity(entry, living);
            living.discard();
        }

        entry.status = PetStatus.STORED;
        removePetEntity(entry.ownerUuid, petUuid);
        markDirty();
    }

    public boolean reactivatePet(UUID petUuid, ServerWorld world){
        Entry entry = getEntry(petUuid);
        if (entry == null || entry.status != PetStatus.STORED) return false;
        if (playerAtMaxActive(entry.ownerUuid)) return false;

        ServerPlayerEntity owner = (ServerPlayerEntity) world.getPlayerByUuid(entry.ownerUuid);
        if (owner == null) return false;

        LivingEntity living = buildEntityFromEntry(entry, world, owner);

        world.spawnEntity(living);
        entry.petUuid = living.getUuid();
        entry.status = PetStatus.ACTIVE;
        addPetEntity(entry.ownerUuid, (OwnablePet) living);
        markDirty();
        return true;
    }

    public void updateActivePets(UUID ownerUuid, List<String> petUuidStrings, ServerWorld world){
        List<UUID> newActive = petUuidStrings.stream().map(UUID::fromString).toList();
        if (newActive.size() > MAX_ACTIVE_PETS) {
            CustomPets.LOGGER.warn("Attempted to set {} active pets for {}, exceeds cap of {}",
                    newActive.size(), ownerUuid, MAX_ACTIVE_PETS);
            return;
        }

        for (Entry e : getPlayerPets(ownerUuid)){
            boolean shouldBeActive = newActive.contains(e.petUuid);
            if (e.status == PetStatus.ACTIVE && !shouldBeActive){
                storePet(e.petUuid, world);
            }else if (e.status == PetStatus.STORED && shouldBeActive){
                reactivatePet(e.petUuid, world);
            }
        }
    }

    public static @Nullable LivingEntity buildEntityFromEntry(Entry entry, ServerWorld world, ServerPlayerEntity owner){
        Entity loadedEntity = EntityType.loadEntityWithPassengers(
                entry.petNbt, world, SpawnReason.EVENT, e -> {
                    e.refreshPositionAndAngles(owner.getX() + 1, owner.getY(), owner.getZ() + 1, owner.getYaw(), 0f);
                    return e;
                }
        );
        if (!(loadedEntity instanceof LivingEntity living)) return null;

        if (living instanceof OwnablePet pet){
            pet.getDelegate().setPetOwnerUuid(owner.getUuid());
            pet.setPetSitting(false);
            living.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(
                    pet.getDelegate().getDataManager().getData().maxHealth
            );
            living.setHealth(living.getMaxHealth());
            pet.getDelegate().setPetHealth((int) living.getMaxHealth());
            living.setVelocity(living.getVelocity().x, 0, living.getVelocity().z);
        }

        if (living instanceof PetFoxEntity foxPet && entry.petVariant != null){
            foxPet.applySavedVariant(entry.petVariant.toUpperCase());
        }

        return living;
    }

    private void syncEntryFromEntity(Entry entry, LivingEntity entity){
        entry.customNameJson = entity.hasCustomName() ? entity.getCustomName().getString() : null;
        if (entity instanceof PetFoxEntity foxPet){
            entry.petVariant = foxPet.getVariant().asString();
        }

        if (entity instanceof OwnablePet pet){
            PetData data = pet.getDelegate().getDataManager().getData();
            PetSkillState skillState = pet.getDelegate().getDataManager().getSkillState();

            entry.cachedStats.name = data.name;
            entry.cachedStats.health = data.health;
            entry.cachedStats.maxHealth = data.maxHealth;
            entry.cachedStats.level = data.level;
            entry.cachedStats.xp = data.xp;
            entry.cachedStats.xpToNextLevel = data.xpToNextLevel;

            List<String> unlocked = new ArrayList<>();
            for (Identifier id : skillState.getUnlocked()) unlocked.add(id.toString());
            entry.cachedStats.unlockedSkills = unlocked;

            List<String> active = new ArrayList<>();
            for (Identifier id : skillState.getActive()) active.add(id.toString());
            entry.cachedStats.activeSkills = active;
        }

        NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY);
        entity.writeData(view);
        NbtCompound nbt = view.getNbt();
        nbt.putString("id", EntityType.getId(entity.getType()).toString());
        entry.petNbt = nbt;
    }
}