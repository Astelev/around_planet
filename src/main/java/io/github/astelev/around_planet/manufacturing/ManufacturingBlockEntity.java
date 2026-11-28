package io.github.astelev.around_planet.manufacturing;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.*;

/**
 * Atomic completion, output backpressure, persisted work, no input/energy use in unloaded chunks.
 */
public final class ManufacturingBlockEntity extends BlockEntity
    implements net.minecraft.world.MenuProvider {
  public static final int CAPACITY = 200000;
  public int progress, total, status;
  private String job = "";
  private MachiningRecipe cachedRecipe;
  private String cachedRecipeKey = "";
  private boolean loading;
  public final ItemStackHandler items =
      new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack s) {
          return slot == 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
          if (!loading) {
            if (slot == 0) {
              progress = 0;
              job = "";
            }
            setChanged();
          }
        }
      };
  public final IItemHandler input =
      new net.neoforged.neoforge.items.wrapper.RangedWrapper(items, 0, 1) {
        @Override
        public ItemStack extractItem(int slot, int count, boolean simulate) {
          return ItemStack.EMPTY;
        }
      };
  public final IItemHandler output =
      new net.neoforged.neoforge.items.wrapper.RangedWrapper(items, 1, 2) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
          return stack;
        }
      };
  public final Power energy = new Power();

  public final class Power extends net.neoforged.neoforge.energy.EnergyStorage {
    private Power() {
      super(CAPACITY, 20000, 0);
    }

    public void restore(int n) {
      energy = Math.clamp(n, 0, CAPACITY);
    }

    private void spend(int n) {
      energy -= n;
      setChanged();
    }

    @Override
    public int receiveEnergy(int max, boolean simulate) {
      int n = super.receiveEnergy(max, simulate);
      if (n > 0 && !simulate) setChanged();
      return n;
    }
  }

  public ManufacturingBlockEntity(BlockPos p, BlockState s) {
    super(ManufacturingRegistry.TYPE.get(), p, s);
  }

  public MachineKind kind() {
    return ((ManufacturingBlock) getBlockState().getBlock()).kind;
  }

  private ItemStack filter = ItemStack.EMPTY;

  public ItemStack filter() {
    return filter.copy();
  }

  public void setFilter(ItemStack stack) {
    var next = stack.copyWithCount(1);
    if (ItemStack.isSameItemSameComponents(filter, next)) return;
    filter = next;
    progress = 0;
    job = "";
    setChanged();
  }

  /**
   * Kept for saved alpha.75 selections and server integrations; the actual selector is the filter.
   */
  public PartKind selected() {
    return filter.getItem() instanceof PartItem p ? p.kind : kind().first();
  }

  public void select(PartKind p) {
    if (kind().supports(p)) setFilter(new ItemStack(ManufacturingRegistry.PARTS.get(p).get()));
  }

  /** Cycle real recipe outputs without consuming an example item, including when JEI is absent. */
  public void cycleFilter() {
    if (level == null) return;
    var options = new java.util.LinkedHashMap<Item, ItemStack>();
    level.getRecipeManager().getAllRecipesFor(ManufacturingRegistry.RECIPE_TYPE.get()).stream()
        .filter(
            r ->
                r.value().machine() == kind()
                    && (items.getStackInSlot(0).isEmpty()
                        || r.value().accepts(items.getStackInSlot(0)))
                    && (r.value().environment().equals("any")
                        || r.value().environment().equals(environment())))
        .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
        .forEach(
            r -> {
              var result = r.value().result(environment());
              options.putIfAbsent(result.getItem(), result);
            });
    var values = new java.util.ArrayList<>(options.values());
    if (values.isEmpty()) return;
    int current = -1;
    for (int i = 0; i < values.size(); i++)
      if (ItemStack.isSameItem(filter, values.get(i))) {
        current = i;
        break;
      }
    setFilter(values.get((current + 1) % values.size()));
  }

  public boolean acceptsResult(ItemStack stack) {
    if (filter.isEmpty()) return true;
    if (net.neoforged.fml.ModList.get().isLoaded("create"))
      return io.github.astelev.around_planet.compat.ManufacturingCreateFilter.test(
          level, filter, stack);
    return ItemStack.isSameItem(filter, stack);
  }

  public String environment() {
    if (level == null) return "ground";
    if (level.dimension().equals(io.github.astelev.around_planet.space.MoonSurface.dimension()))
      return "moon";
    if (net.neoforged.fml.ModList.get().isLoaded("sable")) {
      var world =
          io.github.astelev.around_planet.compat.SpaceSensorNavigation.worldPoint(
              level, worldPosition);
      if (io.github.astelev.around_planet.space.SpaceCells.contains(
          level, world.x, world.y, world.z)) return "orbit";
    } else if (io.github.astelev.around_planet.space.SpaceCells.contains(
        level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) return "orbit";
    return "ground";
  }

  private net.minecraft.world.item.crafting.RecipeHolder<MachiningRecipe> recipe() {
    if (level == null) return null;
    return level
        .getRecipeManager()
        .getAllRecipesFor(ManufacturingRegistry.RECIPE_TYPE.get())
        .stream()
        .filter(
            r ->
                r.value().machine() == kind()
                    && r.value().accepts(items.getStackInSlot(0))
                    && acceptsResult(r.value().result(environment()))
                    && (r.value().environment().equals("any")
                        || r.value().environment().equals(environment())))
        .sorted(java.util.Comparator.comparing(r -> r.id().toString()))
        .findFirst()
        .orElse(null);
  }

  public ItemStack preview() {
    var r = recipe();
    return r == null ? ItemStack.EMPTY : r.value().result(environment());
  }

  private static boolean fits(ItemStack existing, ItemStack result) {
    return existing.isEmpty()
        || ItemStack.isSameItemSameComponents(existing, result)
            && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
  }

  public static void tick(Level l, BlockPos p, BlockState state, ManufacturingBlockEntity be) {
    be.work();
    boolean lit = be.status == 1;
    if (state.getValue(BlockStateProperties.LIT) != lit)
      l.setBlock(p, state.setValue(BlockStateProperties.LIT, lit), 2);
  }

  private void work() {
    var holder = recipe();
    status = 0;
    if (holder == null) {
      if (progress != 0) {
        progress = 0;
        job = "";
        setChanged();
      }
      return;
    }
    var r = holder.value();
    var result = r.result(environment());
    total = r.processingTicks(environment());
    if (cachedRecipe != r) {
      cachedRecipe = r;
      cachedRecipeKey =
          MachiningRecipe.CODEC
              .codec()
              .encodeStart(
                  level
                      .registryAccess()
                      .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),
                  r)
              .getOrThrow()
              .toString();
    }
    String key =
        holder.id()
            + "/"
            + environment()
            + "/"
            + cachedRecipeKey
            + "/"
            + items.getStackInSlot(0).getComponents();
    if (!key.equals(job)) {
      job = key;
      progress = 0;
      setChanged();
    }
    if (items.getStackInSlot(0).getCount() < r.inputCount()) {
      status = 2;
      return;
    }
    if (!fits(items.getStackInSlot(1), result)) {
      status = 3;
      return;
    }
    if (energy.getEnergyStored() < r.energy()) {
      status = 4;
      return;
    }
    status = 1;
    energy.spend(r.energy());
    progress++;
    if (progress >= total) {
      items.extractItem(0, r.inputCount(), false);
      var out = items.getStackInSlot(1);
      if (out.isEmpty()) items.setStackInSlot(1, result);
      else {
        out.grow(result.getCount());
        items.setStackInSlot(1, out);
      }
      progress = 0;
      job = "";
    }
    setChanged();
  }

  public boolean accessible(net.minecraft.world.entity.player.Player p) {
    if (isRemoved()
        || level == null
        || p.level() != level
        || level.getBlockEntity(worldPosition) != this) return false;
    if (net.neoforged.fml.ModList.get().isLoaded("sable")) {
      var a =
          io.github.astelev.around_planet.compat.SpaceSensorNavigation.worldPoint(
              level, worldPosition);
      var b =
          io.github.astelev.around_planet.compat.SpaceSensorNavigation.worldPoint(
              level, p.blockPosition());
      return a.distanceSquared(b) <= 64;
    }
    return p.distanceToSqr(
            worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5)
        <= 64;
  }

  @Override
  public net.minecraft.network.chat.Component getDisplayName() {
    return net.minecraft.network.chat.Component.translatable(
        "block.around_planet." + kind().getSerializedName());
  }

  @Override
  public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
      int id,
      net.minecraft.world.entity.player.Inventory inv,
      net.minecraft.world.entity.player.Player p) {
    return new ManufacturingMenu(id, inv, this);
  }

  @Override
  protected void saveAdditional(CompoundTag n, HolderLookup.Provider r) {
    super.saveAdditional(n, r);
    n.put("Items", items.serializeNBT(r));
    n.putInt("Energy", energy.getEnergyStored());
    n.put("OutputFilter", filter.saveOptional(r));
    n.putInt("Progress", progress);
    n.putString("Job", job);
  }

  @Override
  protected void loadAdditional(CompoundTag n, HolderLookup.Provider r) {
    super.loadAdditional(n, r);
    loading = true;
    try {
      items.deserializeNBT(r, n.getCompound("Items"));
    } finally {
      loading = false;
    }
    energy.restore(n.getInt("Energy"));
    if (n.contains("OutputFilter"))
      filter = ItemStack.parseOptional(r, n.getCompound("OutputFilter")).copyWithCount(1);
    else if (n.contains("Selected")) {
      try {
        select(PartKind.valueOf(n.getString("Selected").toUpperCase(java.util.Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
        filter = ItemStack.EMPTY;
      }
    }
    progress = Math.clamp(n.getInt("Progress"), 0, 72000);
    job = n.getString("Job");
  }
}
