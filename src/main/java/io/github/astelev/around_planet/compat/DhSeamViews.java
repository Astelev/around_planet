package io.github.astelev.around_planet.compat;

import io.github.astelev.around_planet.diagnostics.SeamDiagnostics;
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
        var mc=Minecraft.getInstance();if(!active()){reset("inactive");return;}
        var topology=PlanetBorders.get(mc.level.getWorldBorder());var current=module.ClientRenderStateRef.get();
        if(topology==null || current==null){reset("no_topology_or_primary");return;}
        if(primary!=null && primary!=current || width!=0 && width!=topology.width())reset("primary_reference_or_width_changed");
        primary=current;width=topology.width();double x=mc.player.getX();
        if(opposite!=null && !Double.isNaN(lastX) && Math.abs(x-lastX)>width/2.0 && Math.abs(x-oppositeX)<width/4.0) {
            var old=primary;primary=opposite;opposite=old;module.ClientRenderStateRef.set(primary);swaps++;SeamDiagnostics.event("client","dh.swap",()->"primary="+SeamDiagnostics.id(primary)+" opposite="+SeamDiagnostics.id(opposite)+" x="+x);
        }
        lastX=x;
    }
    public synchronized void afterTick() {
        if(!active())return;var mc=Minecraft.getInstance();var topology=PlanetBorders.get(mc.level.getWorldBorder());
        primary=module.ClientRenderStateRef.get();if(topology==null || primary==null)return;
        int approach=Math.min(PlanetConfig.DH_PREVIEW.getAsInt(),primary.viewDiameterInBlocks/2);
        if(topology.distanceToSeam(mc.player.getX())>approach){reset("left_approach_zone");return;}
        if(opposite!=null && opposite.viewDiameterInBlocks!=primary.viewDiameterInBlocks)reset("view_diameter_changed");
        if(opposite==null){long start=SeamDiagnostics.start();opposite=new ClientRenderState(level,module.fullDataSourceProvider);SeamDiagnostics.time("client","dh.create_opposite",start);SeamDiagnostics.event("client","dh.create",()->"primary="+SeamDiagnostics.id(primary)+" opposite="+SeamDiagnostics.id(opposite)+" diameter="+primary.viewDiameterInBlocks);}
        primary=module.ClientRenderStateRef.get();width=topology.width();
        oppositeX=mc.player.getX()+(mc.player.getX()>=0?-width:width);
        long diagnosticStart=SeamDiagnostics.start();opposite.quadtree.tryTick(new DhBlockPos2D((int)Math.floor(oppositeX),(int)Math.floor(mc.player.getZ())));SeamDiagnostics.time("client","dh.opposite_tick",diagnosticStart);
    }
    public synchronized void reload(long pos){SeamDiagnostics.count("client","dh.reload_request");if(opposite!=null)opposite.quadtree.queuePosToReload(pos);}
    public synchronized void reset(){reset("module_reset");}
    public synchronized void reset(String reason){if(opposite!=null){SeamDiagnostics.event("client","dh.reset."+reason,()->"primary="+SeamDiagnostics.id(primary)+" opposite="+SeamDiagnostics.id(opposite)+" swaps="+swaps);opposite.close();}opposite=null;primary=null;lastX=Double.NaN;width=0;}
    public void close(){reset();synchronized(VIEWS){VIEWS.remove(level);}}
    public record View(RenderParams params,int shiftChunks) {}
    public static View select(RenderParams main) {
        DhSeamViews view;synchronized(VIEWS){view=VIEWS.get(main.dhClientLevel);}if(view==null)return null;
        synchronized(view) {
            if(!view.active() || view.opposite==null){SeamDiagnostics.count("client","dh.select_not_ready");return null;}
            double shift=main.exactCameraPosition.x>=0?view.width:-view.width;
            double sourceX=main.exactCameraPosition.x-shift;
            if(Math.abs(sourceX-view.oppositeX)>view.width/4.0){SeamDiagnostics.count("client","dh.select_stale_position");return null;}
            var p=new RenderParams();p.update((com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam)main);
            p.dhClientWorld=main.dhClientWorld;p.dhClientLevel=main.dhClientLevel;p.clientLevelWrapper=main.clientLevelWrapper;
            p.lightmap=main.lightmap;p.genericRenderer=main.genericRenderer;p.vanillaFogEnabled=main.vanillaFogEnabled;
            p.renderBufferHandler=view.opposite.renderBufferHandler;
            p.exactCameraPosition=new DhVec3d(sourceX,main.exactCameraPosition.y,main.exactCameraPosition.z);
            p.apiCopy.update(p);view.passes++;
            return new View(p,(int)shift/16);
        }
    }
    public static String diagnostic() {
        var result=new StringBuilder("genPlan="+com.seibel.distanthorizons.core.config.Config.Common.WorldGenerator.generatorPlan.get()+" genMode="+com.seibel.distanthorizons.core.config.Config.Common.WorldGenerator.chunkGeneratorMode.get()+" threads="+com.seibel.distanthorizons.core.config.Config.Common.MultiThreading.numberOfThreads.get()+" dutyCycle="+com.seibel.distanthorizons.core.config.Config.Common.MultiThreading.threadRunTimeRatio.get()+" limitIfServerUnhealthy="+com.seibel.distanthorizons.core.config.Config.Common.WorldGenerator.limitIfServerUnhealthy.get()+" pauseIfMovingQuickly="+com.seibel.distanthorizons.core.config.Config.Common.WorldGenerator.pauseIfMovingQuickly.get()+" shaderPack="+shaderPackActive()+" mode="+com.seibel.distanthorizons.core.config.Config.Client.Advanced.Debugging.rendererMode.get()+" wireframe="+com.seibel.distanthorizons.core.config.Config.Client.Advanced.Debugging.renderWireframe.get()+" appendPasses="+DhSeamRenderer.passes);
        java.util.List<DhSeamViews> views;synchronized(VIEWS){views=java.util.List.copyOf(VIEWS.values());}
        for(var view:views)synchronized(view) {
            var primary=view.module.ClientRenderStateRef.get();
            result.append(" [level="+SeamDiagnostics.id(view.level)+" active=").append(view.active()).append(" primary=").append(SeamDiagnostics.id(primary))
                .append(" opposite=").append(SeamDiagnostics.id(view.opposite)).append(" primaryList=").append(bufferCounts(primary))
                .append(" oppositeList=").append(bufferCounts(view.opposite)).append(" sourceX=").append(view.oppositeX)
                .append(" diameter=").append(primary==null?0:primary.viewDiameterInBlocks).append(" swaps=").append(view.swaps).append(" selectedPasses=").append(view.passes).append(']');
        }return result.toString();
    }
    private static String bufferCounts(ClientRenderState state){if(state==null)return "none";var buffers=state.renderBufferHandler.getColumnRenderBuffers();int uploaded=0;for(int i=0;i<buffers.size();i++)if(buffers.get(i).buffersUploaded)uploaded++;return uploaded+"/"+buffers.size();}
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
