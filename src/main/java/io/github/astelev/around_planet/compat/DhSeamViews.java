package io.github.astelev.around_planet.compat;

import com.seibel.distanthorizons.core.level.ClientLevelModule;
import com.seibel.distanthorizons.core.level.ClientLevelModule.ClientRenderState;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.math.DhVec3d;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import net.minecraft.client.Minecraft;
import java.util.*;

/** Canonical DH data and two retained visibility trees; placement is a pure translation. */
public final class DhSeamViews {
    private static final Map<IDhClientLevel,DhSeamViews> VIEWS=new IdentityHashMap<>();
    private final ClientLevelModule module;
    private final IDhClientLevel level;
    private ClientRenderState primary,opposite;
    private double lastX=Double.NaN,oppositeX;
    private int width;
    private long swaps,passes;
    public DhSeamViews(ClientLevelModule module,IDhClientLevel level) {
        this.module=module;this.level=level;synchronized(VIEWS){VIEWS.put(level,this);}
    }
    private boolean active() {
        var mc=Minecraft.getInstance();
        var wrapper=level.getClientLevelWrapper();
        return wrapper!=null && mc.level!=null && mc.player!=null && PlanetConfig.SPEC.isLoaded()
                && PlanetConfig.DH_SEAM.getAsBoolean() && !shaderPackActive()
                && com.seibel.distanthorizons.core.config.Config.Client.Advanced.Debugging.rendererMode.get()
                    ==com.seibel.distanthorizons.api.enums.rendering.EDhApiRendererMode.DEFAULT
                && !com.seibel.distanthorizons.core.config.Config.Client.Advanced.Debugging.renderWireframe.get()
                && wrapper.getWrappedMcObject()==mc.level;
    }
    public static boolean shaderPackActive() {
        var iris=(com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor)
                com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector.INSTANCE.get(
                        com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor.class);
        return iris!=null && iris.isShaderPackInUse();
    }
    public synchronized void beforeTick() {
        var mc=Minecraft.getInstance();if(!active()){reset();return;}
        var topology=PlanetBorders.get(mc.level.getWorldBorder());var current=module.ClientRenderStateRef.get();
        if(topology==null || current==null){reset();return;}
        if(primary!=null && primary!=current || width!=0 && width!=topology.width())reset();
        primary=current;width=topology.width();double x=mc.player.getX();
        if(opposite!=null && !Double.isNaN(lastX) && Math.abs(x-lastX)>width/2.0 && Math.abs(x-oppositeX)<width/4.0) {
            var old=primary;primary=opposite;opposite=old;module.ClientRenderStateRef.set(primary);swaps++;
        }
        lastX=x;
    }
    public synchronized void afterTick() {
        if(!active())return;var mc=Minecraft.getInstance();var topology=PlanetBorders.get(mc.level.getWorldBorder());
        primary=module.ClientRenderStateRef.get();if(topology==null || primary==null)return;
        int approach=Math.min(PlanetConfig.DH_PREVIEW.getAsInt(),primary.viewDiameterInBlocks/2);
        if(topology.distanceToSeam(mc.player.getX())>approach){reset();return;}
        if(opposite!=null && opposite.viewDiameterInBlocks!=primary.viewDiameterInBlocks)reset();
        if(opposite==null)opposite=new ClientRenderState(level,module.fullDataSourceProvider);
        primary=module.ClientRenderStateRef.get();width=topology.width();
        oppositeX=mc.player.getX()+(mc.player.getX()>=0?-width:width);
        opposite.quadtree.tryTick(new DhBlockPos2D((int)Math.floor(oppositeX),(int)Math.floor(mc.player.getZ())));
    }
    public synchronized void reload(long pos){if(opposite!=null)opposite.quadtree.queuePosToReload(pos);}
    public synchronized void reset(){if(opposite!=null)opposite.close();opposite=null;primary=null;lastX=Double.NaN;width=0;}
    public void close(){reset();synchronized(VIEWS){VIEWS.remove(level);}}
    public record View(RenderParams params,int shiftChunks) {}
    public static View select(RenderParams main) {
        DhSeamViews view;synchronized(VIEWS){view=VIEWS.get(main.dhClientLevel);}if(view==null)return null;
        synchronized(view) {
            if(!view.active() || view.opposite==null)return null;
            double shift=main.exactCameraPosition.x>=0?view.width:-view.width;
            double sourceX=main.exactCameraPosition.x-shift;
            if(Math.abs(sourceX-view.oppositeX)>view.width/4.0)return null;
            var p=new RenderParams();p.update((com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam)main);
            p.dhClientWorld=main.dhClientWorld;p.dhClientLevel=main.dhClientLevel;p.clientLevelWrapper=main.clientLevelWrapper;
            p.lightmap=main.lightmap;p.genericRenderer=main.genericRenderer;p.vanillaFogEnabled=main.vanillaFogEnabled;
            p.renderBufferHandler=view.opposite.renderBufferHandler;
            p.exactCameraPosition=new DhVec3d(sourceX,main.exactCameraPosition.y,main.exactCameraPosition.z);
            p.apiCopy.update(p);view.passes++;
            return new View(p,(int)shift/16);
        }
    }
    public record Snapshot(Object primary,Object opposite,long swaps,long passes,int uploaded,String diagnostics) {}
    public static Snapshot snapshot() {
        synchronized(VIEWS){for(var view:VIEWS.values())synchronized(view) {
            if(view.primary==null || view.opposite==null)continue;
            int n=0;var buffers=view.opposite.renderBufferHandler.getColumnRenderBuffers();
            for(int i=0;i<buffers.size();i++)if(buffers.get(i).buffersUploaded)n++;
            long vertices=0;int zeroIds=0;
            for(int i=0;i<buffers.size();i++)for(var vbo:buffers.get(i).vboOpaqueWrappers)
                if(vbo instanceof com.seibel.distanthorizons.common.render.openGl.glObject.buffer.GLVertexBuffer gl) {
                    vertices+=gl.getVertexCount();if(gl.getId()==0)zeroIds++;
                }
            return new Snapshot(view.primary,view.opposite,view.swaps,view.passes,n,
                    "sourceX="+view.oppositeX+" width="+view.width+" vertices="+vertices+" zeroIds="+zeroIds);
        }}
        return null;
    }
}
