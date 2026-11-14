package io.github.astelev.around_planet.gametest;
import io.github.astelev.around_planet.PlanetConfig;
import io.github.astelev.around_planet.space.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;
@PrefixGameTestTemplate(false)
public final class PlanetRotationGameTests {
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void highCellConfigIsAcceptedAndOldDefaultMigratesOnce(GameTestHelper h){var config=com.electronwill.nightconfig.core.CommentedConfig.inMemory();config.set("space.cellY",9000);io.github.astelev.around_planet.PlanetConfigMigration.migrate(config);PlanetConfig.SPEC.correct(config);h.assertTrue(((Number)config.get("space.cellY")).intValue()==9000,"Spec rejected custom high cell Y");var legacy=com.electronwill.nightconfig.core.CommentedConfig.inMemory();legacy.set("space.cellY",1536);io.github.astelev.around_planet.PlanetConfigMigration.migrate(legacy);h.assertTrue(((Number)legacy.get("space.cellY")).intValue()==6144,"Legacy default did not migrate");legacy.set("space.cellY",7000);io.github.astelev.around_planet.PlanetConfigMigration.migrate(legacy);h.assertTrue(((Number)legacy.get("space.cellY")).intValue()==7000,"Migration overwrote a custom height");
        for(int value:new int[]{1536,1600,1700}){var old=com.electronwill.nightconfig.core.CommentedConfig.inMemory();old.set("space.cellY",value);old.set("space.heightLayoutRevision",1);io.github.astelev.around_planet.PlanetConfigMigration.migrate(old);PlanetConfig.SPEC.correct(old);h.assertTrue(((Number)old.get("space.cellY")).intValue()==6144 && ((Number)old.get("space.heightLayoutRevision")).intValue()==2,"Marked old height did not upgrade: "+value);}
        h.succeed();}
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void localNightBrightnessAndDaylightDetectorsFollowLongitude(GameTestHelper h)throws Exception{
        var level=h.getLevel();long clock=level.getDayTime();boolean enabled=PlanetConfig.ENABLED.getAsBoolean();int size=PlanetConfig.PLANET_SIZE.get();
        try{PlanetConfig.ENABLED.set(true);PlanetConfig.PLANET_SIZE.set(131072);level.setDayTime(6000);level.setWeatherParameters(60000,0,false,false);
            var day=new BlockPos(0,300,0);var night=new BlockPos(65536,300,0);level.getChunkAt(day);level.getChunkAt(night);
            h.assertTrue(level.getMaxLocalRawBrightness(day)==15,"Noon sky light not full");h.assertTrue(level.getMaxLocalRawBrightness(night)==4,"Midnight sky light still uses global noon");
            try(var scope=PlanetDaylight.scope(level,night.getX())){h.assertTrue(level.isNight() && !level.isDay(),"Scoped entity/bed night still global");h.assertTrue(Math.abs(level.getSunAngle(0)-Math.PI)<1e-5,"Detector clock not at local midnight");}
            var update=DaylightDetectorBlock.class.getDeclaredMethod("updateSignalStrength",BlockState.class,Level.class,BlockPos.class);update.setAccessible(true);
            for(var pos:new BlockPos[]{day,night}){level.setBlock(pos,Blocks.DAYLIGHT_DETECTOR.defaultBlockState(),3);update.invoke(null,level.getBlockState(pos),level,pos);}
            h.assertTrue(level.getBlockState(day).getValue(DaylightDetectorBlock.POWER)>0,"Noon detector off");h.assertTrue(level.getBlockState(night).getValue(DaylightDetectorBlock.POWER)==0,"Midnight detector on");
            level.setDayTime(18000);h.assertTrue(level.getMaxLocalRawBrightness(day)==4 && level.getMaxLocalRawBrightness(night)==15,"Local daylight did not swap after half turn");
        }finally{level.setDayTime(clock);PlanetConfig.ENABLED.set(enabled);PlanetConfig.PLANET_SIZE.set(size);}
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="around_planet",timeoutTicks=100)
    public static void parkedReentryFollowsRotatingGroundAcrossSaveWithoutLoadingDestination(GameTestHelper h){
        var planet=new PlanetScale(131072).geometry();var spin=PlanetSpin.at(6000,true);var next=PlanetSpin.at(12000,true);var world=new SpaceVector(50000,2000,12000);var local=new SpaceVector(10,-20,3);var state=spin.launch(planet,world,local);var id=UUID.randomUUID();var data=new SpaceOrbitData();
        data.navigate(id,new SpaceNavigation(planet,spin.frame(planet,world.x(),world.z())));data.put(id,new SpaceTrajectory("planet",10,state,9.81*planet.radius()*planet.radius()));data.reenterRotating(id,state.position(),10,spin);data.impulse(id,10,spin.frame(planet,world.x(),world.z()).toCosmic(new SpaceVector(1,0,2)));
        var loaded=SpaceOrbitData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());loaded.followContacts(next,6010);var moved=loaded.sample(id,6010);
        h.assertTrue(next.surface(planet,moved.position()).subtract(world).length()<1e-7,"Pending contact drifted to another ground point after reload");h.assertTrue(next.landingVelocity(planet,moved.position(),moved.velocity()).subtract(local.add(new SpaceVector(1,0,2))).length()<1e-7,"Pending contact lost ground-relative speed");
        loaded.remove(id);h.assertTrue(loaded.followContacts(spin,6020).isEmpty(),"Removed contact remained parked");h.succeed();
    }
}
