# Space sky and unload capture — 0.11.0-alpha.7

## Inertial sky

The in-world space sky now renders deterministic stars, a Sun disk with a halo,
and a round planet with simple procedural land/ocean colors. One fixed cosmic
Sun direction controls both the disk and the planet day/night shading. Vanilla
day time does not rotate this sky. Native block/entity lightmaps are unchanged.
The map shares the same lighting direction.

The observer is computed from the saved cosmic trajectory and the physical camera
offset within the occupied cell. The retained entry basis maps cosmic directions
to the cell, so changing the allocated cell address does not change apparent
planet size or sky direction. Fractional render time uses the same Kepler solver
as telemetry. Planet radius comes from the single circumference setting.

Stars, Sun and sphere use three cached GPU meshes; rendering does not request
terrain, textures from the generator, chunks or DH data. The planet is opaque and
occludes stars behind it. Native ships render subsequently in front of the sky.
Existing ground, DH and cloud suppression in space remains in effect.

## Fast child objects that unload early

In `<world>/serverconfig/around_planet-server.toml`:

```toml
[space]
unloadDetachSpeed = 1.0
```

This is a speed threshold in blocks/meters per second, strictly exceeded by the
free connected group's mass-weighted mean **local** linear velocity. Orbital
speed and pure rotation do not count. Slow objects stay in the parent's frame;
the existing 192-block loaded detachment remains available independently.

The hook runs immediately before Sable serializes a native unload, including its
single-body and chunk-unload paths. Ordinary saves do not trigger promotion.
The owner, anchored groups and bodies constrained to the owner are excluded.
A connected free assembly gets one independent saved trajectory, using the parent
cosmic state plus its local position/velocity transformed through the entry basis.
It keeps native IDs, holding storage, internal linear and angular velocities.
No new active cell, terrain ticket or physics body is created for this capture.

On native observation, the saved internal velocities are restored once. Partial
groups wait in place; a complete coherent group can acquire an asynchronously
prepared cell. Its existing orbit is retained, avoiding double-counting the
captured common velocity. Pending bodies are excluded from parent maneuvers and
ordinary ascent/distance-promotion paths. Dormant catalog entries appear on M.
Loaded bindings are indexed by ship; inactive contact checks run once per second.
Coasting still evaluates a saved Kepler trajectory on demand, rather than
integrating dormant native physics every tick.

This does **not** yet relocate an unloaded holding record to a surface chunk.
An inbound dormant orbit latches contact until the native assembly becomes
available, then proceeds through normal loaded reentry. See the
[unloaded reentry audit](planet-scale-and-objects.md#current-unloaded-reentry-behavior-alpha6-audit).

## Validation

- 71 unit tests, including shared illumination, fixed global Sun, apparent planet
  size, camera offsets and fractional Kepler evaluation.
- 19 native server GameTests with Sable/Create/Cruising. The new test actually
  unloads and reloads a fast free body before 192 blocks, checks slow/owner
  exclusion, persistent bindings, no eager cell allocation, unchanged orbital
  velocity and restored native angular velocity. Existing rotary-group and seat
  transfer/reentry regressions remain included.
- 8 server GameTests without optional mods.
- Integrated client with Sodium/DH/Simple Clouds: actual ascent, fixed-frame
  physics, day/night planet screenshot, Sun screenshots across a 12000-tick
  day-time change, independent-object catalog/map and whole-cell seated reentry.
