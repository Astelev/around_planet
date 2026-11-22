package io.github.astelev.around_planet.solid;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.items.IItemHandler;
public final class SolidColumnBlockEntity extends BlockEntity {
    private double fuel;private boolean burnPositive=true;private int sent=-1;
    SolidMotor owner;
    public final IItemHandler inventory=new IItemHandler(){
        public int getSlots(){return 1;}
        public ItemStack getStackInSlot(int slot){int n=SolidPropellant.portions(fuel);return slot==0&&n>0?new ItemStack(SolidRegistry.PORTION.get(),n):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 64;}
        public boolean isItemValid(int slot,ItemStack stack){return slot==0&&stack.is(SolidRegistry.FUEL.get().asItem());}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(!isItemValid(slot,stack)||!canLoad())return stack;if(!simulate)loadFuel();return stack.copyWithCount(stack.getCount()-1);}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
    };
    public SolidColumnBlockEntity(BlockPos p,BlockState s){super(SolidRegistry.TYPE.get(),p,s);}
    public boolean burnPositive(){return burnPositive;}
    public void burnPositive(boolean value){burnPositive=value;}
    public double fuel(){return fuel;}public double fraction(){return fuel/SolidPropellant.CAPACITY;}
    public boolean canLoad(){return fuel<=1e-9&&(owner==null||!owner.active());}
    public boolean loadFuel(){if(!canLoad())return false;double before=mass();fuel=SolidPropellant.CAPACITY;changed(before);sync();return true;}
    public double consume(double units){double before=mass(),used=Math.min(fuel,Math.max(0,units));fuel=Math.max(0,fuel-used);if(fuel<1e-9)fuel=0;if(used>0){changed(before);int step=(int)Math.ceil(fraction()*32);if(step!=sent){sent=step;sync();}}return used;}
    public double mass(){return SolidPropellant.portions(fuel)*SolidMassBridge.kgPerPortion(this);}
    private void changed(double before){setChanged();if(level!=null&&!level.isClientSide&&!io.github.astelev.around_planet.fuel.FuelMassBridge.pondus()&&net.neoforged.fml.ModList.get().isLoaded("sable"))io.github.astelev.around_planet.compat.SolidNativeMass.changed(this,mass()-before);}
    private void sync(){if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);n.putDouble("SolidFuelUnits",fuel);n.putBoolean("BurnPositive",burnPositive);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);fuel=SolidPropellant.valid(n.getDouble("SolidFuelUnits"));burnPositive=!n.contains("BurnPositive")||n.getBoolean("BurnPositive");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
