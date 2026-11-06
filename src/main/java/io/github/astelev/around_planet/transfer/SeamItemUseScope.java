package io.github.astelev.around_planet.transfer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
public final class SeamItemUseScope {
    public static final class Frame {final ServerPlayer player;BlockHitResult hit;Frame(ServerPlayer p){player=p;}}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    public static ServerPlayer actor(){var f=CURRENT.get();return f==null?null:f.player;}
    public static Frame enter(ServerPlayer p){var old=CURRENT.get();CURRENT.set(new Frame(p));return old;}
    public static void hit(BlockHitResult hit){var f=CURRENT.get();if(f!=null)f.hit=hit;}
    public static BlockHitResult hit(){var f=CURRENT.get();return f==null?null:f.hit;}
    public static void restore(Frame f){if(f==null)CURRENT.remove();else CURRENT.set(f);}
    private SeamItemUseScope(){}
}
