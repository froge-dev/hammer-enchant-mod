package com.frogedev.hammer_enchant;

import com.frogedev.hammer_enchant.datagen.Generators;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(HammerEnchantMod.MOD_ID)
public class HammerEnchantMod {
    public static final String MOD_ID = "hammer_enchant";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HammerEnchantMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Prevent JVM from optimizing away these classes...
        new Generators();

        modEventBus.addListener(this::onModCommonSetup);
        ModEnchantments.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.register(Generators.class);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
    }

    private void onModCommonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
    }

    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
    }
}
