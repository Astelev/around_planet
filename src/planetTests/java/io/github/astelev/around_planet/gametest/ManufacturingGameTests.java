package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.manufacturing.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ManufacturingGameTests {
  static ManufacturingBlockEntity machine(GameTestHelper h, MachineKind kind) {
    var p = h.absolutePos(new BlockPos(1, 2, 1));
    h.getLevel()
        .setBlockAndUpdate(p, ManufacturingRegistry.MACHINES.get(kind).get().defaultBlockState());
    return (ManufacturingBlockEntity) h.getLevel().getBlockEntity(p);
  }

  static void tick(ManufacturingBlockEntity be, int count) {
    for (int i = 0; i < count; i++)
      ManufacturingBlockEntity.tick(be.getLevel(), be.getBlockPos(), be.getBlockState(), be);
  }

  static PartData iron() {
    return new PartData(
        ResourceLocation.parse("around_planet:iron"),
        new MaterialStats(.65, 1200, .7, .35),
        "ground",
        0xc9ced2);
  }

  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void manufacturingRequiresEnergyAndPersistsWork(GameTestHelper h) {
    var be = machine(h, MachineKind.LATHE);
    be.items.setStackInSlot(0, new ItemStack(Items.IRON_BLOCK));
    tick(be, 10);
    h.assertTrue(
        be.progress == 0 && be.status == 4 && be.items.getStackInSlot(0).getCount() == 1,
        "Unpowered lathe consumed resources");
    be.energy.restore(100000);
    tick(be, 50);
    h.assertTrue(
        be.progress == 50 && be.energy.getEnergyStored() == 97000, "Wrong FE cost or progress");
    var copy = new ManufacturingBlockEntity(be.getBlockPos(), be.getBlockState());
    copy.setLevel(h.getLevel());
    copy.loadWithComponents(
        be.saveWithFullMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
    tick(copy, 110);
    var out = copy.items.getStackInSlot(1);
    var data = out.get(ManufacturingRegistry.PART_DATA.get());
    h.assertTrue(
        copy.items.getStackInSlot(0).isEmpty()
            && out.is(ManufacturingRegistry.PARTS.get(PartKind.PRECISION_GEAR).get()),
        "Save/reload reset or duplicated work");
    h.assertTrue(
        data != null
            && data.stats().equals(iron().stats())
            && data.environment().equals("ground")
            && copy.energy.getEnergyStored() == 90400,
        "Material properties or energy lost on save");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void outputBackpressureAndCapabilitiesAreAtomic(GameTestHelper h) {
    var be = machine(h, MachineKind.MILL);
    be.items.setStackInSlot(0, new ItemStack(Items.COPPER_BLOCK, 2));
    be.items.setStackInSlot(1, new ItemStack(Items.DIRT));
    be.energy.restore(100000);
    tick(be, 300);
    h.assertTrue(
        be.status == 3
            && be.energy.getEnergyStored() == 100000
            && be.items.getStackInSlot(0).getCount() == 2,
        "Blocked output consumed input/energy");
    be.items.setStackInSlot(1, ItemStack.EMPTY);
    tick(be, 180);
    h.assertTrue(
        be.items.getStackInSlot(0).getCount() == 1 && be.items.getStackInSlot(1).getCount() == 1,
        "Milling did not consume one block");
    var l = h.getLevel();
    var pos = be.getBlockPos();
    var in = l.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
    var out = l.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
    var power = l.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.NORTH);
    h.assertTrue(
        in != null && out != null && power != null && power.extractEnergy(100, false) == 0,
        "Missing capability or external FE extraction");
    h.assertTrue(
        in.extractItem(0, 1, false).isEmpty()
            && !out.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty(),
        "Automation took unfinished input or inserted output");
    h.assertTrue(
        out.extractItem(0, 1, false).getItem() instanceof PartItem,
        "Automation could not extract finished part");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void printerUsesFourDustAndSupportsEveryPart(GameTestHelper h) {
    var be = machine(h, MachineKind.PRINTER);
    for (var part : PartKind.values()) {
      be.select(part);
      be.items.setStackInSlot(1, ItemStack.EMPTY);
      be.items.setStackInSlot(0, new ItemStack(ManufacturingRegistry.DUSTS.get(0).get(), 3));
      be.energy.restore(100000);
      tick(be, 310);
      h.assertTrue(
          be.status == 2 && be.energy.getEnergyStored() == 100000,
          "Insufficient powder consumed FE");
      be.items.setStackInSlot(0, new ItemStack(ManufacturingRegistry.DUSTS.get(0).get(), 5));
      tick(be, 300);
      var out = be.items.getStackInSlot(1);
      h.assertTrue(
          out.is(ManufacturingRegistry.PARTS.get(part).get())
              && be.items.getStackInSlot(0).getCount() == 1,
          "Printer failed part " + part);
      h.assertTrue(
          out.get(ManufacturingRegistry.PART_DATA.get())
              .material()
              .equals(ResourceLocation.parse("around_planet:copper")),
          "Wrong powder material");
    }
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void qualificationSurvivesCodecAndRejectsRawItems(GameTestHelper h) {
    var ops =
        h.getLevel()
            .registryAccess()
            .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
    var json =
        com.google.gson.JsonParser.parseString(
            "{\"type\":\"around_planet:qualified_part\",\"part\":\"laval_nozzle\",\"minimum\":{\"working_temperature\":900}}");
    var ingredient = Ingredient.CODEC_NONEMPTY.parse(ops, json).getOrThrow();
    var valid = ManufacturingRegistry.part(PartKind.LAVAL_NOZZLE, iron(), 1);
    h.assertTrue(
        ingredient.test(valid) && !ingredient.isSimple(),
        "Stack predicate simplified or partial minimum failed");
    var copy =
        ItemStack.parseOptional(
            h.getLevel().registryAccess(),
            (net.minecraft.nbt.CompoundTag) valid.save(h.getLevel().registryAccess()));
    h.assertTrue(ingredient.test(copy), "Component properties lost in NBT");
    h.assertTrue(
        !ingredient.test(
                new ItemStack(ManufacturingRegistry.PARTS.get(PartKind.LAVAL_NOZZLE).get()))
            && !ingredient.test(ManufacturingRegistry.part(PartKind.IMPELLER, iron(), 1)),
        "Raw or wrong part accepted");
    var encoded = Ingredient.CODEC_NONEMPTY.encodeStart(ops, ingredient).getOrThrow();
    h.assertTrue(
        Ingredient.CODEC_NONEMPTY.parse(ops, encoded).getOrThrow().test(copy),
        "Ingredient codec lost stat thresholds");
    h.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "around_planet", timeoutTicks = 100)
  public static void customMaterialAndOffworldRequirements(GameTestHelper h) {
    var be = machine(h, MachineKind.LATHE);
    be.select(PartKind.LAVAL_NOZZLE);
    be.energy.restore(100000);
    be.items.setStackInSlot(0, new ItemStack(Items.GOLD_BLOCK));
    tick(be, 160);
    var data = be.items.getStackInSlot(1).get(ManufacturingRegistry.PART_DATA.get());
    h.assertTrue(
        data != null
            && data.material().equals(ResourceLocation.parse("around_planet:test_alloy"))
            && data.stats().temperature() == 1700,
        "Datapack material not recognized");
    var requirement =
        new PartIngredient(PartKind.LAVAL_NOZZLE, new MaterialStats(.7, 1500, .7, .7), "orbit");
    h.assertTrue(
        !requirement.test(be.items.getStackInSlot(1)), "Earth part passed orbit-only requirement");
    var orbital =
        ManufacturingRegistry.part(
            PartKind.LAVAL_NOZZLE,
            new PartData(data.material(), data.stats(), "orbit", data.color()),
            1);
    h.assertTrue(requirement.test(orbital), "Custom orbital material rejected");
    var recipe =
        h.getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ManufacturingRegistry.RECIPE_TYPE.get())
            .getFirst()
            .value();
    h.assertTrue(
        recipe.processingTicks("orbit") < recipe.processingTicks("ground")
            && recipe.processingTicks("moon") == recipe.ticks(),
        "Orbital efficiency bonus incorrect");
    h.succeed();
  }
}
