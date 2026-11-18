package io.github.astelev.around_planet.solid;

/** Uses Pondus's ordinary item inventory accounting, including its global/container multipliers.
 * Missing defaults only; no replacement of its mass calculator, tracker or cache. */
public final class SolidMassBridge {
    private static java.lang.reflect.Method itemEntry,blockEntry,itemMass,multiplier,instance;
    private static java.lang.reflect.Field itemModifier;
    public static void registerDefaults(){if(!io.github.astelev.around_planet.fuel.FuelMassBridge.pondus())return;try{
        String root="org.fractalinteractive.ci.pondus_inventory_fi.";var registry=Class.forName(root+"mass.MassRegistry");var item=Class.forName(root+"config.ItemMassEntry");var block=Class.forName(root+"config.BlockMassEntry");var global=Class.forName(root+"config.GlobalConfig");
        itemEntry=registry.getMethod("getItemMass",net.minecraft.world.item.Item.class);blockEntry=registry.getMethod("getBlockMass",net.minecraft.world.level.block.Block.class,net.minecraft.world.level.block.state.BlockState.class);
        itemMass=item.getMethod("getMass",int.class,double.class);multiplier=block.getMethod("getInventoryMultiplier");instance=global.getMethod("getInstance");itemModifier=global.getField("itemMassModifier");
        var register=registry.getMethod("registerItem",String.class,item);var ctor=item.getConstructor(String.class,String.class,boolean.class);
        for(var entry:new Object[][]{{SolidRegistry.PORTION.get(),"1"},{SolidRegistry.FUEL.get().asItem(),"64"}}){var i=(net.minecraft.world.item.Item)entry[0];String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).toString();if(itemEntry.invoke(null,i)==null)register.invoke(null,id,ctor.newInstance(id,entry[1],true));}
        var rb=registry.getMethod("registerBlock",String.class,block);var bc=block.getConstructor(String.class,String.class,double.class,boolean.class);
        for(var entry:new Object[][]{{SolidRegistry.COLUMN.get(),"8"},{SolidRegistry.FUEL.get(),"64"},{io.github.astelev.around_planet.propulsion.PropulsionRegistry.SOLID.get(),"30"}}){var b=(net.minecraft.world.level.block.Block)entry[0];if(blockEntry.invoke(null,b,b.defaultBlockState())==null){String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();rb.invoke(null,id,bc.newInstance(id,entry[1],1.0,true));}}
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot register solid fuel Pondus defaults",e);}}
    public static double kgPerPortion(SolidColumnBlockEntity be){if(!io.github.astelev.around_planet.fuel.FuelMassBridge.pondus())return 1;try{if(itemEntry==null)registerDefaults();var item=itemEntry.invoke(null,SolidRegistry.PORTION.get());var block=blockEntry.invoke(null,be.getBlockState().getBlock(),be.getBlockState());double kg=(double)itemMass.invoke(item,1,1.0)*itemModifier.getDouble(instance.invoke(null))*(double)multiplier.invoke(block);return Double.isFinite(kg)&&kg>0?kg:0;}catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot read solid propellant mass",e);}}
    private SolidMassBridge(){}
}
