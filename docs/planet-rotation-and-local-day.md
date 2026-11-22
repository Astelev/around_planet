# Planet rotation and local daylight — 0.11.0-alpha.13

One uniform eastward turn takes 24000 game ticks (1200 seconds at 20 TPS).
At global day time 6000, longitude X=0 faces the fixed cosmic Sun. Positive X
is east, so local time is ahead there and the daylight band moves west.
The east/west seam has identical solar phase. Latitude retains the existing
square equirectangular chart and solid north/south borders; topology is unchanged.
The Sun lies in the equatorial plane, with no seasons or axial tilt.

## Surface

Local solar phase is `(dayTime / 24000 - 1/4 + X / circumference) mod 1`.
The vanilla eased celestial angle is replaced by a uniform phase so that surface
sunlight and the inertial space Sun agree exactly. The client camera supplies
longitude for sky, fog, lightmap, clocks and ordinary celestial rendering APIs.
The shared surface sky derives its Sun and stars from the same rotated tangent
frame used by navigation, including latitude.

Server brightness queries use their supplied block position. Living-entity ticks
scope day/night queries to the entity, including ordinary sun-burning behavior.
Daylight detectors and beds have position scopes too. Sable plot-local addresses
are resolved through the native pose before choosing geographic longitude. Sleeping advances to local
dawn for the sleepers and retains NeoForge's sleep-finished event. Global raw day
counters and positionless server APIs outside a scope retain the prime-meridian
clock. Mods that read raw dayTime directly, custom AI schedules and independent
render pipelines may require their own position adapter. Weather remains global.
No chunk lighting arrays are rewritten and no extra terrain generation is needed.

`doDaylightCycle=false` freezes phase and removes the spin velocity; `/time` and
sleep move planetary phase together with the world clock. Those vanilla clock
jumps are not elapsed physics time: existing Kepler trajectories still use game
ticks. This is intentional, rather than injecting a maneuver into every orbit.

## Navigation and atmosphere

Planet geometry remains a body-fixed chart. `PlanetSpin` rotates that chart into
the inertial cosmic frame; its inverse maps an inbound point to current ground
longitude. Saved navigation frames remain inertial after launch. Existing saved
space trajectories keep their positions, velocities and frame orientation; they
are not retroactively given a new launch boost.

On ascent, cosmic linear velocity is rotated local velocity plus `omega × r`.
On descent the local tangent basis uses current planetary phase and subtracts
that same surface velocity. The atmosphere co-rotates: drag and the 50 blocks/s
handoff limit operate on velocity **relative to the air/ground**, not inertial
orbital speed. Native internal angular motion, constraints and seat identity
retain their existing behavior; this change does not add planetary angular spin
to native bodies or model atmospheric lift/Coriolis forces in the flat surface.

A contact waiting for asynchronous destination chunks stores body-fixed ground
coordinates and ground-relative velocity. It follows rotation until the complete
cell can return. These fields survive saves; thrust impulses while waiting are
retained. Old pending contacts acquire the current phase on first refresh. Free
bodies receive the opposite velocity correction when the waiting frame changes.
No pending contact forces an unloaded Sable body or destination to activate.
Unloaded ordinary coasts are still evaluated analytically on demand.

## Visuals and speed comparison

The cached procedural planet mesh stores unlit geography. Its shader rotates
positions/normals and lights them against the inertial Sun, so continents move
under a stationary terminator. Stars and Sun stay fixed. The orbit map's checker
sphere uses the same phase, with a bounded 384-pixel image and quantized cache
updates; trajectories and global coordinate axes stay inertial.

For the default circumference 131072 and surface gravity 9.81:

| Location | Co-rotation speed at equator | Circular speed | Ratio |
| --- | ---: | ---: | ---: |
| Surface | 109.23 blocks/s | 452.38 blocks/s | 4.14 |
| Atmosphere top, +4000 | 130.17 blocks/s | 414.39 blocks/s | 3.18 |

The equator is the fastest latitude. Rotation is safely below circular speed in
the normal surface/atmosphere band. Rotation period and configured gravity are
not silently changed for custom planets: `PLANET_SPIN` logs their comparison and
warns if the atmosphere-top ratio drops below three. Zero gravity keeps its
existing inertial mode and has no circular-orbit speed. A claim for arbitrary
altitudes would be incorrect: a hypothetical rigid co-rotating frame eventually
exceeds circular speed far away, but actual spacecraft coast inertially there.

## Validation

99 unit tests include spin derivatives, chart round trips, longitude phase,
launch/landing velocity inversion, default speed margin, frozen time, local dawn
and moving-atmosphere drag. Server tests exercise local skylight and real daylight
detectors plus pending-contact persistence and maneuver retention. Existing native
Sable/Create tests use rotating entry fixtures and test bearings, free-body momentum,
seats, dormancy and reentry. Client checks cover the real planet shader spin uniform,
a half-day turn, the orbital map, native seated ascent/return and opposite-longitude
surface night with the client lightmap. Production artifacts exclude test classes.
