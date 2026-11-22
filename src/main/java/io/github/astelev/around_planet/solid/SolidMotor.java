package io.github.astelev.around_planet.solid;
import io.github.astelev.around_planet.propulsion.PropulsionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;
/** A latched burn along one straight stack, farthest charged section first. No chunk requests. */
public final class SolidMotor {
    public static final int MAX_COLUMNS=128;
    private final PropulsionBlockEntity nozzle;private List<SolidColumnBlockEntity> columns=List.of();private boolean ignited,spent,complete;
    public SolidMotor(PropulsionBlockEntity nozzle){this.nozzle=nozzle;}
    public boolean active(){return ignited;}
    public void tick(int signal){
        scan();
        if(!ignited&&signal==0)spent=false;
        if(!ignited&&!spent&&signal>0&&complete&&columns.stream().anyMatch(c->c.fuel()>0)&&claim())ignited=true;
        if(ignited)claim();
    }
    private void scan(){
        var list=new ArrayList<SolidColumnBlockEntity>();var level=nozzle.getLevel();complete=true;
        Direction axis=nozzle.getBlockState().getValue(BlockStateProperties.FACING);var pos=nozzle.getBlockPos();
        for(int i=0;i<=MAX_COLUMNS;i++){
            pos=pos.relative(axis);if(!level.hasChunkAt(pos)){complete=false;break;}
            var state=level.getBlockState(pos);if(!state.is(SolidRegistry.COLUMN.get())||state.getValue(BlockStateProperties.AXIS)!=axis.getAxis())break;
            if(i==MAX_COLUMNS){complete=false;break;}
            if(!(level.getBlockEntity(pos) instanceof SolidColumnBlockEntity column)){complete=false;break;}list.add(column);
        }
        for(var c:columns)if(!list.contains(c)&&c.owner==this)c.owner=null;
        columns=List.copyOf(list);
    }
    private boolean claim(){for(var c:columns)if(c.owner!=null&&c.owner!=this&&c.owner.active())return false;for(var c:columns)c.owner=this;return true;}
    public double burn(double dt){
        if(!ignited||!complete||!Double.isFinite(dt)||dt<=0||!claim())return 0;
        double mass=0,requested=SolidPropellant.FLOW*dt;
        for(int i=columns.size()-1;i>=0&&requested>1e-10;i--){
            var c=columns.get(i);if(c.isRemoved()||c.getLevel().getBlockEntity(c.getBlockPos())!=c){complete=false;return mass*SolidPropellant.exhaustSpeed();}
            double kg=SolidMassBridge.kgPerPortion(c);if(kg<=0)continue;
            c.burnPositive(nozzle.getBlockState().getValue(BlockStateProperties.FACING).getAxisDirection()==Direction.AxisDirection.POSITIVE);double used=c.consume(requested/kg)*kg;mass+=used;requested=Math.max(0,requested-used);
        }
        if(columns.stream().noneMatch(c->c.fuel()>1e-9)){ignited=false;spent=true;release();}
        nozzle.setChanged();return mass*SolidPropellant.exhaustSpeed();
    }
    public void release(){for(var c:columns)if(c.owner==this)c.owner=null;columns=List.of();}
    public void save(net.minecraft.nbt.CompoundTag n){n.putBoolean("SolidIgnited",ignited);n.putBoolean("SolidSpent",spent);}
    public void load(net.minecraft.nbt.CompoundTag n){ignited=n.getBoolean("SolidIgnited");spent=n.getBoolean("SolidSpent");}
}
