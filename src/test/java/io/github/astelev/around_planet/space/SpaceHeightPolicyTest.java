package io.github.astelev.around_planet.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceHeightPolicyTest {
    @Test void allOldLegalHeightsUpgradeIncludingPreviouslyMarkedConfigs(){for(int h:new int[]{1400,1536,1600,1700})for(int revision:new int[]{0,1})assertEquals(6144,SpaceHeightPolicy.migrate(h,revision));}
    @Test void highCustomSettingsAreRetained(){for(int h:new int[]{3072,6144,9000,30000})assertEquals(h,SpaceHeightPolicy.migrate(h,1));}
    @Test void currentRevisionIsIdempotent(){assertEquals(6144,SpaceHeightPolicy.migrate(6144,2));assertEquals(9000,SpaceHeightPolicy.migrate(9000,2));}
}
