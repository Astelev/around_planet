package io.github.astelev.around_planet.maneuver;
import net.neoforged.neoforge.registries.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ManeuverRegistry {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
    public static final DeferredBlock<ManeuverPlannerBlock> PLANNER=BLOCKS.registerBlock("maneuver_planner",ManeuverPlannerBlock::new,BlockBehaviour.Properties.of().strength(3).noOcclusion());
    public static final DeferredBlock<TelescopeBlock> TELESCOPE=BLOCKS.registerBlock("telescope",TelescopeBlock::new,BlockBehaviour.Properties.of().strength(3).noOcclusion());
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> TYPES=DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,"around_planet");
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<ManeuverPlannerBlockEntity>> TYPE=TYPES.register("maneuver_planner",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(ManeuverPlannerBlockEntity::new,PLANNER.get()).build(null));
    static{ITEMS.registerSimpleBlockItem(PLANNER);ITEMS.registerSimpleBlockItem(TELESCOPE);}
    public static void register(net.neoforged.bus.api.IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS)){e.accept(PLANNER.get());e.accept(TELESCOPE.get());}});}
    private ManeuverRegistry(){}
}
