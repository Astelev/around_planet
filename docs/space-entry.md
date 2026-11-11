# First playable space transfer — 0.11.0-alpha.2

This alpha adds a fixed, nonrotating local frame to high-altitude Overworld
cells. The owner's common translational velocity is stored in a persistent
virtual trajectory; native rotation, bearings and relative body motion remain
active. Orbital reentry, a planet visual and strict per-cell entity/interaction
isolation are subsequent stages.

## Try it

Install the same around_planet JAR on server and client; keep Sable/Aeronautics
and the existing Cruising JAR. No patched library and no new dimension are needed.
In `<world>/serverconfig/around_planet-server.toml`:

```toml
[eastWest]
enabled = true

[space]
enabled = true
entryY = 768
cellY = 1536
```

`space.enabled` defaults to true; the existing top-level experimental activation
`eastWest.enabled` is still required for new arrivals. Restart after changing
settings. Raise an **unanchored** assembly's combined center of mass through
Y=768. Its entire hull must clear ordinary terrain plus a safety margin. A real
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
The physical return destination is the original X/Z, at `entryY-32`, so it does
not immediately trigger another ascent.

## Local physics

The mass-weighted assembly center remains at the cell anchor. Immediately before
each native physics substep, after queued engine forces, its common linear
velocity is transferred into the saved virtual trajectory and subtracted from
bodies in the cell. Native world gravity is compensated locally; angular
velocities are untouched. Free bodies receive the opposite frame impulse.
Ordinary entities also have local gravity suppressed without changing their saved
`NoGravity` flag. Partial owner loading still receives gravity compensation.

This is an **entry-frame linear trajectory** (`mu=0`), not yet a spherical
planetary orbit. There is no automatic geometric return at the edges of a cell;
use the command below until orbital reentry is integrated. Returning restores
virtual linear velocity and applies a five-second reentry cooldown.

While riding or standing on a cell's assembly, use:

```text
/around_planet_space return
```

This queues a return and waits for full destination chunks. Already allocated
cells can return even if new space arrivals are disabled. Complex changes to
assembly membership inside a cell (splitting/docking) are not handled yet; a
partial connected group is not returned independently.

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

- 41 unit tests and 11 native GameTests with Sable/Create/Cruising.
- New native three-axis out/back transfer of two rotary-connected bodies and a
  real Create seat/pig: unchanged joint layout, linear/angular momentum, plot
  seat position and mount; persisted membership and Cruising ticket exclusion.
- Actual integrated client with Sable/Create/Cruising/Sodium/DH/Simple Clouds: ascent triggers
  automatic allocation and transport; same native ship, seat, client object/GPU
  mesh and chest block entity; real FULL client chunks; Cruising height view and
  DH flight mode inactive in the cell. Requested return preserves the passenger
  and ship identity, and Cruising height mode resumes outside. A live-physics
  stage removes the forced native ship ticket and retains the occupied ship
  while real UDP snapshots run. Ten seconds of unpaused physics checks the fixed
  center, virtual thrust impulse, render gates and retained ship callbacks.
- A 500-step native physics test covers two rotary-connected owner bodies, a
  free body and a control body outside space: fixed owner center, unchanged
  controller angular velocities, virtual thrust, opposite free-body impulse,
  and ordinary gravity outside the cell.
- Optional integrations are also checked on a server without Sable/Cruising/DH.

Saved membership is serialized and tested through a fresh data instance. Full
player logout/login while seated in a cell, every Aeronautics addon block,
multiple simultaneously occupied cells and latency-injected UDP transfers have
not been verified in this alpha. Reliable transfer packets rebase logical/render
history and normalize old-coordinate snapshots; this retains the existing seam
approach rather than promising zero visual discontinuity under all latency.
