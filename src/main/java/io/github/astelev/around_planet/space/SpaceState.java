package io.github.astelev.around_planet.space;

import java.util.Objects;

public record SpaceState(SpaceVector position,SpaceVector velocity) {
    public SpaceState {Objects.requireNonNull(position);Objects.requireNonNull(velocity);}
}
