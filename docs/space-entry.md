# First playable space transfer — 0.11.0-alpha.8

This alpha adds a fixed, nonrotating local frame to high-altitude Overworld
cells. The owner's common translational velocity is stored in a persistent
virtual trajectory; native rotation, bearings and relative body motion remain
active. Automatic spherical reentry and a server-authoritative navigation HUD are now
active. A fixed Sun/star sky and shaded planet are rendered in space. Strict per-cell entity/interaction isolation remains a subsequent stage.

## Try it

Install the same around_planet JAR on server and client; keep Sable/Aeronautics
and Cruising 1.1.5 or later. No patched library and no new dimension are needed.
In `<world>/serverconfig/around_planet-server.toml`:

```toml
[planet]
circumferenceBlocks = 131072

[eastWest]
enabled = true

[space]
enabled = true
entryY = 3000
cellY = 1536
```

`space.enabled` defaults to true; the existing top-level experimental activation
`eastWest.enabled` is still required for new arrivals. Restart after changing
settings. Raise an **unanchored** assembly's combined center of mass through
Y=3000. Its entire hull must clear ordinary terrain plus a safety margin. A real
external/world constraint or rope prevents a partial transfer, as at the X seam.

A destination is prepared asynchronously with ordinary, fully lit chunks. The
transfer waits for readiness instead of synchronously generating terrain in a
physics tick. The original plot, native ship UUIDs, bearings/joints, linear and
angular velocity and attached passengers remain intact. Both standing passengers
and native plot-coordinate seats use the existing seam transport mechanics,
now extended to a common three-axis translation. Other ordinary vehicles retain
the existing rider and movement correction path.

Cells have a 256-block half extent and 1024-block spacing. Up to 81 physical
addresses are considered near world origin, restricted by the current border.
Large assemblies that do not fit are not transferred. Cell membership, return
location and native ship IDs are saved in `data/around_planet_space_cells.dat`.
The physical return destination is the spherical contact projected to world X/Z, at the current `entryY-32`, so it does
not immediately trigger another ascent.

## Local physics

The mass-weighted assembly center remains at the cell anchor. Immediately before
each native physics substep, after queued engine forces, its common linear
velocity is transferred into the saved virtual trajectory and subtracted from
bodies in the cell. Native world gravity is compensated locally; angular
velocities are untouched. Free bodies receive the opposite frame impulse.
Ordinary entities also have local gravity suppressed without changing their saved
`NoGravity` flag. Partial owner loading still receives gravity compensation.

## Planet coordinates and automatic return

The virtual origin is the planet center, independent of Minecraft/F3 cell
addresses. Planet circumference is `planet.circumferenceBlocks`, the single source of
size; radius is that circumference divided by `2*pi`, and both border half extents
are half the circumference. See [size migration and independent objects](planet-scale-and-objects.md). World X maps to longitude and wraps normally.
World Z maps to latitude between the existing solid north/south borders, stopping
just short of the exact poles to keep the basis defined. This is a distorted
surface chart, not a change to north/south border topology.

Entry captures the local east/up/south basis, actual entry altitude, incoming
velocity and planet dimensions. The basis stays fixed while coasting; the ship
still rotates normally inside its native cell. The saved trajectory uses the
existing analytical two-body solver, with `mu=surfaceGravity*radius^2`.
`space.surfaceGravity` defaults to **9.81 blocks/s²**; set it to **0.0** before
entry to retain straight inertial coasting. Existing planet-bound trajectories
keep their captured parameters. A suborbital craft therefore returns without a
continuous thrust to support it, while its physical owner center stays fixed.

The [orbital map](orbital-map.md) opens on M in space, with a white planet disk,
current trajectory, zoom/pan/rotation and return-shell marker. It does not pause
simulation or change the trajectory.

The HUD shows planet-centered XYZ, velocity components, total speed in m/s and
km/h (one block = one meter), and radial altitude. Samples come from the server
four times per second only for players in a space cell. F3 still shows the
physical simulation address. English and Russian labels are included.

An inward crossing at `entryY-32` (Y=736 by default) starts return to the surface
chart location corresponding to the current cosmic position. The whole loaded
cell returns as one transaction: owner and rotary-connected children, independent
free Sable bodies, world entities and native seat passengers. Native plot
coordinates, UUIDs and joint anchors are retained. A proper rigid rotation maps
the fixed entry basis to the destination surface basis; orientations, angular
velocity, offsets and relative velocities rotate together. Every body's absolute
linear velocity is its rotated relative velocity plus the cell's virtual
velocity in the destination surface axes.

