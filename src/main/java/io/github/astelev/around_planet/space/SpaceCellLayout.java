package io.github.astelev.around_planet.space;
import java.util.Optional;
import java.util.Set;
/** Bounded deterministic allocation inside real canonical columns, never in seam image/guard columns. */
public final class SpaceCellLayout {
    public record Address(int slot,double x,double z){}
    public static Optional<Address> find(Set<Integer> occupied,double west,double east,double north,double south) {
        for(int ring=0;ring<=4;ring++)for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++) {
            if(Math.max(Math.abs(x),Math.abs(z))!=ring)continue;
            int slot=(x+4)*9+z+4;double px=x*1024.0,pz=z*1024.0;
            if(!occupied.contains(slot) && px-256>=west && px+256<=east && pz-256>=north && pz+256<=south)return Optional.of(new Address(slot,px,pz));
        }return Optional.empty();
    }
    private SpaceCellLayout(){}
}
