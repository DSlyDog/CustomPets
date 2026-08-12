package com.reedwellarts.custompets.items;

import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.items.item.PetNameTagItem;
import com.reedwellarts.custompets.items.item.PetWandItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ItemRegistry {

    public static final Item PET_NAME_TAG = register(
      "pet_name_tag",
      PetNameTagItem::new,
      new Item.Settings().maxCount(1)
    );

    public static final Item PET_WAND = register(
            "pet_wand",
            PetWandItem::new,
            new Item.Settings().maxCount(1)
    );

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings){
        Identifier id = Identifier.of(CustomPets.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item item = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerModItems(){
        CustomPets.LOGGER.info("Registering items for {}", CustomPets.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries ->
                entries.add(PET_NAME_TAG)
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries ->
                entries.add(PET_WAND));
    }
}
