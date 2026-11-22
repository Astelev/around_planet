package io.github.astelev.around_planet.loader;
import net.neoforged.neoforge.registries.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ShipLoaderRegistry {
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
 private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,"around_planet");
 public static final DeferredBlock<ShipLoaderBlock> BLOCK=BLOCKS.registerBlock("ship_loader",ShipLoaderBlock::new,BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops().lightLevel(s->s.getValue(ShipLoaderBlock.ENABLED)?5:0));
 public static final java.util.function.Supplier<BlockEntityType<ShipLoaderBlockEntity>> TYPE=TYPES.register("ship_loader",()->BlockEntityType.Builder.of(ShipLoaderBlockEntity::new,BLOCK.get()).build(null));
 static{ITEMS.registerSimpleBlockItem(BLOCK);}
 public static void register(net.neoforged.bus.api.IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS))e.accept(BLOCK.get());});}
 private ShipLoaderRegistry(){}
}
