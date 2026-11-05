package io.github.astelev.around_planet.client;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.border.PlanetBorders;
import io.github.astelev.around_planet.seam.SeamGeometry;
import io.github.astelev.around_planet.seam.SeamPlacement;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.*;

/** Independent, bounded canonical GPU cache. Image placement never becomes its identity. */
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,value=Dist.CLIENT)
public final class SeamMeshCache {
    private static final long MAX_BYTES=64L*1024*1024;
    private static final int MAX_SECTIONS=512;
    private static State state;
    public static long builds,draws;
    private record Request(long key,SectionPos image,long revision,boolean local,int signature,int dataSignature) {}
    private record Completed(Request request,SectionCompiler.Results result) {}
    private static final class Mesh implements AutoCloseable {
        final Map<RenderType,VertexBuffer> layers=new HashMap<>();
        long bytes,lastUse,draws;
        int signature,dataSignature;
        boolean localBaseline;
        @Override public void close(){layers.values().forEach(VertexBuffer::close);}
    }
    private static final class State {
        final ClientLevel level;
        final LinkedHashMap<Long,Mesh> meshes=new LinkedHashMap<>(16,.75F,true);
        final LinkedHashMap<Long,Request> pending=new LinkedHashMap<>();
        final Map<Long,Long> revisions=new HashMap<>();
        final ConcurrentLinkedQueue<Completed> completed=new ConcurrentLinkedQueue<>();
        final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"around-planet-mesh");t.setDaemon(true);return t;});
        SectionBufferBuilderPack pack; // worker-owned, including disposal
        volatile boolean closed;
        int inFlight;
        long runningKey=Long.MIN_VALUE;
        long revision,bytes;
        int localCursor;
        State(ClientLevel level){this.level=level;}
        void close() {
            closed=true;meshes.values().forEach(Mesh::close);meshes.clear();pending.clear();revisions.clear();
            Completed item;while((item=completed.poll())!=null)if(item.result!=null)item.result.release();
            worker.execute(()->{if(pack!=null)pack.close();});worker.shutdown();
        }
    }
    public static void changed(ClientLevel level,ChunkPos image) {
        if(!PlanetConfig.RETAIN_MESHES.getAsBoolean())return;
        if(state==null || state.level!=level){clear();state=new State(level);}
        var topology=PlanetBorders.get(level.getWorldBorder());if(topology==null)return;
        // Refresh neighboring faces too; no reads from the generator or server chunk cache.
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            var chunk=SeamImages.get(level,image.x+dx,image.z+dz);if(chunk==null)continue;
            int canonical=SeamGeometry.sourceX(topology,chunk.getPos().x);
            for(int y=level.getMinSection();y<level.getMaxSection();y++) {
                long key=SectionPos.asLong(canonical,y,chunk.getPos().z);
                if(chunk.getSection(level.getSectionIndexFromSectionY(y)).hasOnlyAir()) {
                    var old=state.meshes.remove(key);if(old!=null){state.bytes-=old.bytes;old.close();}
                    state.pending.remove(key);state.revisions.remove(key);continue;
                }
                long revision=++state.revision;state.revisions.put(key,revision);
                state.pending.put(key,new Request(key,SectionPos.of(chunk.getPos(),y),revision,false,0,dataSignature(chunk,y)));
            }
        }
        while(state.pending.size()>MAX_SECTIONS) {
            long key=state.pending.keySet().iterator().next();state.pending.remove(key);state.revisions.remove(key);
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || !PlanetConfig.SPEC.isLoaded())return;
        var bound=PlanetBorders.get(mc.level.getWorldBorder());
        if(bound!=null && PlanetConfig.RETAIN_MESHES.getAsBoolean() &&
                bound.distanceToSeam(mc.player.getX())<=Math.max(PlanetConfig.ACTIVATION.getAsInt(),mc.options.getEffectiveRenderDistance()*16)) {
            if(state==null || state.level!=mc.level){clear();state=new State(mc.level);}
            captureLocal(state);
        }
        var s=state;if(s==null)return;
        if(mc.level!=s.level || !PlanetConfig.RETAIN_MESHES.getAsBoolean()){clear();return;}
        Completed done;
        int uploads=0;
        while(uploads++<2 && (done=s.completed.poll())!=null) {
            s.inFlight--;s.runningKey=Long.MIN_VALUE;
            if(done.result==null){s.worker.execute(()->{if(s.pack!=null)s.pack.discardAll();});continue;}
            if(!Objects.equals(s.revisions.get(done.request.key),done.request.revision)){
                done.result.release();s.worker.execute(()->s.pack.clearAll());continue;
            }
            var mesh=new Mesh();
            try {
                var layers=done.result.renderedLayers.entrySet().iterator();
                while(layers.hasNext()) {
                    var layer=layers.next();
                    var data=layer.getValue();
                    mesh.bytes+=data.vertexBuffer().remaining()+(data.indexBuffer()==null?0:data.indexBuffer().remaining());
                    layers.remove(); // VertexBuffer.upload owns/closes this MeshData.
                    var vbo=new VertexBuffer(VertexBuffer.Usage.STATIC);mesh.layers.put(layer.getKey(),vbo);
                    vbo.bind();vbo.upload(data);
                }
                mesh.lastUse=mc.level.getGameTime();mesh.signature=done.request.signature;
                mesh.dataSignature=done.request.dataSignature;mesh.localBaseline=done.request.local;
                var old=s.meshes.put(done.request.key,mesh);s.bytes+=mesh.bytes;
                if(old!=null){s.bytes-=old.bytes;old.close();}
                builds++;
            } catch(Throwable failure){mesh.close();throw failure;}
            finally {VertexBuffer.unbind();done.result.release();s.worker.execute(()->s.pack.clearAll());}
        }
        // An evicted image is not an empty section: never replace a retained mesh with that placeholder.
        s.pending.values().removeIf(request->request.local
                ?s.level.getChunkSource().getChunk(request.image.x(),request.image.z(),ChunkStatus.FULL,false)==null
                :SeamImages.get(s.level,request.image.x(),request.image.z())==null);
        var topology=PlanetBorders.get(s.level.getWorldBorder());
        long now=s.level.getGameTime();
        var retained=s.meshes.entrySet().iterator();
        while(retained.hasNext()) {
            var entry=retained.next();var section=SectionPos.of(entry.getKey());var mesh=entry.getValue();
            int width=topology==null?0:topology.width()/16;
            if(width!=0 && (SeamImages.get(s.level,section.x()+width,section.z())!=null
                    || SeamImages.get(s.level,section.x()-width,section.z())!=null
                    || (topology.distanceToSeam(mc.player.getX())<=Math.max(PlanetConfig.ACTIVATION.getAsInt(),mc.options.getEffectiveRenderDistance()*16)
                        && s.level.getChunkSource().getChunk(section.x(),section.z(),ChunkStatus.FULL,false)!=null)))mesh.lastUse=now;
            if(now-mesh.lastUse>200) {s.bytes-=mesh.bytes;mesh.close();retained.remove();}
        }
        s.revisions.keySet().removeIf(key->!s.meshes.containsKey(key) && !s.pending.containsKey(key) && s.runningKey!=key);
        while(s.meshes.size()>MAX_SECTIONS || s.bytes>MAX_BYTES) {
            var it=s.meshes.entrySet().iterator();var mesh=it.next().getValue();s.bytes-=mesh.bytes;mesh.close();it.remove();
            s.revisions.keySet().removeIf(key->!s.meshes.containsKey(key) && !s.pending.containsKey(key) && s.runningKey!=key);
        }
        // One snapshot per tick, one outstanding compile/upload job owns the builder pack.
        if(s.inFlight>=1 || s.pending.isEmpty())return;
        var iterator=s.pending.values().iterator();var request=iterator.next();iterator.remove();
        var region=new RenderRegionCache().createRegion(s.level,request.image,false);
        if(region==null)return;
        var camera=mc.gameRenderer.getMainCamera().getPosition();
        var sorting=VertexSorting.byDistance((float)(camera.x-request.image.minBlockX()),
                (float)(camera.y-request.image.minBlockY()),(float)(camera.z-request.image.minBlockZ()));
        s.inFlight++;s.runningKey=request.key;
        s.worker.execute(()->{
            SectionCompiler.Results result=null;
            try {
                if(s.pack==null)s.pack=new SectionBufferBuilderPack();
                var compiler=new SectionCompiler(mc.getBlockRenderer(),mc.getBlockEntityRenderDispatcher());
                result=compiler.compile(request.image,region,sorting,s.pack);
                synchronized(s) {
                    if(s.closed)result.release();else s.completed.add(new Completed(request,result));
                }
            } catch(Throwable failure) {
                if(result!=null)result.release();
                AroundPlanet.LOGGER.error("Failed to build retained seam section {}",request.image,failure);
                synchronized(s){if(!s.closed)s.completed.add(new Completed(request,null));}
            }
        });
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var s=state;var mc=Minecraft.getInstance();
        if(s==null || mc.level!=s.level || mc.player==null)return;
        var camera=event.getCamera().getPosition();
        double range=(mc.options.getEffectiveRenderDistance()+1)*16.0;
        for(var type:RenderType.chunkBufferLayers()) {
            if(RenderLevelStageEvent.Stage.fromRenderType(type)!=event.getStage())continue;
            type.setupRenderState();
            try {
                var shader=RenderSystem.getShader();if(shader==null)continue;
                if(shader.CHUNK_OFFSET!=null)shader.CHUNK_OFFSET.set(0.0F,0.0F,0.0F);
                for(var entry:s.meshes.entrySet()) {
                    var section=SectionPos.of(entry.getKey());var mesh=entry.getValue();
                    var topology=PlanetBorders.get(s.level.getWorldBorder());if(topology==null)continue;
                    int imageX=SeamPlacement.nearest(section.x(),(int)Math.floor(camera.x/16),topology.width()/16);
                    var origin=new net.minecraft.core.BlockPos(imageX*16,section.minBlockY(),section.minBlockZ());
                    if(Math.abs(origin.getX()-camera.x)>range || Math.abs(origin.getZ()-camera.z)>range)continue;
                    var vbo=mesh.layers.get(type);if(vbo==null || SeamSectionCoverage.ready(s.level,imageX,section.y(),section.z(),type))continue;
                    if(!event.getFrustum().isVisible(new AABB(origin.getX(),origin.getY(),origin.getZ(),origin.getX()+16,origin.getY()+16,origin.getZ()+16)))continue;
                    var matrix=new Matrix4f(event.getModelViewMatrix()).translate((float)(origin.getX()-camera.x),
                            (float)(origin.getY()-camera.y),(float)(origin.getZ()-camera.z));
                    vbo.bind();vbo.drawWithShader(matrix,event.getProjectionMatrix(),shader);
                    draws++;mesh.draws++;
                }
            } finally {VertexBuffer.unbind();type.clearRenderState();}
        }
    }
    private static void captureLocal(State s) {
        var mc=Minecraft.getInstance();var topology=PlanetBorders.get(s.level.getWorldBorder());
        int radius=PlanetConfig.RADIUS.getAsInt()+1,span=radius*2+1;
        int column=Math.floorMod(s.localCursor++,(radius+1)*span),d=column/span,n=column%span;
        int dz=n==0?0:((n+1)/2)*(n%2==1?-1:1);
        int half=topology.width()/32,x=mc.player.getX()>=0?half-1-d:-half+d,z=mc.player.chunkPosition().z+dz;
        var chunk=s.level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);if(chunk==null)return;
        var ys=new ArrayList<Integer>();
        for(int y=s.level.getMinSection();y<s.level.getMaxSection();y++)ys.add(y);
        int eye=mc.player.blockPosition().getY()>>4;ys.sort(Comparator.comparingInt(y->Math.abs(y-eye)));
        for(int y:ys) {
            long key=SectionPos.asLong(x,y,z);
            if(chunk.getSection(s.level.getSectionIndexFromSectionY(y)).hasOnlyAir())continue;
            var origin=SectionPos.of(x,y,z).origin();if(!mc.levelRenderer.isSectionCompiled(origin))continue;
            int dataSignature=dataSignature(chunk,y),signature=signature(chunk,y,dataSignature);
            var mesh=s.meshes.get(key);var pending=s.pending.get(key);
            if(mesh!=null && !mesh.localBaseline && mesh.dataSignature==dataSignature) {
                // First native observation of unchanged image geometry is a handover, not an edit.
                // Establish its native light baseline without replacing the retained GPU buffer.
                mesh.signature=signature;mesh.localBaseline=true;continue;
            }
            if(pending!=null || s.runningKey==key || mesh!=null && mesh.signature==signature)continue;
            long revision=++s.revision;s.revisions.put(key,revision);
            s.pending.put(key,new Request(key,SectionPos.of(x,y,z),revision,true,signature,dataSignature));
        }
        while(s.pending.size()>MAX_SECTIONS) {
            long key=s.pending.keySet().iterator().next();s.pending.remove(key);s.revisions.remove(key);
        }
    }
    private static int dataSignature(LevelChunk chunk,int y) {
        var data=new FriendlyByteBuf(Unpooled.buffer());
        try {
            chunk.getSection(chunk.getLevel().getSectionIndexFromSectionY(y)).write(data);
            return ByteBufUtil.hashCode(data);
        } finally {data.release();}
    }
    private static int signature(LevelChunk chunk,int y,int hash) {
        var engine=chunk.getLevel().getChunkSource().getLightEngine();
        for(var layer:List.of(LightLayer.SKY,LightLayer.BLOCK)) {
            var light=engine.getLayerListener(layer).getDataLayerData(SectionPos.of(chunk.getPos(),y));
            hash=31*hash+(light==null?0:Arrays.hashCode(light.getData()));
        }
        return hash;
    }
    public static boolean hasMesh(int x,int y,int z){return state!=null && state.meshes.containsKey(SectionPos.asLong(x,y,z));}
    public static long drawCount(int x,int y,int z){var mesh=state==null?null:state.meshes.get(SectionPos.asLong(x,y,z));return mesh==null?0:mesh.draws;}
    public static Object identity(int x,int y,int z){return state==null?null:state.meshes.get(SectionPos.asLong(x,y,z));}
    public static boolean settled(int x,int y,int z) {
        long key=SectionPos.asLong(x,y,z);
        return state!=null && state.meshes.containsKey(key) && !state.pending.containsKey(key) && state.runningKey!=key;
    }
    public static int size(){return state==null?0:state.meshes.size();}
    public static void clear(){var s=state;state=null;if(s!=null)synchronized(s){s.close();}}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(state!=null && event.getLevel()==state.level)clear();}
    private SeamMeshCache() {}
}
