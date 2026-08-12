package com.reedwellarts.custompets.items.item;

import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import com.reedwellarts.custompets.networking.CustomPetsServerNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class PetNameTagItem extends Item {

    public PetNameTagItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getEntityWorld().isClient()) return ActionResult.PASS;

        if (!(entity instanceof OwnablePet pet)) return ActionResult.PASS;

        if (pet.getDelegate().getPetOwnerUuid() == null || !pet.getDelegate().getPetOwnerUuid().equals(user.getUuid())) return ActionResult.PASS;

        CustomPetsServerNetworking.sendOpenScreen((ServerPlayerEntity) user, entity.getUuidAsString());
        return ActionResult.SUCCESS;
    }
}
