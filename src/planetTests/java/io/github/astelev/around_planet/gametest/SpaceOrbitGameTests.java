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
    private static void reject(GameTestHelper h,CompoundTag tag) {
        try{SpaceOrbitData.load(tag,h.getLevel().registryAccess());throw new AssertionError("Invalid save accepted");}
        catch(IllegalArgumentException expected){}
    }
}
