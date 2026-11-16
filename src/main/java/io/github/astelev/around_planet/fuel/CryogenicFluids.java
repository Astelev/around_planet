package io.github.astelev.around_planet.fuel;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.fluids.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Supplier;
/** Own NeoForge fluids; neither Petrochem nor Create is needed to register or store them. */
public final class CryogenicFluids {
 private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,"around_planet");
 private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"around_planet");
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
 public static final Supplier<FluidType> HYDROGEN_TYPE=TYPES.register("liquid_hydrogen",()->new FluidType(FluidType.Properties.create().density(71).viscosity(150).temperature(20)){});
 public static final Supplier<FluidType> OXYGEN_TYPE=TYPES.register("liquid_oxygen",()->new FluidType(FluidType.Properties.create().density(1141).viscosity(200).temperature(90)){});
 public static final Supplier<BaseFlowingFluid.Source> HYDROGEN=FLUIDS.register("liquid_hydrogen",()->new BaseFlowingFluid.Source(hydrogen()));
 public static final Supplier<BaseFlowingFluid.Flowing> HYDROGEN_FLOWING=FLUIDS.register("flowing_liquid_hydrogen",()->new BaseFlowingFluid.Flowing(hydrogen()));
 public static final Supplier<BaseFlowingFluid.Source> OXYGEN=FLUIDS.register("liquid_oxygen",()->new BaseFlowingFluid.Source(oxygen()));
 public static final Supplier<BaseFlowingFluid.Flowing> OXYGEN_FLOWING=FLUIDS.register("flowing_liquid_oxygen",()->new BaseFlowingFluid.Flowing(oxygen()));
 public static final Supplier<LiquidBlock> HYDROGEN_BLOCK=BLOCKS.register("liquid_hydrogen",()->new LiquidBlock(HYDROGEN.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
 public static final Supplier<LiquidBlock> OXYGEN_BLOCK=BLOCKS.register("liquid_oxygen",()->new LiquidBlock(OXYGEN.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
 public static final Supplier<BucketItem> HYDROGEN_BUCKET=ITEMS.register("liquid_hydrogen_bucket",()->new BucketItem(HYDROGEN.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
 public static final Supplier<BucketItem> OXYGEN_BUCKET=ITEMS.register("liquid_oxygen_bucket",()->new BucketItem(OXYGEN.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
 private static BaseFlowingFluid.Properties hydrogen(){return new BaseFlowingFluid.Properties(HYDROGEN_TYPE,HYDROGEN,HYDROGEN_FLOWING).bucket(HYDROGEN_BUCKET).block(HYDROGEN_BLOCK);}
 private static BaseFlowingFluid.Properties oxygen(){return new BaseFlowingFluid.Properties(OXYGEN_TYPE,OXYGEN,OXYGEN_FLOWING).bucket(OXYGEN_BUCKET).block(OXYGEN_BLOCK);}
 public static void register(net.neoforged.bus.api.IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);BLOCKS.register(bus);ITEMS.register(bus);bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)){e.accept(HYDROGEN_BUCKET.get());e.accept(OXYGEN_BUCKET.get());}});}
 private CryogenicFluids(){}
}
