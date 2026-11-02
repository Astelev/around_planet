package io.github.astelev.aerospace.compat.dh;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BiomeSurfaceHeightsTest {
    @Test void seaStaysAtBaseAndLandUsesStaticOffsets() {
        assertEquals(0, BiomeSurfaceHeights.offset("minecraft:ocean",false,false));
        assertEquals(0, BiomeSurfaceHeights.offset("minecraft:river",false,false));
        assertTrue(BiomeSurfaceHeights.offset("minecraft:plains",false,false) > 0);
        assertTrue(BiomeSurfaceHeights.offset("minecraft:savanna_plateau",false,false) > BiomeSurfaceHeights.offset("minecraft:plains",false,false));
        assertEquals(112, BiomeSurfaceHeights.offset("custom:peaks",true,false));
        assertEquals(0, BiomeSurfaceHeights.offset("custom:unknown",false,false));
    }
    @Test void heightAndColorShareWorldEpoch() {
        FallbackBiomeColors.clear();
        FallbackBiomeColors.setView("one", 0, 0);
        long epoch = FallbackBiomeColors.generation();
        FallbackBiomeColors.observe(epoch,"one",0,0,123,40);
        assertEquals(40,FallbackBiomeColors.heightOffset(0,0));
        assertEquals(123,FallbackBiomeColors.color(0,0,456));
        FallbackBiomeColors.clear();
        FallbackBiomeColors.setView("one",0,0);
        FallbackBiomeColors.observe(epoch,"one",0,0,789,80);
        assertEquals(0,FallbackBiomeColors.heightOffset(0,0));
        FallbackBiomeColors.clear();
    }
}
