package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.space.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.io.*;
import java.util.UUID;

/** Actual Minecraft SavedData/NBT round trip without loading a Sable ship, cell or destination chunk. */
@PrefixGameTestTemplate(false)
public final class SpaceOrbitGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void dormantTrajectorySurvivesSaveAndDetachedObjectInheritsMotion(GameTestHelper h) throws Exception {
        var data=new SpaceOrbitData();var owner=new UUID(1,1);var child=new UUID(1,2);
        long epoch=h.getLevel().getGameTime();
        var orbit=new SpaceTrajectory("planet",epoch,new SpaceState(new SpaceVector(10000,0,0),new SpaceVector(0,6,0)),398600);
        data.put(owner,orbit);data.setDirty(false);
        long later=epoch+20L*86400*30;
        var expected=data.sample(owner,later);
        h.assertTrue(!data.isDirty() && data.get(owner).orElseThrow().epochTick()==epoch,"Dormant query rewrote the saved orbit");
        var bytes=new ByteArrayOutputStream();NbtIo.write(data.save(new CompoundTag(),h.getLevel().registryAccess()),new DataOutputStream(bytes));
        var tag=NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),NbtAccounter.unlimitedHeap());
        var restored=SpaceOrbitData.load(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.sample(owner,later).equals(expected),"Saved trajectory lost phase or speed after reload");
        h.assertTrue(!restored.isDirty(),"Loading orbit data dirtied it");
        var local=new SpaceState(new SpaceVector(10,20,30),new SpaceVector(1,2,3));
        var detached=restored.detach(owner,child,later,SpaceFrame.IDENTITY,local);
        h.assertTrue(detached.initial().position().equals(expected.position().add(local.position()))
                && detached.initial().velocity().equals(expected.velocity().add(local.velocity())),"Detached record lost origin motion");
        h.assertTrue(restored.ids().size()==2 && restored.get(owner).orElseThrow().equals(orbit),"Detaching replaced the owner trajectory");
        restored.impulse(owner,later,new SpaceVector(10,0,0));
        h.assertTrue(restored.get(child).orElseThrow().equals(detached),"Later owner thrust changed the independent dormant object");
        try {restored.detach(owner,child,later,SpaceFrame.IDENTITY,local);throw new AssertionError("Existing orbit silently overwritten");}
        catch(IllegalArgumentException expectedFailure){}
        // Factory is also exercised, not just the detached test instance. No world ticks are registered.
        var persisted=SpaceOrbitData.get(h.getLevel().getServer());var probe=UUID.randomUUID();
        try {
            persisted.put(probe,orbit);
            h.assertTrue(SpaceOrbitData.get(h.getLevel().getServer()).get(probe).orElseThrow().equals(orbit),"SavedData factory did not retain records");
            h.getLevel().getServer().overworld().getDataStorage().save();
            net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
            var path=h.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").resolve(SpaceOrbitData.STORAGE_ID+".dat");
            var disk=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());
            var fromDisk=SpaceOrbitData.load(disk.getCompound("data"),h.getLevel().registryAccess());
            h.assertTrue(fromDisk.sample(probe,later).equals(expected),"World SavedData file lost the unloaded orbit");
        }
        finally {persisted.remove(probe);}
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void corruptOrFutureOrbitDataDoesNotSilentlyDiscardObjects(GameTestHelper h) {
        var corrupt=new CompoundTag();corrupt.putInt("schema",2);corrupt.put("objects",new net.minecraft.nbt.ListTag());
        reject(h,corrupt);
        corrupt.putInt("schema",1);var list=new net.minecraft.nbt.ListTag();list.add(net.minecraft.nbt.IntTag.valueOf(5));corrupt.put("objects",list);reject(h,corrupt);
        var good=new SpaceOrbitData();good.put(new UUID(1,2),new SpaceTrajectory("planet",0,new SpaceState(new SpaceVector(100,0,0),SpaceVector.ZERO),0));
        var tag=good.save(new CompoundTag(),h.getLevel().registryAccess());tag.getList("objects",10).getCompound(0).remove("vx");reject(h,tag);
        tag=good.save(new CompoundTag(),h.getLevel().registryAccess());var entries=tag.getList("objects",10);entries.add(entries.getCompound(0).copy());reject(h,tag);
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void pendingReentryKeepsNavigationMomentumAndContactAcrossSave(GameTestHelper h) {
        var data=new SpaceOrbitData();var id=UUID.randomUUID();var planet=new PlanetGeometry(65536,65536);var nav=new SpaceNavigation(planet,planet.frame(120,45));long now=h.getLevel().getGameTime();
        var contact=planet.position(340,736,70);var velocity=planet.frame(340,70).toCosmic(new SpaceVector(30,-20,5));
        data.put(id,new SpaceTrajectory("around_planet:overworld",now,new SpaceState(planet.position(340,737,70),velocity),1));data.navigate(id,nav);data.reenter(id,contact,now);
        data.impulse(id,now+100,new SpaceVector(1,2,3));var state=data.sample(id,now+10000);
        h.assertTrue(state.position().equals(contact) && state.velocity().equals(velocity.add(new SpaceVector(1,2,3))),"Waiting for chunks moved the contact or discarded engine momentum");
        var restored=SpaceOrbitData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.navigation(id).orElseThrow().equals(nav) && restored.sample(id,now+10000).equals(state) && restored.reentry(id).orElseThrow().equals(contact),"Pending reentry did not survive saving");
        restored.remove(id);h.assertTrue(restored.navigation(id).isEmpty() && restored.reentry(id).isEmpty(),"Returned cell left stale navigation");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void singlePlanetSizeMigratesLegacyBordersAndRespectsExplicitSetting(GameTestHelper h) {
        var c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("eastWest.boundaryX",65536);c.set("eastWest.boundaryZ",32768);new io.github.astelev.around_planet.PlanetConfigMigration().correct(c);
        h.assertTrue(((Number)c.get("planet.circumferenceBlocks")).intValue()==131072 && !c.contains("eastWest.boundaryX") && !c.contains("eastWest.boundaryZ"),"Legacy borders were not migrated to one circumference");
        c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("eastWest.boundaryX",65536);c.set("planet.circumferenceBlocks",262144);new io.github.astelev.around_planet.PlanetConfigMigration().correct(c);h.assertTrue(((Number)c.get("planet.circumferenceBlocks")).intValue()==262144,"Legacy field overrode explicitly selected planet size");
        c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("eastWest.boundaryX",256);new io.github.astelev.around_planet.PlanetConfigMigration().correct(c);h.assertTrue(((Number)c.get("planet.circumferenceBlocks")).intValue()==512,"Custom test-world size was not preserved");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void oldSpaceDefaultMovesAboveAeroworksButCustomSettingsRemain(GameTestHelper h) {
        var c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("space.entryY",768);
        new io.github.astelev.around_planet.PlanetConfigMigration().correct(c);
        h.assertTrue(((Number)c.get("space.entryY")).intValue()==2208 && ((Number)c.get("space.atmosphereTop")).intValue()==4000,"Old default was not upgraded with atmosphere settings");
        c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("space.entryY",3000);new io.github.astelev.around_planet.PlanetConfigMigration().correct(c);h.assertTrue(((Number)c.get("space.entryY")).intValue()==2208,"Alpha.8 default was not lowered");
        c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("space.entryY",900);
        new io.github.astelev.around_planet.PlanetConfigMigration().correct(c);
        h.assertTrue(((Number)c.get("space.entryY")).intValue()==900,"Custom entry threshold was overwritten");
        c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("space.entryY",768);c.set("space.atmosphereTop",5000);
        new io.github.astelev.around_planet.PlanetConfigMigration().correct(c);
        h.assertTrue(((Number)c.get("space.entryY")).intValue()==768,"Already migrated custom setting changed on reload");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void lateAtmosphereProfileMigratesDefaultsOnceAndRetainsCustomValues(GameTestHelper h){
        var c=com.electronwill.nightconfig.core.CommentedConfig.inMemory();c.set("space.atmosphereTop",6000);c.set("space.atmosphereDrag",.02);c.set("space.reentryMaxSpeed",20.0);
        var migration=new io.github.astelev.around_planet.PlanetConfigMigration();migration.correct(c);
        h.assertTrue(((Number)c.get("space.atmosphereTop")).intValue()==4000 && ((Number)c.get("space.atmosphereBottom")).intValue()==2000 && ((Number)c.get("space.reentryMaxSpeed")).doubleValue()==50 && Math.abs(((Number)c.get("space.atmosphereDrag")).doubleValue()-.012)<1e-10,"Shipped atmospheric defaults did not migrate");
        c.set("space.atmosphereTop",6000);c.set("space.atmosphereDrag",.02);c.set("space.reentryMaxSpeed",20.0);migration.correct(c);
        h.assertTrue(((Number)c.get("space.atmosphereTop")).intValue()==6000 && ((Number)c.get("space.reentryMaxSpeed")).doubleValue()==20,"Profile migration was reapplied to explicit later settings");h.succeed();
    }
    private static void reject(GameTestHelper h,CompoundTag tag) {
        try{SpaceOrbitData.load(tag,h.getLevel().registryAccess());throw new AssertionError("Invalid save accepted");}
        catch(IllegalArgumentException expected){}
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void maneuverPlansPersistWithoutApplyingImpulses(net.minecraft.gametest.framework.GameTestHelper h){
        var data=new io.github.astelev.around_planet.space.SpaceOrbitData();var id=java.util.UUID.randomUUID();var state=new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(10000,0,0),new io.github.astelev.around_planet.space.SpaceVector(0,100,0));var orbit=new io.github.astelev.around_planet.space.SpaceTrajectory("planet",100,state,100000000);data.put(id,orbit);
        var node=new io.github.astelev.around_planet.space.ManeuverNode(java.util.UUID.randomUUID(),200,20,-5,3);data.maneuver(id,node);h.assertTrue(data.sample(id,200).equals(orbit.at(200)),"Planning applied a real impulse");
        var tag=data.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());var loaded=io.github.astelev.around_planet.space.SpaceOrbitData.load(tag,h.getLevel().registryAccess());h.assertTrue(loaded.maneuvers(id).equals(java.util.List.of(node)),"Maneuver lost on save/load");
        loaded.maneuver(id,new io.github.astelev.around_planet.space.ManeuverNode(node.id(),250,10,0,0));h.assertTrue(loaded.maneuvers(id).size()==1&&loaded.maneuvers(id).getFirst().tick()==250,"Edit duplicated node");loaded.removeManeuver(id,node.id());h.assertTrue(loaded.maneuvers(id).isEmpty(),"Node did not delete");data.remove(id);h.assertTrue(data.maneuvers(id).isEmpty(),"Orphan maneuvers retained after orbit removal");h.succeed();
    }

    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void rendezvousTargetPersistsAndRetiringTargetClearsSelection(net.minecraft.gametest.framework.GameTestHelper h){var data=new io.github.astelev.around_planet.space.SpaceOrbitData();var a=java.util.UUID.randomUUID();var b=java.util.UUID.randomUUID();var path=new io.github.astelev.around_planet.space.SpaceTrajectory("test",0,new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(30000,0,0),new io.github.astelev.around_planet.space.SpaceVector(0,300,0)),4e9);data.put(a,path);data.put(b,path);data.target(a,b);var copy=io.github.astelev.around_planet.space.SpaceOrbitData.load(data.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.target(a).orElseThrow().equals(b),"Target selection did not persist");copy.remove(b);h.assertTrue(copy.target(a).isEmpty(),"Retired object remained selected");boolean rejected=false;try{data.target(a,a);}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected,"Self-target accepted");h.succeed();}
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="around_planet",batch="orbitalWarp",timeoutTicks=100)
    public static void orbitalWarpAdvancesEveryRecordAndPreservesManeuverAndTargetIdentity(net.minecraft.gametest.framework.GameTestHelper h){var data=new io.github.astelev.around_planet.space.SpaceOrbitData();var a=java.util.UUID.randomUUID();var b=java.util.UUID.randomUUID();long now=10000;var orbit=new io.github.astelev.around_planet.space.SpaceTrajectory("p",now,new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(30000,0,0),new io.github.astelev.around_planet.space.SpaceVector(0,300,0)),4e9);var line=new io.github.astelev.around_planet.space.SpaceTrajectory("p",now,new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(50000,0,0),new io.github.astelev.around_planet.space.SpaceVector(1,2,3)),0);data.put(a,orbit);data.put(b,line);var node=new io.github.astelev.around_planet.space.ManeuverNode(java.util.UUID.randomUUID(),now+10000,10,2,3);data.maneuver(a,node);data.target(a,b);data.warp(now,2400);h.assertTrue(data.sample(a,now).position().subtract(orbit.at(now+2400).position()).length()<1e-8&&data.sample(a,now).velocity().subtract(orbit.at(now+2400).velocity()).length()<1e-8,"Orbit was not advanced analytically");h.assertTrue(data.sample(b,now).equals(line.at(now+2400)),"Unloaded/linear orbit was skipped");var copy=io.github.astelev.around_planet.space.SpaceOrbitData.load(data.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.target(a).orElseThrow().equals(b)&&copy.maneuvers(a).getFirst().id().equals(node.id())&&copy.maneuvers(a).getFirst().tick()==node.tick()-2400,"Warp lost saved target or maneuver countdown");h.succeed();}
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="around_planet",batch="orbitalWarp",timeoutTicks=100)
    public static void timeWarpCommandAdvancesDaylightWithoutSkippingMinecraftTicks(net.minecraft.gametest.framework.GameTestHelper h){var level=h.getLevel();var server=level.getServer();var data=io.github.astelev.around_planet.space.SpaceOrbitData.get(server);var id=java.util.UUID.randomUUID();long before=level.getGameTime(),day=level.getDayTime();var orbit=new io.github.astelev.around_planet.space.SpaceTrajectory("warp-command",before,new io.github.astelev.around_planet.space.SpaceState(new io.github.astelev.around_planet.space.SpaceVector(100000,0,0),new io.github.astelev.around_planet.space.SpaceVector(1,2,3)),0);try{data.put(id,orbit);var root=server.getCommands().getDispatcher().getRoot().getChild("time");h.assertTrue(root.getChild("warp")!=null&&root.getChild("query")!=null&&root.getChild("set")!=null,"Command replaced vanilla time commands");server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(2).withSuppressedOutput(),"time warp 120");h.assertTrue(level.getGameTime()==before&&level.getDayTime()==day+2400,"Warp changed gameTime or failed to advance daylight");h.assertTrue(data.sample(id,before).equals(orbit.at(before+2400)),"Command did not advance saved orbit");h.succeed();}finally{data.remove(id);level.setDayTime(day);}}
    @GameTest(template="empty",templateNamespace="around_planet",batch="orbitalWarpLimits",timeoutTicks=100)
    public static void timeWarpStopsBeforeManeuverAndRetainsItsCountdown(GameTestHelper h) {
        var level=h.getLevel();var data=SpaceOrbitData.get(level.getServer());var id=UUID.randomUUID();long now=level.getGameTime(),day=level.getDayTime();
        try {
            data.put(id,new SpaceTrajectory("warp-limit",now,new SpaceState(new SpaceVector(100000,0,0),new SpaceVector(1,0,0)),0));
            var node=new ManeuverNode(UUID.randomUUID(),now+200,10,0,0);data.maneuver(id,node);
            var result=SpaceTimeWarp.advance(level.getServer(),2400);
            h.assertTrue(result.ticks()==180&&result.limit().equals("around_planet.warp.maneuver"),"Warp skipped a planned maneuver");
            h.assertTrue(data.maneuvers(id).getFirst().tick()==now+20&&level.getDayTime()==day+180,"Countdown and planet rotation diverged");
            var stopped=SpaceTimeWarp.advance(level.getServer(),2400);
            h.assertTrue(stopped.ticks()==0&&level.getDayTime()==day+180,"Repeated warp bypassed the maneuver guard");
            h.succeed();
        } finally {data.remove(id);level.setDayTime(day);}
    }
    @GameTest(template="empty",templateNamespace="around_planet",batch="maneuverMetadata",timeoutTicks=100)
    public static void individualWarningSurvivesSaveWarpAndLegacyMigration(GameTestHelper h){
        var data=new SpaceOrbitData();var id=UUID.randomUUID();data.put(id,new SpaceTrajectory("test",0,new SpaceState(new SpaceVector(100000,0,0),SpaceVector.ZERO),0));
        var first=new ManeuverNode(UUID.randomUUID(),10000,10,0,0,7);var second=new ManeuverNode(UUID.randomUUID(),20000,0,2,0,90);data.maneuver(id,first);data.maneuver(id,second);
        var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());var copy=SpaceOrbitData.load(saved,h.getLevel().registryAccess());copy.warp(100,200);
        h.assertTrue(copy.maneuvers(id).getFirst().warningSeconds()==7&&copy.maneuvers(id).getLast().warningSeconds()==90&&copy.maneuvers(id).getFirst().tick()==9800,"Warp/save lost individual warnings");
        saved.getList("objects",10).getCompound(0).getList("maneuvers",10).getCompound(0).remove("warningSeconds");copy=SpaceOrbitData.load(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.maneuvers(id).getFirst().warningSeconds(30)==30,"Legacy node lost planner fallback");h.succeed();
    }
}
