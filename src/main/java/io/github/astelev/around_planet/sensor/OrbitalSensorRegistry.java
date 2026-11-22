package io.github.astelev.around_planet.sensor;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
public final class OrbitalSensorRegistry {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
    public static final DeferredBlock<OrbitalSensorBlock> BLOCK=BLOCKS.registerBlock("orbital_sensor",OrbitalSensorBlock::new,BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2).noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ITEM=ITEMS.registerSimpleBlockItem(BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"around_planet");
    public static final java.util.function.Supplier<BlockEntityType<OrbitalSensorBlockEntity>> TYPE=TYPES.register("orbital_sensor",()->BlockEntityType.Builder.of(OrbitalSensorBlockEntity::new,BLOCK.get()).build(null));
    public static void register(net.neoforged.bus.api.IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);bus.addListener(OrbitalSensorRegistry::creative);}
    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e){if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS))e.accept(ITEM.get());}
    private OrbitalSensorRegistry(){}
}
