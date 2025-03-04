package com.frogedev.hammer_enchant;

import com.frogedev.hammer_enchant.enchantment.MiningShapeEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, HammerEnchantMod.MOD_ID);

    public static final RegistryObject<Enchantment> WIDE_SHAPE_ENCHANTMENT = ENCHANTMENTS.register("wide_shape", MiningShapeEnchantment.build(3));
    public static final RegistryObject<Enchantment> DEEP_SHAPE_ENCHANTMENT = ENCHANTMENTS.register("deep_shape", MiningShapeEnchantment.build(2));

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }
}
