package io.github.astelev.around_planet.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.astelev.around_planet.network.PlasmaPayload;
import io.github.astelev.around_planet.space.*;
import org.joml.Vector3d;

/** Read-only air-relative velocity. The stationary space-cell anchor is not its orbital velocity. */
public final class PlasmaMotion {
    public static PlasmaPayload.Sample sample(ServerSubLevel ship) {
        var level = ship.getLevel();
        if (ship.isRemoved() || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return null;
        var container = SubLevelContainer.getContainer(level);
        if (!container.physicsSystem().getPhysicsHandle(ship).isValid() || ship.getSelfMassTracker().getCenterOfMass() == null) return null;
        var position = ship.logicalPose().transformPosition(new Vector3d(ship.getSelfMassTracker().getCenterOfMass()));
        var v = container.physicsSystem().getPipeline().getLinearVelocity(ship, new Vector3d());
        var velocity = new SpaceVector(v.x, v.y, v.z); double altitude = position.y;
        var cells = SpaceCells.get(level); var cell = cells.forShip(ship.getUniqueId());
        if (cell == null) for (var candidate : cells.all()) if (candidate.contains(position.x, position.y, position.z)) { cell = candidate; break; }
        if (cell != null) {
            var orbits = SpaceOrbitData.get(level.getServer()); var trajectory = orbits.get(cell.id()); var nav = orbits.navigation(cell.id());
            if (trajectory.isEmpty() || nav.isEmpty() || !CelestialSystem.EARTH.equals(trajectory.get().primary())) return null;
            var state = orbits.sample(cell.id(), level.getGameTime());
            altitude = state.position().length() - nav.get().planet().radius();
            velocity = nav.get().frame().toLocal(state.velocity().subtract(PlanetDaylight.spin(level).velocity(state.position()))).add(velocity);
        }
        double density = SpaceAtmosphere.model().density(altitude);
        if (io.github.astelev.around_planet.plasma.PlasmaMath.intensity(velocity.length(), density) <= .001) return null;
        return new PlasmaPayload.Sample(ship.getUniqueId(), velocity, density);
    }
    private PlasmaMotion() {}
}
