package io.github.astelev.around_planet.manufacturing;

import java.util.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;

public final class ManufacturingRegistry {
  private static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks("around_planet");
  private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("around_planet");
  private static final DeferredRegister<BlockEntityType<?>> TYPES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "around_planet");
  private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
      DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "around_planet");
  private static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>>
      INGREDIENTS =
          DeferredRegister.create(
              net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES,
              "around_planet");
  private static final DeferredRegister<RecipeType<?>> RECIPES =
      DeferredRegister.create(Registries.RECIPE_TYPE, "around_planet");
  private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, "around_planet");
  private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, "around_planet");
  public static final java.util.function.Supplier<DataComponentType<PartData>> PART_DATA =
      COMPONENTS.register(
          "manufactured_part",
          () ->
              DataComponentType.<PartData>builder()
                  .persistent(PartData.CODEC)
                  .networkSynchronized(
                      net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(
                          PartData.CODEC))
                  .build());
  public static final java.util.function.Supplier<
          net.neoforged.neoforge.common.crafting.IngredientType<PartIngredient>>
      PART_INGREDIENT =
          INGREDIENTS.register(
              "qualified_part",
              () ->
                  new net.neoforged.neoforge.common.crafting.IngredientType<>(
                      PartIngredient.CODEC));
  public static final Map<PartKind, java.util.function.Supplier<PartItem>> PARTS =
      new EnumMap<>(PartKind.class);
  public static final Map<MachineKind, DeferredBlock<ManufacturingBlock>> MACHINES =
      new EnumMap<>(MachineKind.class);
  public static final List<java.util.function.Supplier<Item>> DUSTS = new ArrayList<>();

  static {
    for (var p : PartKind.values())
      PARTS.put(
          p, ITEMS.register(p.getSerializedName(), () -> new PartItem(new Item.Properties(), p)));
    for (String material : List.of("copper", "iron", "zinc", "netherite"))
      DUSTS.add(ITEMS.registerSimpleItem(material + "_dust"));
    for (var kind : MachineKind.values()) {
      var block =
          BLOCKS.registerBlock(
              kind.getSerializedName(),
              p -> new ManufacturingBlock(p, kind),
              net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                  .noOcclusion()
                  .strength(3.5f)
                  .requiresCorrectToolForDrops());
      MACHINES.put(kind, block);
      ITEMS.registerSimpleBlockItem(block);
    }
  }

  public static final java.util.function.Supplier<BlockEntityType<ManufacturingBlockEntity>> TYPE =
      TYPES.register(
          "manufacturing",
          () ->
              BlockEntityType.Builder.of(
                      ManufacturingBlockEntity::new,
                      MACHINES.values().stream()
                          .map(java.util.function.Supplier::get)
                          .toArray(net.minecraft.world.level.block.Block[]::new))
                  .build(null));
  public static final java.util.function.Supplier<
          net.minecraft.world.inventory.MenuType<ManufacturingMenu>>
      MENU =
          MENUS.register(
              "manufacturing",
              () ->
                  net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                      ManufacturingMenu::client));
  public static final java.util.function.Supplier<RecipeType<MachiningRecipe>> RECIPE_TYPE =
      RECIPES.register(
          "machining",
          () ->
              new RecipeType<>() {
                public String toString() {
                  return "around_planet:machining";
                }
              });
  public static final java.util.function.Supplier<RecipeSerializer<MachiningRecipe>>
      RECIPE_SERIALIZER =
          SERIALIZERS.register(
              "machining",
              () ->
                  new RecipeSerializer<>() {
                    public com.mojang.serialization.MapCodec<MachiningRecipe> codec() {
                      return MachiningRecipe.CODEC;
                    }

                    public net.minecraft.network.codec.StreamCodec<
                            net.minecraft.network.RegistryFriendlyByteBuf, MachiningRecipe>
                        streamCodec() {
                      return net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(
                          MachiningRecipe.CODEC.codec());
                    }
                  });

  public static ItemStack part(PartKind kind, PartData data, int count) {
    var result = new ItemStack(PARTS.get(kind).get(), count);
    result.set(PART_DATA.get(), data);
    return result;
  }

  public static void register(net.neoforged.bus.api.IEventBus bus) {
    BLOCKS.register(bus);
    ITEMS.register(bus);
    TYPES.register(bus);
    COMPONENTS.register(bus);
    INGREDIENTS.register(bus);
    RECIPES.register(bus);
    SERIALIZERS.register(bus);
    MENUS.register(bus);
    bus.addListener(ManufacturingRegistry::capabilities);
    bus.addListener(io.github.astelev.around_planet.network.ManufacturingFilterPayload::register);
    bus.addListener(
        (net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
          if (e.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
            MACHINES.values().forEach(b -> e.accept(b.get()));
            DUSTS.forEach(i -> e.accept(i.get()));
            for (var p : PartKind.values())
              for (var d : MaterialDefinitions.examples()) e.accept(part(p, d, 1));
          }
        });
  }

  private static void capabilities(
      net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) {
    e.registerBlockEntity(
        net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
        TYPE.get(),
        (be, side) -> be.energy);
    e.registerBlockEntity(
        net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
        TYPE.get(),
        (be, side) ->
            side == null
                ? be.items
                : side == net.minecraft.core.Direction.DOWN ? be.output : be.input);
  }

  private ManufacturingRegistry() {}
}