Return waits for full, correctly lit destination chunks, including world-rider
columns; it does not synchronously generate terrain inside a physics step.
While waiting, the crossing point is held rather than propagated through the
planet. Velocity and further thrust impulses remain saved, and the HUD indicates
preparation. Navigation basis and pending contact survive saving. An externally
anchored/incomplete group, oversized footprint or border obstruction postpones
the whole transaction; no partial assembly is moved. A five-second cooldown
prevents immediate reentry into space after return.

Alpha.2 entry-relative trajectories migrate once into planet-centered
coordinates without discarding their saved displacement or velocity. Upgrading
is supported; downgrading these navigation records is not.

While riding or standing on a cell's assembly, use:

```text
/around_planet_space return
```

This requests an immediate return at the current projected planet location and
waits for full destination chunks. Already allocated
cells can return even if new space arrivals are disabled. Loaded owner membership is reconciled after native connection changes. Free
groups can depart into independent frames; a partially loaded connected group
is never returned independently. Full dormant lifecycle/rendezvous handling
remains subsequent work.

## Cruising compatibility

Optional mixins inside around_planet change behavior **only for registered ships/attached passengers or entities
inside registered cell bounds**; no Cruising JAR replacement is required. The tested version is
Cruising 1.1.4-diagnostic.

- Player/ordinary entity height-flight classification returns false in a cell.
- Sable assembly terrain-ticket classification uses the normal terrain path.
- The client stops substituting temporary air, even if an old temporary-view
  packet is still queued during the crossing.
- Cruising's altitude-driven DH adaptation and fallback surface drawing are
  disabled there. Normal behavior resumes on return.

Chunk columns underneath still contain ordinary terrain. They are actually
loaded and sent, preserving native lighting/block-entity behavior; no saved
column is replaced with empty data. Ship blocks stay in their original Sable
plots, not in newly created sections above Minecraft's normal build height.
Outside registered space cells, gravity and ordinary rendering retain their
normal paths.

## Space rendering

While the camera/player is in a registered cell, ordinary terrain, DH LOD passes,
vanilla sky/clouds/weather, Simple Clouds render/compute passes and retained seam
meshes are skipped. The backdrop is black for now; the round planet is a later
stage. Sable terrain-layer callbacks and ship block entities remain rendered.
These are render gates, not empty chunk replacements: actual chunks, lighting
and block entities remain loaded for mod behavior. Returning resumes the normal
render paths. This does not eliminate terrain streaming or every mod's background
CPU task, and does not yet provide a separate view for each cell.

## Validation

- 66 unit tests and 18 native physics GameTests with Sable/Create/Cruising.
- New native three-axis out/back transfer of two rotary-connected bodies and a
  real Create seat/pig: unchanged joint layout, linear/angular momentum, plot
  seat position and mount; persisted membership and Cruising ticket exclusion.
- Actual integrated client with Sable/Create/Cruising/Sodium/DH/Simple Clouds: ascent triggers
  automatic allocation and transport; same native ship, seat, client object/GPU
  mesh and chest block entity; real FULL client chunks; Cruising height view and
  DH flight mode inactive in the cell. Automatic spherical return preserves the passenger
  and ship identity, includes an independent free construction, and Cruising height mode resumes outside. A live-physics
  stage removes the forced native ship ticket and retains the occupied ship
  while real UDP snapshots run. Ten seconds of unpaused physics checks the fixed
  center, virtual thrust impulse, render gates and retained ship callbacks.
- A 500-step native physics test covers two rotary-connected owner bodies, a
  free body and a control body outside space: fixed owner center, unchanged
  controller angular velocities, virtual thrust, opposite free-body impulse,
  and ordinary gravity outside the cell.
- A native rotated reentry test covers an owner with a real rotary joint and an
  independent body: relative layout, absolute linear velocity and angular direction
  survive the chart change, including a whole-shell step followed by a maneuver.
  Pending contact/basis serialization is checked separately.
- Optional integrations are also checked on a server without Sable/Cruising/DH.

Saved membership is serialized and tested through a fresh data instance. Full
player logout/login while seated in a cell, every Aeronautics addon block,
multiple simultaneously occupied cells and latency-injected UDP transfers have
not been verified in this alpha. Reliable transfer packets rebase logical/render
history and normalize old-coordinate snapshots; this retains the existing seam
approach rather than promising zero visual discontinuity under all latency.

See [atmospheric reentry](atmospheric-reentry.md) for the 6000-block inbound shell, progressive drag and safe handoff.
