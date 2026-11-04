package io.github.astelev.around_planet.compat;

import com.seibel.distanthorizons.core.level.ClientLevelModule;
import com.seibel.distanthorizons.core.level.ClientLevelModule.ClientRenderState;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.math.DhVec3d;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.portal.PlanetPortals;
import io.github.astelev.around_planet.PlanetConfig;
import net.minecraft.client.Minecraft;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import java.util.Map;
import java.util.IdentityHashMap;

/** Two reusable render trees backed by one canonical DH data provider. No cloned world or database. */
public final class DhPortalViews {
    private static final Map<IDhClientLevel,DhPortalViews> VIEWS = new IdentityHashMap<>();
    private final ClientLevelModule module;
    private final IDhClientLevel level;
    private ClientRenderState primary, opposite;
    private double lastX = Double.NaN, oppositeX;
    private int topologyWidth;
    public long swaps, portalPasses;

    public DhPortalViews(ClientLevelModule module, IDhClientLevel level) {
        this.module=module; this.level=level;
        synchronized(VIEWS) {VIEWS.put(level,this);}
    }

    public synchronized void beforeTick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null)return;
        if(!PlanetConfig.SPEC.isLoaded() || !PlanetConfig.DH_PORTAL_RENDER.getAsBoolean()) {reset();return;}
        var topology=PlanetBorders.get(mc.level.getWorldBorder());
        var current=module.ClientRenderStateRef.get();
        if(topology==null || current==null) {reset();return;}
        if(primary!=null && primary!=current || topologyWidth!=0 && topologyWidth!=topology.width())reset();
        primary=current;topologyWidth=topology.width();
        double x=stablePosition().x;
        if(opposite!=null && !Double.isNaN(lastX) && Math.abs(x-lastX)>topology.width()/2.0
                && Math.abs(x-oppositeX)<topology.width()/4.0) {
            var old=primary;primary=opposite;opposite=old;
            module.ClientRenderStateRef.set(primary);
            swaps++;
        }
        lastX=x;
    }

    public synchronized void afterTick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null)return;
        if(!PlanetConfig.SPEC.isLoaded() || !PlanetConfig.DH_PORTAL_RENDER.getAsBoolean())return;
        var topology=PlanetBorders.get(mc.level.getWorldBorder());
        primary=module.ClientRenderStateRef.get();
        if(topology==null || primary==null)return;
        // Bound the additional work to the approach to our seam, not every portal in the world.
        var position=stablePosition();
        int approach=Math.min(PlanetConfig.DH_PREVIEW_DISTANCE.getAsInt(),primary.viewDiameterInBlocks/2);
        if(topology.distanceToSeam(position.x)>approach) {reset();primary=module.ClientRenderStateRef.get();return;}
        if(opposite!=null && opposite.viewDiameterInBlocks!=primary.viewDiameterInBlocks)reset();
        if(opposite==null)opposite=new ClientRenderState(level,module.fullDataSourceProvider);
        primary=module.ClientRenderStateRef.get();topologyWidth=topology.width();
        oppositeX=position.x+(position.x>=0?-topology.width():topology.width());
        opposite.quadtree.tryTick(new DhBlockPos2D((int)Math.floor(oppositeX),(int)Math.floor(position.z)));
    }

    public synchronized void reload(long pos) {if(opposite!=null)opposite.quadtree.queuePosToReload(pos);}
    public synchronized void reset() {
        if(opposite!=null)opposite.close();opposite=null;primary=null;lastX=Double.NaN;topologyWidth=0;
    }
    public void close() {reset();synchronized(VIEWS) {VIEWS.remove(level);}}

    public static boolean managedPass() {
        if(!PortalRendering.isRendering() || PortalRendering.getPortalLayer()!=1)return false;
        var iris=(com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor)
                com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector.INSTANCE.get(
                        com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor.class);
        if(iris!=null && iris.isShaderPackInUse())return false;
        var portal=PortalRendering.getRenderingPortal();
        return portal!=null && PlanetPortals.owned(portal) && portal.getDestDim()==portal.level().dimension();
    }
    public static boolean select(RenderParams params) {
        if(!managedPass())return false;
        DhPortalViews view;
        synchronized(VIEWS) {view=VIEWS.get(params.dhClientLevel);}
        if(view==null)return false;
        synchronized(view) {
            if(view.opposite==null)return false;
            var camera=PortalRendering.getRenderingCameraPos();
            if(Math.abs(camera.x-view.oppositeX)>view.topologyWidth/4.0)return false;
            params.renderBufferHandler=view.opposite.renderBufferHandler;
            params.exactCameraPosition=new DhVec3d(camera.x,camera.y,camera.z);
            params.apiCopy.update(params);
            view.portalPasses++;
            return true;
        }
    }
    public static boolean ready() {
        if(!managedPass())return false;
        // RenderParams has selected the opposite buffers before the cancellation event fires.
        return SELECTED.get();
    }
    private static final ThreadLocal<Boolean> SELECTED=ThreadLocal.withInitial(()->false);
    public static void selectForPass(RenderParams params) {SELECTED.set(select(params));}
    private static net.minecraft.world.phys.Vec3 stablePosition() {
        var saved=RenderStates.originalPlayerPos;
        return saved!=null?saved:Minecraft.getInstance().player.position();
    }
    public record Snapshot(Object primary,Object opposite,long swaps,long portalPasses,int oppositeBuffers,String diagnostics) {}
    public static Snapshot snapshot() {
        synchronized(VIEWS) {
            for(var view:VIEWS.values()) {
                synchronized(view) {
                    if(view.primary!=null && view.opposite!=null) {
                        var sections=new java.util.ArrayList<com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection>();
                        view.opposite.quadtree.populateListWithEnabledRenderSections(sections);
                        long built=sections.stream().filter(s->s.renderBufferContainer!=null).count();
                        long enabled=sections.stream().filter(s->s.getRenderingEnabled()).count();
                        var buffers=view.opposite.renderBufferHandler.getColumnRenderBuffers();
                        int uploaded=0;
                        for(int i=0;i<buffers.size();i++)if(buffers.get(i).buffersUploaded)uploaded++;
                        return new Snapshot(view.primary,view.opposite,view.swaps,view.portalPasses,
                                uploaded,
                                "sections="+sections.size()+" built="+built+" enabled="+enabled);
                    }
                }
            }
        }
        return null;
    }
}
