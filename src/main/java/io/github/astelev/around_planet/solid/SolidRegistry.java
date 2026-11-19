package io.github.astelev.around_planet.solid;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class SolidRegistry {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"around_planet");
    public static final DeferredBlock<SolidColumnBlock> COLUMN=BLOCKS.registerBlock("solid_fuel_column",SolidColumnBlock::new,BlockBehaviour.Properties.of().strength(3).noOcclusion());
    public static final DeferredBlock<Block> FUEL=BLOCKS.registerSimpleBlock("solid_rocket_fuel",BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.PACKED_MUD));
    // Internal mass-accounting portions are not craftable, extractable, or listed in creative tabs.
    public static final DeferredItem<net.minecraft.world.item.Item> PORTION=ITEMS.registerSimpleItem("solid_propellant_portion");
    public static final java.util.function.Supplier<BlockEntityType<SolidColumnBlockEntity>> TYPE=TYPES.register("solid_fuel_column",()->BlockEntityType.Builder.of(SolidColumnBlockEntity::new,COLUMN.get()).build(null));
    static {ITEMS.registerSimpleBlockItem(COLUMN);ITEMS.registerSimpleBlockItem(FUEL);}
    public static void register(net.neoforged.bus.api.IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);bus.addListener(SolidRegistry::capabilities);bus.addListener(SolidRegistry::creative);}
    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,TYPE.get(),(be,side)->be.inventory);}
    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e){if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS)){e.accept(COLUMN.get());e.accept(FUEL.get());}}
    private SolidRegistry(){}
}
