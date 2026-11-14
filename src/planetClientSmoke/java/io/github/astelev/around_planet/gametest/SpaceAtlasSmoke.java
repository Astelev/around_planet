package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.client.*;
import io.github.astelev.around_planet.space.*;
import net.minecraft.client.Minecraft;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
/** Actual server-generated atlas, real Sable rotations, GPU lightmap and both planet views. */
final class SpaceAtlasSmoke {
    private static int ticks,stage,at;private static boolean zoomed;private static long revision;private static volatile Throwable failure;
    static void tick(Minecraft mc,ServerSubLevel ship){
        if(stage==8)return;
        ticks++;if(failure!=null)throw new AssertionError(failure);if(ticks>2400)throw new AssertionError("Atlas smoke timeout stage="+stage);
        if(!PlanetAtlasTexture.ready())return;
        if(stage==0){revision=PlanetAtlasTexture.revision;stage=1;at=ticks;rotate(mc,ship,0);return;}
        if(ticks-at<35)return;
        if(stage<=4){
            if(!SpaceRenderView.active())throw new AssertionError("Lost space lighting scope");
            try{var light=mc.gameRenderer.lightTexture();var field=light.getClass().getDeclaredField("lightPixels");field.setAccessible(true);var image=(com.mojang.blaze3d.platform.NativeImage)field.get(light);for(int y=0;y<16;y++)for(int x=0;x<16;x++){int c=image.getPixelRGBA(x,y);if((c&255)<105||((c>>>8)&255)<111||((c>>>16)&255)<122)throw new AssertionError("Space lighting became black at rotation "+stage);}}catch(ReflectiveOperationException e){throw new AssertionError(e);}
            snap(mc,"space-lit-tilt-"+stage+"-alpha26.png");
            io.github.astelev.around_planet.AroundPlanet.LOGGER.info("SPACE_LIGHT PASS rotation={} stable lightmap",(stage-1)*45);
            stage++;at=ticks;if(stage<=4){rotate(mc,ship,(stage-1)*Math.PI/4);return;}
            mc.getSingleplayerServer().execute(()->{try{var level=ship.getLevel();var cell=SpaceCells.get(level).forShip(ship.getUniqueId());var data=SpaceOrbitData.get(level.getServer());double radius=data.navigation(cell.id()).orElseThrow().planet().radius();var position=new SpaceVector(3*radius,0,0);data.put(cell.id(),new SpaceTrajectory("around_planet:overworld",level.getGameTime(),new SpaceState(position,SpaceVector.ZERO),0));io.github.astelev.around_planet.compat.SpaceReentry.entered(level,cell.id(),position);}catch(Throwable e){failure=e;}});return;
        }
        if(stage==5){var p=SpaceHud.current();var eye=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,mc.player.getX()+20,mc.player.getY()+20,mc.player.getZ()+20);eye.xo=eye.getX();eye.yo=eye.getY();eye.zo=eye.getZ();mc.setCameraEntity(eye);look(mc,p.frame().toLocal(new SpaceVector(-p.x(),-p.y(),-p.z())));stage=6;at=ticks;return;}
        if(stage==6){captureGlobe(mc);mc.setScreen(new OrbitMapScreen());stage=7;at=ticks;return;}
        if(stage==7){if(!zoomed){try{var field=OrbitMapScreen.class.getDeclaredField("camera");field.setAccessible(true);var camera=(OrbitCamera)field.get(mc.screen);camera.fit(new SpaceState(new SpaceVector(SpaceHud.current().radius()*3,0,0),new SpaceVector(0,1,0)),SpaceHud.current().radius()*1.3);camera.pitch=.35;zoomed=true;at=ticks;return;}catch(ReflectiveOperationException e){throw new AssertionError(e);}}snap(mc,"map-biome-planet-alpha26.png");if(PlanetAtlasTexture.revision!=revision)throw new AssertionError("Atlas regenerated while rotating/opening map");mc.setScreen(null);mc.setCameraEntity(mc.player);mc.options.hideGui=false;io.github.astelev.around_planet.AroundPlanet.LOGGER.info("PLANET_ATLAS PASS shared cached texture in sky/map and four ship rotations");stage=8;mc.stop();}
    }
    private static void captureGlobe(Minecraft mc){
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(640,480,true,false);
        try{
            target.setClearColor(0,0,0,1);target.clear(false);target.bindWrite(true);
            var p=SpaceHud.current();var camera=mc.gameRenderer.getMainCamera();var eye=camera.getPosition();
            var observer=SpaceEnvironment.observer(new SpaceState(new SpaceVector(p.x(),p.y(),p.z()),SpaceVector.ZERO),p.frame(),new SpaceVector(eye.x,eye.y,eye.z),p.anchor());
            var direction=p.frame().toLocal(observer.scale(-1/observer.length()));
            var up=Math.abs(direction.y())>.95?new org.joml.Vector3f(0,0,1):new org.joml.Vector3f(0,1,0);
            var view=new org.joml.Matrix4f().lookAt(new org.joml.Vector3f(),new org.joml.Vector3f((float)direction.x(),(float)direction.y(),(float)direction.z()),up);
            var projection=new org.joml.Matrix4f().perspective((float)Math.toRadians(70),640f/480,.05f,1000);
            SpaceSkyRenderer.render(view,projection,0,camera);
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/space-biome-planet-alpha26.png"));}
            catch(java.io.IOException e){throw new AssertionError(e);}
        }finally{target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
    private static void rotate(Minecraft mc,ServerSubLevel ship,double angle){mc.getSingleplayerServer().execute(()->{try{ship.getLevel().setDayTime(18000);var c=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(ship.getLevel());c.physicsSystem().getPipeline().teleport(ship,new org.joml.Vector3d(ship.logicalPose().position()),new org.joml.Quaterniond().rotationZ(angle));ship.updateBoundingBox();ship.updateLastPose();}catch(Throwable e){failure=e;}});}
    private static void look(Minecraft mc,SpaceVector n){mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.getCameraEntity().setYRot((float)Math.toDegrees(Math.atan2(-n.x(),n.z())));mc.getCameraEntity().setXRot((float)-Math.toDegrees(Math.atan2(n.y(),Math.hypot(n.x(),n.z()))));mc.getCameraEntity().yRotO=mc.getCameraEntity().getYRot();mc.getCameraEntity().xRotO=mc.getCameraEntity().getXRot();}
    private static void snap(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),m->{});}
}
