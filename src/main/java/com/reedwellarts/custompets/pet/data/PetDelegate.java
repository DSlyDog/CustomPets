package com.reedwellarts.custompets.pet.data;

import com.mojang.serialization.Codec;
import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.items.item.PetWandItem;
import com.reedwellarts.custompets.pet.core.enums.SkillCategory;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.items.ItemRegistry;
import com.reedwellarts.custompets.pet.core.interfaces.Skill;
import com.reedwellarts.custompets.pet.skill.SkillRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class PetDelegate<T extends MobEntity & OwnablePet & InventoryOwner> {

    private UUID ownerUuid;

    private final PetDataManager dataManager = new PetDataManager();

    private static final int KILL_XP = 20;
    private static final int HIT_XP = 15;
    private static final int RIDE_PASSIVE_XP = 7;
    private static final int INVENTORY_PASSIVE_XP = 7;
    private static final double DISTANCE_FOR_RIDE_XP = 300;
    private static final int ATTACK_DAMAGE_BASE= 2;

    private double distanceRidden = 0;
    private float inventoryUsageCount = 0;
    private int purgeTicker = 0;
    private int sacrificeTicker = 0;
    private boolean hasSacrificed = false;
    private BlockPos lastPos;
    private Item healItem;
    private boolean isGrounded = false;

    private final Set<UUID> recentlyHit = new HashSet<>();

    public PetDelegate(T entity, Item healItem){
        this.healItem = healItem;
        doSkillUnlockPass(entity);
    }

    public void doSkillUnlockPass(T entity){
        for (Map.Entry<Identifier, Supplier<Skill>> entry : SkillRegistry.REGISTRY.entrySet()){
            dataManager.unlockSkill(entry.getKey());
        }

        entity.setFlyable(dataManager.isFlyable());

        var maxHealthAttr = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealthAttr != null){
            maxHealthAttr.setBaseValue(dataManager.getData().maxHealth);
        }
        entity.setHealth(dataManager.getHealth());

        var attackAttr = entity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (attackAttr != null){
            attackAttr.setBaseValue(ATTACK_DAMAGE_BASE * dataManager.getData().attackPowerMultiplier);
        }
    }

    public UUID getPetOwnerUuid() {
        return ownerUuid;
    }

    public LivingEntity getPetOwnerAsLiving(T entity){
        return entity.getEntityWorld().getPlayerByUuid(ownerUuid);
    }

    public void setPetOwnerUuid(@Nullable UUID uuid) {
        this.ownerUuid = uuid;
    }

    public void setPetName(String name){
        dataManager.setName(name);
    }

    public void setPetHealth(int health){
        dataManager.setHealth(health);
    }

    public void setPetMaxHealth(T pet, int maxHealth){
        dataManager.setMaxHealth(maxHealth);
        pet.getAttributeInstance(EntityAttributes.MAX_HEALTH)
                .setBaseValue(maxHealth);
    }

    public PetDataManager getDataManager(){
        return dataManager;
    }

    public Vec3d travel(T entity, Vec3d movementInput) {
        if (entity.hasPassengers() && entity.getControllingPassenger() instanceof PlayerEntity rider){
            entity.setYaw(rider.getYaw());
            entity.setPitch(rider.getPitch() * 0.5f);
            entity.bodyYaw = entity.getBodyYaw();
            entity.headYaw = entity.bodyYaw;

            float strafe = rider.sidewaysSpeed * 0.5f;
            float forward = rider.forwardSpeed;
            if (forward < 0.0F) forward *= 0.25f;

            if (entity.isFlyable()){
                entity.setNoGravity(true);
                float speed = (float) entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) * 2f;

                if (rider.isJumping()) {
                    isGrounded = false;
                    entity.setVelocity(entity.getVelocity().x, speed, entity.getVelocity().z);
                }
                else if (entity.isFlightDescending()) {
                    entity.setVelocity(entity.getVelocity().x, -speed, entity.getVelocity().z);
                }
                else {
                    entity.setVelocity(entity.getVelocity().x, 0.0, entity.getVelocity().z);
                }
            }else{
                entity.setNoGravity(false);
            }

            if (rider.isJumping() && entity.isOnGround()){
                entity.setVelocity(entity.getVelocity().x, 0.6D, entity.getVelocity().z);
            }

            entity.setMovementSpeed((float) entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) * 2F);
            return new Vec3d(strafe, entity.upwardSpeed / 2, forward);
        }

        return movementInput;
    }

    public boolean onFall(T entity){
        if (entity.isFlyable()) return true;
        return false;
    }

    public void dismountPlayer(T entity){
        entity.removeAllPassengers();
        isGrounded = true;
    }

    public boolean canTrust(LivingEntity entity) {
        return this.getPetOwnerUuid().equals(entity.getUuid());
    }

    public void tick(T entity){
        if (entity.isPetSitting()){
            entity.getNavigation().stop();
            entity.setVelocity(0.0, entity.getVelocity().y, 0.0);
        }

        purgeTicker++;
        if (purgeTicker >= 1200){
            purgeTicker = 0;
            recentlyHit.clear();
        }

        if (hasSacrificed) {
            sacrificeTicker++;
            if (sacrificeTicker >= 6000) {
                sacrificeTicker = 0;
                hasSacrificed = false;
            }
        }

        if (entity.hasPassengers()){
            boolean effectiveOnGround = !entity.getEntityWorld().getBlockState(entity.getBlockPos().down()).isAir();
            if (entity.getControllingPassenger() instanceof ServerPlayerEntity rider){
                if (effectiveOnGround && !entity.isJumping()){
                    isGrounded = true;
                }
                if (entity.isFlyable() && !isGrounded){
                    rider.setSneaking(false);
                }

                if (!effectiveOnGround){
                    isGrounded = false;
                }
            }
        }

        tickPassiveRideXp(entity);
    }

    public void tickPassiveRideXp(T entity){
        BlockPos currentPos = entity.getBlockPos();

        if (lastPos != null){
            distanceRidden += Math.sqrt(currentPos.getSquaredDistance(lastPos));
            if (distanceRidden >= DISTANCE_FOR_RIDE_XP){
                dataManager.awardXp(RIDE_PASSIVE_XP, SkillCategory.PASSIVE);
                distanceRidden = 0;
            }

        }

        lastPos = currentPos;

        if (!entity.hasPassengers()) lastPos = null;
    }

    public ActionResult interactMob(T entity, PlayerEntity player, Hand hand) {
        if (entity.getEntityWorld().isClient()){
            return ActionResult.SUCCESS;
        }

        if (player.getUuid().equals(this.getPetOwnerUuid())){
            if (player.getStackInHand(hand).isOf(ItemRegistry.PET_NAME_TAG)){
                return ActionResult.PASS;
            }

            if (player.getStackInHand(hand).isOf(this.healItem)){
                entity.setHealth(entity.getHealth() + entity.getMaxHealth() * 0.25f);
                dataManager.getData().health = (int) entity.getHealth();

                if (entity.getHealth() < entity.getMaxHealth() && entity.getEntityWorld() instanceof ServerWorld serverWorld){
                    serverWorld.spawnParticles(
                            ParticleTypes.HEART,
                            entity.getParticleX(1.0),
                            entity.getRandomBodyY() + 0.5,
                            entity.getParticleZ(1.0),
                            7,
                            0.5,
                            0.5,
                            0.5,
                            0.05
                    );
                }
                return ActionResult.CONSUME;
            }

            if (player.getStackInHand(hand).isOf(ItemRegistry.PET_WAND)){
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                PetWandItem.openSkillTreeForPet(serverPlayer, entity, serverPlayer.getEntityWorld());
                return ActionResult.PASS;
            }

            if (player.getStackInHand(hand).isOf(Items.EXPERIENCE_BOTTLE)){
                dataManager.awardXp(1000000, SkillCategory.PASSIVE);
            }

            if (dataManager.hasInventory() && !player.isSneaking()){
                ScreenHandlerType<?> handlerType = switch (dataManager.getData().inventoryRows) {
                    case 2 -> ScreenHandlerType.GENERIC_9X2;
                    case 3 -> ScreenHandlerType.GENERIC_9X3;
                    case 4 -> ScreenHandlerType.GENERIC_9X4;
                    case 5 -> ScreenHandlerType.GENERIC_9X5;
                    case 6 -> ScreenHandlerType.GENERIC_9X6;
                    default -> ScreenHandlerType.GENERIC_9X1;
                };

                player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, player1) ->
                        new GenericContainerScreenHandler(
                                        handlerType, syncId, playerInventory, entity.getInventory(), 1
                                ),
                        Text.literal(entity.getName().getString() + "'s Inventory")
                        )
                );
                return ActionResult.SUCCESS;
            }

            if (player.getStackInHand(hand).isOf(Items.SADDLE)){
                if (dataManager.isRideable()){
                    entity.setPetSitting(false);
                    player.startRiding(entity, true, true);
                    if (entity.isFlyable()){
                        isGrounded = false;
                    }
                    return ActionResult.CONSUME;
                }
            }

            entity.setPetSitting(!entity.isPetSitting());
            return ActionResult.CONSUME;
        }

        return null;
    }

    public SimpleInventory updateInventory(T entity){
        if (entity.getInventory().size() < dataManager.getInventoryRows() * 9){
            SimpleInventory newInv = new SimpleInventory(dataManager.getInventoryRows() * 9);
            SimpleInventory currentInv = entity.getInventory();
            for (int i = 0; i < entity.getInventory().size(); i++){
                newInv.setStack(i, currentInv.getStack(i));
            }
            return newInv;
        }
        return null;
    }

    public void tryAttack(T entity, boolean result, Entity target){
        recentlyHit.add(target.getUuid());
        if (result && target instanceof LivingEntity living){
            CustomPets.LOGGER.info("Pet hit target");
            if (living.isDead() || living.getHealth() <= 0){
                dataManager.awardXp(KILL_XP, SkillCategory.COMBAT);
            }
        }

        entity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(ATTACK_DAMAGE_BASE * dataManager.getAttackPowerMultiplier());
    }

    public boolean onPlayerKilledTarget(Entity target){
        if (recentlyHit.contains(target.getUuid())){
            dataManager.awardXp(HIT_XP, SkillCategory.COMBAT);
            return true;
        }

        return false;
    }

    public boolean onDamage(T self, DamageSource damageSource){
        if (damageSource.getAttacker() instanceof ServerPlayerEntity player){
            if (player.getUuid().equals(this.getPetOwnerUuid())) return false;
        }

        return true;
    }

    public void doDamage(T entity, ServerWorld world, DamageSource source, float amount){
        entity.damage(world, source, amount);
    }

    public PetTrackingState.Entry onDeath(T entity, DamageSource damageSource) {
        if (!entity.getEntityWorld().isClient()){
            UUID owner = this.getPetOwnerUuid();
            if (owner != null && entity.getEntityWorld() instanceof ServerWorld serverWorld){
                PetTrackingState state = PetTrackingState.get(serverWorld);
                PetTrackingState.Entry entry = state.markDead(entity.getUuid(), entity, serverWorld);

                if (entry == null) {
                    CustomPets.LOGGER.warn("Pet {} died with no tracked entry — was it ever tamed through TameEntityEventHandler?", entity.getUuid());
                    return null;
                }

                PlayerEntity ownerPlayer = serverWorld.getPlayerByUuid(ownerUuid);
                if (ownerPlayer != null){
                    ownerPlayer.sendMessage(damageSource.getDeathMessage(entity), false);
                }
                return entry;
            }
        }
        return null;
    }

    public boolean doSacrifice(){
        if (hasSacrificed){
            return false;
        }

        hasSacrificed = true;
        return true;
    }

    public void writeCustomData(T entity, WriteView view) {
        if (this.ownerUuid != null){
            view.putString("PetOwnerUuid", this.ownerUuid.toString());
        }

        view.putBoolean("PetSitting", entity.isPetSitting());

        var unlockedAppender = view.getListAppender("PetSkillsUnlocked", Codec.STRING);
        for (Identifier id : dataManager.getSkillState().getUnlocked()){
            unlockedAppender.add(id.toString());
        }

        var activeAppender = view.getListAppender("PetSkillsActive", Codec.STRING);
        for (Identifier id : dataManager.getSkillState().getActive()){
            activeAppender.add(id.toString());
        }

        dataManager.getData().writeCustomData(view);
    }

    public void readCustomData(T entity, ReadView view) {
        this.ownerUuid = view.contains("PetOwnerUuid") ? UUID.fromString(view.getString("PetOwnerUuid", "")) : null;
        entity.setPetSitting(view.getBoolean("PetSitting", false));

        dataManager.getData().readCustomData(view);

        dataManager.getSkillState().clear();

        view.getOptionalTypedListView("PetSkillsUnlocked", Codec.STRING)
                .ifPresent(list -> {
                    for (int i=0; i<list.stream().toArray().length; i++){
                        String raw = (String) list.stream().toArray()[i];
                        Identifier id = Identifier.tryParse(raw);
                        if (id != null){
                            dataManager.unlockSkill(id);
                        }
                    }
                });

        view.getOptionalTypedListView("PetSkillsActive", Codec.STRING)
                .ifPresent(list -> {
                    for (int i=0; i<list.stream().toArray().length; i++){
                        String raw = (String) list.stream().toArray()[i];
                        Identifier id = Identifier.tryParse(raw);
                        if (id != null){
                            dataManager.activateSkill(id);
                        }
                    }
                });

        doSkillUnlockPass(entity);
    }
}
