package com.reedwellarts.custompets.pet.entities;

import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.pet.goals.PetDefendOwnerGoal;
import com.reedwellarts.custompets.pet.goals.PetFollowOwnerGoal;
import com.reedwellarts.custompets.pet.data.PetDelegate;
import com.reedwellarts.custompets.pet.goals.PetFollowOwnerTargetGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class PetFoxEntity extends FoxEntity implements OwnablePet, InventoryOwner {

    private static final TrackedData<Boolean> SITTING =
            DataTracker.registerData(PetFoxEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FLYABLE =
            DataTracker.registerData(PetFoxEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DESCENDING =
            DataTracker.registerData(PetFoxEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Predicate<Entity> JUST_ATTACKED_SOMETHING_FILTER;
    private static final Method SET_VARIANT_METHOD;

    static {
        JUST_ATTACKED_SOMETHING_FILTER = (entity) -> {
            if (!(entity instanceof LivingEntity livingEntity)) {
                return false;
            } else {
                return livingEntity.getAttacking() != null && livingEntity.getLastAttackTime() < livingEntity.age + 600;
            }
        };

        Method m = null;
        try {
            m = FoxEntity.class.getDeclaredMethod("setVariant", FoxEntity.Variant.class);
            m.setAccessible(true);
        }catch (NoSuchMethodException e){
            CustomPets.LOGGER.error("Could not find FoxEntity#setVariant(FoxEntity.Variant)", e);
        }
        SET_VARIANT_METHOD = m;
    }

    private final PetDelegate<PetFoxEntity> delegate = new PetDelegate<>(this, Items.SWEET_BERRIES);
    private SimpleInventory inventory = new SimpleInventory(9 * delegate.getDataManager().getData().inventoryRows);

    public PetFoxEntity(EntityType<? extends FoxEntity> entityType, World world) {
        super(entityType, world);
        delegate.setPetName(getName().getString());
    }

    @Override
    public PetDelegate getDelegate(){
        return delegate;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SITTING, false);
        builder.add(FLYABLE, false);
        builder.add(DESCENDING, false);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.clear(goal -> true);

        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new PowderSnowJumpGoal(this, this.getEntityWorld()));
        this.goalSelector.add(6, new JumpChasingGoal());
        this.goalSelector.add(7, new AttackGoal(this));
        this.goalSelector.add(8, new FollowParentGoal(this, (double)1.25F));
        this.goalSelector.add(9, new GoToVillageGoal(this, 200));
        this.goalSelector.add(10, new EatBerriesGoal((double)1.2F, 12, 1));
        this.goalSelector.add(10, new PounceAtTargetGoal(this, 0.4F));
        this.goalSelector.add(11, new WanderAroundFarGoal(this, (double)1.0F));
        this.goalSelector.add(12, new LookAtEntityGoal(this, PlayerEntity.class, 24.0F));

        this.goalSelector.add(1, new PetFollowOwnerGoal<>(this, 1.1D, 8.0F, 3.0F));
        this.goalSelector.add(2, new PetDefendOwnerGoal<>(
                this,
                LivingEntity.class,
                false,
                false,
                (entity, world) ->
                        JUST_ATTACKED_SOMETHING_FILTER.test(entity) && !delegate.canTrust(entity))
        );
        this.goalSelector.add(2, new PetFollowOwnerTargetGoal<>(this));
    }

    @Override
    public EntityType getBaseType() {
        return EntityType.FOX;
    }

    @Override
    public boolean isPetSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setPetSitting(boolean sitting) {
        this.dataTracker.set(SITTING, sitting);
        this.setSitting(sitting);
    }

    @Override
    public boolean isFlyable() {
        return this.dataTracker.get(FLYABLE);
    }

    @Override
    public void setFlyable(boolean flyable) {
        this.dataTracker.set(FLYABLE, flyable);
    }

    @Override
    public boolean isFlightDescending() {
        return this.dataTracker.get(DESCENDING);
    }


    @Override
    public void setFlightDescending(boolean isDescending) {
        this.dataTracker.set(DESCENDING, isDescending);
    }

    @Override
    public FoxEntity asLiving() {
        return this;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger){
        return passenger instanceof PlayerEntity && this.getPassengerList().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger(){
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    @Override
    public void travel(Vec3d movementInput) {
        Vec3d result = delegate.travel(this, movementInput);
        this.setRotation(this.getYaw(), this.getPitch());
        super.travel(result);
    }

    @Override
    public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
        if (delegate.onFall(this)) return false;
        return super.handleFallDamage(fallDistance, damagePerDistance, damageSource);
    }

    @Override
    public void tick(){
        super.tick();
        delegate.tick(this);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        SimpleInventory inv = delegate.updateInventory(this);
        CustomPets.LOGGER.info("inv != null: {}", inv != null);
        inventory = inv != null ? inv : inventory;
        ActionResult result = delegate.interactMob(this, player, hand);
        return result != null ? result : super.interactMob(player, hand);
    }

    @Override
    public boolean damage(ServerWorld serverWorld, DamageSource damageSource, float amount) {
        if (damageSource.getAttacker() instanceof ServerPlayerEntity player){
            if (player.getUuid().equals(delegate.getPetOwnerUuid())) return false;
        }

        boolean damaged = super.damage(serverWorld, damageSource, amount);
        if (damaged) {
            delegate.setPetHealth((int) this.getHealth());
        }
        return damaged;
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean result = super.tryAttack(world, target);
        delegate.tryAttack(this, result, target);
        return result;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        delegate.onDeath(this, damageSource);
        super.onDeath(damageSource);
    }

    @Override
    public void applySavedVariant(@Nullable String variantId){
        if (variantId == null || variantId.isBlank()) return;
        if (SET_VARIANT_METHOD == null) return;

        try{
            FoxEntity.Variant variant = FoxEntity.Variant.valueOf(variantId);
            SET_VARIANT_METHOD.invoke(this, variant);
        }catch(Exception e){
            CustomPets.LOGGER.warn("Failed to apply fox variant '{}' to pet fox {}", variantId, this.getUuid(), e);
        }
    }

    @Override
    public void unsetPetRemoved(){
        super.unsetRemoved();
    }

    @Override
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        delegate.setPetName(name == null ? getName().getString() : name.getString());
    }

    @Override
    public SimpleInventory getInventory() {
        return inventory;
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        delegate.writeCustomData(this, view);
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        delegate.readCustomData(this, view);
    }
}
