package io.github.astelev.around_planet.manufacturing;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Server-owned selection; preview slot cannot be picked up, shifted, dragged or automated. */
public final class ManufacturingMenu extends AbstractContainerMenu {
  public final ManufacturingBlockEntity machine;
  public final MachineKind kind;
  private final net.minecraft.world.SimpleContainer preview =
      new net.minecraft.world.SimpleContainer(1);
  public final ContainerData data;

  public static ManufacturingMenu client(
      int id, Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf b) {
    var be = inv.player.level().getBlockEntity(b.readBlockPos());
    if (!(be instanceof ManufacturingBlockEntity machine))
      throw new IllegalStateException("Missing manufacturing block");
    return new ManufacturingMenu(id, inv, machine);
  }

  public ManufacturingMenu(int id, Inventory inv, ManufacturingBlockEntity be) {
    super(ManufacturingRegistry.MENU.get(), id);
    machine = be;
    kind = be.kind();
    data =
        inv.player.level().isClientSide
            ? new SimpleContainerData(9)
            : new ContainerData() {
              public int get(int i) {
                return switch (i) {
                  case 0 -> be.selected().ordinal();
                  case 1 -> be.progress & 65535;
                  case 2 -> be.progress >>> 16;
                  case 3 -> be.total & 65535;
                  case 4 -> be.total >>> 16;
                  case 5 -> be.energy.getEnergyStored() & 65535;
                  case 6 -> be.energy.getEnergyStored() >>> 16;
                  case 7 ->
                      be.environment().equals("orbit")
                          ? 2
                          : be.environment().equals("moon") ? 1 : 0;
                  case 8 -> be.status;
                  default -> 0;
                };
              }

              public void set(int i, int v) {}

              public int getCount() {
                return 9;
              }
            };
    addSlot(new SlotItemHandler(be.items, 0, 39, 73));
    addSlot(
        new SlotItemHandler(be.items, 1, 120, 73) {
          @Override
          public boolean mayPlace(ItemStack s) {
            return false;
          }
        });
    addSlot(
        new Slot(preview, 0, 80, 73) {
          @Override
          public boolean mayPlace(ItemStack s) {
            return false;
          }

          @Override
          public boolean mayPickup(Player p) {
            return false;
          }
        });
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(inv, 9 + row * 9 + col, 8 + col * 18, 121 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 179));
    addDataSlots(data);
  }

  @Override
  public void broadcastChanges() {
    if (!machine.getLevel().isClientSide) preview.setItem(0, machine.preview());
    super.broadcastChanges();
  }

  @Override
  public boolean stillValid(Player p) {
    return machine.accessible(p);
  }

  @Override
  public boolean clickMenuButton(Player p, int id) {
    if (!stillValid(p)
        || p.isSpectator()
        || id < 0
        || id >= PartKind.values().length
        || !kind.supports(PartKind.values()[id])) return false;
    machine.select(PartKind.values()[id]);
    broadcastChanges();
    return true;
  }

  public int energy() {
    return (data.get(5) & 65535) | ((data.get(6) & 65535) << 16);
  }

  public int progress() {
    return (data.get(1) & 65535) | ((data.get(2) & 65535) << 16);
  }

  public int total() {
    return (data.get(3) & 65535) | ((data.get(4) & 65535) << 16);
  }

  @Override
  public boolean canTakeItemForPickAll(ItemStack s, Slot slot) {
    return slot.index != 2 && super.canTakeItemForPickAll(s, slot);
  }

  @Override
  public ItemStack quickMoveStack(Player p, int index) {
    if (index == 2 || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    var slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var copy = stack.copy();
    if (index < 2) {
      if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    return copy;
  }
}
