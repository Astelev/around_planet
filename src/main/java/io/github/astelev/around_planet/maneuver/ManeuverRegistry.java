package io.github.astelev.around_planet.maneuver;
import net.neoforged.neoforge.registries.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ManeuverRegistry {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("around_planet");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("around_planet");
    public static final DeferredBlock<ManeuverPlannerBlock> PLANNER=BLOCKS.registerBlock("maneuver_planner",ManeuverPlannerBlock::new,BlockBehaviour.Properties.of().strength(3).noOcclusion());
    static{ITEMS.registerSimpleBlockItem(PLANNER);}
    public static void register(net.neoforged.bus.api.IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS))e.accept(PLANNER.get());});}
    private ManeuverRegistry(){}
}
