package io.github.astelev.around_planet.moon;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
public final class MoonRegistry {
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
 private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,"around_planet");
 private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOMES=DeferredRegister.create(Registries.BIOME_SOURCE,"around_planet");
 public static final DeferredBlock<Block> REGOLITH=BLOCKS.registerSimpleBlock("regolith",net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_GRAY));
 static {ITEMS.registerSimpleBlockItem(REGOLITH);GENERATORS.register("moon",()->MoonChunkGenerator.CODEC);BIOMES.register("moon",()->MoonBiomeSource.CODEC);}
 public static void register(net.neoforged.bus.api.IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);GENERATORS.register(bus);BIOMES.register(bus);bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS))e.accept(REGOLITH.get());});}
 private MoonRegistry(){}
}
