package io.github.astelev.around_planet.gametest;

import io.github.astelev.around_planet.AroundPlanet;
import io.github.astelev.around_planet.portal.PlanetPortals;
import io.github.astelev.around_planet.portal.PortalLayout;
import io.github.astelev.around_planet.topology.EastWestTopology;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

@GameTestHolder(AroundPlanet.MOD_ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=AroundPlanet.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class PortalBackendGameTests {
    @SubscribeEvent public static void register(RegisterGameTestsEvent event){event.register(PortalBackendGameTests.class);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void pairLifecycleAndNativeTransform(GameTestHelper h) {
        var level=h.getLevel();var storage=GlobalPortalStorage.get(level);var border=level.getWorldBorder();
        double nativeMin=border.getMinX(),nativeMax=border.getMaxX();
        var foreign=Portal.ENTITY_TYPE.create(level);foreign.portalTag="test:foreign";foreign.isGlobalPortal=true;
        foreign.setOriginPos(new Vec3(0,200,0));foreign.setDestinationDimension(level.dimension());foreign.setDestination(new Vec3(50,200,0));
        foreign.setOrientationAndSize(new Vec3(1,0,0),new Vec3(0,1,0),4,4);storage.addPortal(foreign);
        var layout=new PortalLayout(EastWestTopology.atBoundary(256),-128,1024,1024);
        boolean rejected=false;
        try {new PortalLayout(layout.topology(),100,100,1024);}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Invalid height interval accepted");
        PlanetPortals.reconcile(level,layout);var pair=PlanetPortals.managed(level);
        h.assertTrue(io.github.astelev.around_planet.portal.PortalChunkPolicy.allow(level,15,0),"Canonical watch subscription rejected");
        h.assertTrue(io.github.astelev.around_planet.portal.PortalChunkPolicy.allow(level,16,0),"Native boundary neighbor halo rejected");
        h.assertTrue(!io.github.astelev.around_planet.portal.PortalChunkPolicy.allow(level,17,0),"Ordinary outside subscriptions retained");
        h.assertTrue(pair.size()==2,"Expected exactly EW pair");
        var east=pair.stream().filter(p->PlanetPortals.EAST.equals(p.portalTag)).findFirst().orElseThrow();
        var west=pair.stream().filter(p->PlanetPortals.WEST.equals(p.portalTag)).findFirst().orElseThrow();
        h.assertTrue(east.transformPoint(new Vec3(256.75,640,101)).equals(new Vec3(-255.25,640,101)),"East transform lost overshoot/altitude/Z");
        h.assertTrue(west.transformPoint(new Vec3(-256.125,640,101)).equals(new Vec3(255.875,640,101)),"West transform lost overshoot");
        h.assertTrue(east.getNormal().x<0 && west.getNormal().x>0,"Wrong portal faces");
        PlanetPortals.reconcile(level,layout);
        h.assertTrue(PlanetPortals.managed(level).getFirst()==pair.getFirst(),"Repeated startup recreated existing portals");
        h.assertTrue(border.getMinX()==nativeMin && border.getMaxX()==nativeMax,"Vanilla border was modified");
        h.runAfterDelay(10,()->{
            try {
                h.assertTrue(!east.isRemoved() && !west.isRemoved(),"Aeronautics tick incorrectly removed global portals");
                var defaults=new PortalLayout(EastWestTopology.atBoundary(65536),-2048,16384,29999984);
                PlanetPortals.reconcile(level,defaults);
                var defaultEast=PlanetPortals.managed(level).stream().filter(p->PlanetPortals.EAST.equals(p.portalTag)).findFirst().orElseThrow();
                h.assertTrue(defaultEast.isPortalValid() && defaultEast.getWidth()==59999968,"Default full-world span invalid");
                h.assertTrue(defaultEast.transformPoint(new Vec3(65536.75,2000,16000000)).equals(new Vec3(-65535.25,2000,16000000)),"Default boundary or altitude transform incorrect");
                PlanetPortals.reconcile(level,new PortalLayout(EastWestTopology.atBoundary(512),-128,2048,1024));
                h.assertTrue(PlanetPortals.managed(level).size()==2 && !PlanetPortals.managed(level).contains(east),"Configuration migration left old/duplicate portals");
                h.assertTrue(storage.data.contains(foreign),"Foreign portal was deleted");
                PlanetPortals.reconcile(level,null);
                h.assertTrue(io.github.astelev.around_planet.portal.PortalChunkPolicy.allow(level,17,0),"Disabled policy still clips subscriptions");
                h.assertTrue(PlanetPortals.managed(level).isEmpty() && storage.data.contains(foreign),"Disable did not preserve foreign portals");
                h.succeed();
            } finally {PlanetPortals.reconcile(level,null);storage.removePortal(foreign);}
        });
    }
}
