package com.reedwellarts.custompets.items.item;

import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.networking.CustomPetsServerNetworking;
import com.reedwellarts.custompets.networking.payloads.SendPetStatsSnapshotPayload;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import com.reedwellarts.custompets.pet.data.PetData;
import com.reedwellarts.custompets.pet.skill.PetSkillState;
import com.reedwellarts.custompets.util.CustomPetsConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class PetWandItem extends Item{

    private static final double REACH = 4.5;

    public PetWandItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand){
        if (world.isClient()) return ActionResult.SUCCESS;

        if (getTargetedEntity(player, REACH) != null) return ActionResult.PASS;

        if (player instanceof ServerPlayerEntity serverPlayer && world instanceof ServerWorld serverWorld){
            openRosterScreen(serverPlayer, serverWorld);
        }
        return ActionResult.SUCCESS;
    }

    private static Entity getTargetedEntity(PlayerEntity player, double reach){
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(reach));
        Box seachBox = player.getBoundingBox().stretch(look.multiply(reach)).expand(1.0D);

        EntityHitResult hit = ProjectileUtil.raycast(
                player, start, end, seachBox,
                candidate -> !candidate.isSpectator() && candidate.canHit(),
                reach * reach
        );

        return hit == null ? null : hit.getEntity();
    }

    public static void openRosterScreen(ServerPlayerEntity owner, ServerWorld world){
        List<SendPetStatsSnapshotPayload.PetSnapshot> snapshots = buildAllSnapshots(owner, world);
        CustomPetsServerNetworking.sendPetStatsSnapshot(owner, snapshots, CustomPetsConfig.MAX_ACTIVE, true);
    }

    public static void openSkillTreeForPet(ServerPlayerEntity owner, OwnablePet pet, ServerWorld world) {
        PetTrackingState trackingState = PetTrackingState.get(world);
        List<SendPetStatsSnapshotPayload.PetSnapshot> snapshots = List.of(buildSnapshot(owner, pet, trackingState));
        CustomPetsServerNetworking.sendPetStatsSnapshot(owner, snapshots, CustomPetsConfig.MAX_ACTIVE, false);
    }

    private static List<SendPetStatsSnapshotPayload.PetSnapshot> buildAllSnapshots(ServerPlayerEntity owner, ServerWorld world){
        PetTrackingState trackingState = PetTrackingState.get(world);
        List<SendPetStatsSnapshotPayload.PetSnapshot> snapshots = new ArrayList<>();

        for (PetTrackingState.Entry entry : trackingState.getPlayerPets(owner.getUuid())){
            snapshots.add(buildSnapshotFromEntry(owner, entry, trackingState, world));
        }

        return snapshots;
    }

    private static SendPetStatsSnapshotPayload.PetSnapshot buildSnapshot(
            ServerPlayerEntity owner, OwnablePet pet, PetTrackingState trackingState
    ){
        MobEntity petEntity = (MobEntity) pet;
        PetData petData = pet.getDelegate().getDataManager().getData();
        PetSkillState skillState = pet.getDelegate().getDataManager().getSkillState();

        List<String> unlockedSkills = new ArrayList<>();
        for (Identifier id : skillState.getUnlocked()) unlockedSkills.add(id.toShortString());

        List<String> activeSkills = new ArrayList<>();
        for (Identifier id : skillState.getActive()) activeSkills.add(id.toString());

        return new SendPetStatsSnapshotPayload.PetSnapshot(
                petEntity.getUuid().toString(),
                Registries.ENTITY_TYPE.getId(pet.getBaseType()).toString(),
                petData.name,
                petData.health,
                petData.maxHealth,
                petData.level,
                petData.xp,
                petData.xpToNextLevel,
                unlockedSkills,
                activeSkills,
                trackingState.isActivePet(owner.getUuid(), petEntity.getUuid())
        );
    }

    private static SendPetStatsSnapshotPayload.PetSnapshot buildSnapshotFromEntry(
            ServerPlayerEntity owner, PetTrackingState.Entry entry, PetTrackingState trackingState, ServerWorld world
    ){
        Entity liveEntity = world.getEntity(entry.petUuid);
        if (liveEntity instanceof OwnablePet livePet){
            return buildSnapshot(owner, livePet, trackingState);
        }

        PetTrackingState.Entry.CachedStats stats = entry.cachedStats;
        return new SendPetStatsSnapshotPayload.PetSnapshot(
                entry.petUuid.toString(),
                entry.petTypeId,
                stats.name,
                stats.health,
                stats.maxHealth,
                stats.level,
                stats.xp,
                stats.xpToNextLevel,
                stats.unlockedSkills,
                stats.activeSkills,
                entry.status == PetTrackingState.PetStatus.ACTIVE
        );
    }
}
