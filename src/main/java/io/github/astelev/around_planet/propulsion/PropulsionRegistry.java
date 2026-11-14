package io.github.astelev.around_planet.propulsion;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class PropulsionRegistry {
    public enum Kind {CREATIVE,RSU,DAMPER}
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
    private static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"around_planet");
    public static final DeferredBlock<PropulsionBlock> ENGINE=block("creative_thruster",Kind.CREATIVE),RSU=block("rsu",Kind.RSU),DAMPER=block("inertia_damper",Kind.DAMPER);
    public static final java.util.function.Supplier<BlockEntityType<PropulsionBlockEntity>> TYPE=TYPES.register("propulsion",()->BlockEntityType.Builder.of(PropulsionBlockEntity::new,ENGINE.get(),RSU.get(),DAMPER.get()).build(null));
    private static DeferredBlock<PropulsionBlock> block(String id,Kind kind){var block=BLOCKS.registerBlock(id,p->new PropulsionBlock(p,kind),BlockBehaviour.Properties.of().strength(3).noOcclusion());ITEMS.registerSimpleBlockItem(block);return block;}
    public static void register(net.neoforged.bus.api.IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);bus.addListener(PropulsionRegistry::capabilities);bus.addListener(PropulsionRegistry::creative);}
    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,TYPE.get(),(be,side)->be.kind()==Kind.DAMPER?be.energy:null);}
    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e){if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS)){e.accept(ENGINE.get());e.accept(RSU.get());e.accept(DAMPER.get());}}
    private PropulsionRegistry(){}
}
