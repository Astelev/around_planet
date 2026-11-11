# Orbital motion foundation: first implementation stage (0.11.0-dev)

The original 0.11.0-dev stage added the mathematical and persistent data layer.
The branch now includes the first [playable cell transport](space-entry.md) in
0.11.0-alpha.3. Common engine velocity enters a persistent planet-centered
trajectory while the owner center is held locally. A captured surface basis maps
native forces to that trajectory; automatic reentry rotates and returns all
loaded bodies in the cell. A navigation HUD displays server samples. Dormant
native unload/activation hooks and planetary rendering remain subsequent integrations.

## Agreed world arrangement

All cells remain in the Overworld, in a dedicated high-altitude band. The real
Minecraft position of a cell is a storage/simulation address, independent of its
virtual cosmic position. A cell is allocated/kept active only for actual native
simulation observers, including eligible player/chunk-loader activity. Dormant
objects need an orbital record, not a loaded region or a separate dimension.

Space occupies empty *vertical space*, not replacement empty saved columns.
A Minecraft chunk also contains the terrain below the same X/Z. Persisting an
empty replacement would erase that terrain. Cruising's existing temporary-air
facades are relevant here: `TemporaryLevelChunk` never enters the normal
save/lighting pipeline, and `HighAltitudePolicy` suppresses Sable terrain tickets
only when the whole hull clears the configured threshold and collision margin.
Cell loading and Cruising compatibility are implemented; strict per-cell isolation remains to be added.

Choose the physical altitude/bounds with native coordinate packing in mind:
`BlockPos.asLong` has a signed 12-bit Y field. Arbitrarily huge physical Y values
can alias block positions. Virtual coordinates can be large doubles, while
physical cell coordinates should remain in a tested local range. Ordinary
terrain, north/south border behavior and Cruising's normal flight mode must not
be globally replaced with space behavior.

A fixed local basis is used initially. The ship rotates inside its cell through
Sable; the entire cell does not rotate with every bearing/attitude change. This
avoids introducing unimplemented Coriolis/centrifugal terms.

## Implemented data and motion

- `SpaceTrajectory`: initial position/velocity, primary ID, gravitational
  parameter mu and epoch in simulation game ticks. Units are blocks and seconds,
  with 20 simulation ticks per second.
- With mu=0 it evaluates straight inertial motion. With mu>0 it evaluates a
  point-mass two-body trajectory through universal Kepler variables: elliptic,
  parabolic, hyperbolic and radial arcs before collision.
- Querying state solves an equation for the requested time; it does not integrate
  every elapsed tick or mutate the stored epoch. There are still small numerical
  calculations **on demand**, not literally zero calculations forever.
- A maneuver evaluates the old trajectory at the event time and creates a new
  position/velocity epoch with delta-v applied once.
- `SpaceFrame` converts local vectors/states through a proper orthonormal basis.
  An unattached object receives the opposite local delta-v when the owner's
  non-gravitational motion changes. Common orbital free fall is already part of
  the coast trajectory; it must not be counted as a second thrust impulse.
- Detachment starts an independent trajectory from
  `r_child = r_frame + R*r_local`, `v_child = v_frame + R*v_local`. The owner can
  accelerate later without changing the child's stored orbit.
- `SpaceOrbitData` stores independent stable UUID-to-trajectory records in
  Overworld SavedData, `data/around_planet_space_orbits.dat`. It has no tick
  subscriber, chunk requests or Sable references. Queries do not mark it dirty;
  mutations do. The future native adapter must explicitly map stable object IDs
  to ship groups and preserve that mapping across saves.
- Versioned NBT validates missing, duplicate, non-finite and future-format data.
  The accessor refuses to replace an existing unreadable file with an empty
  catalog after vanilla SavedData catches a deserialization exception.

The intended clock is `server.overworld().getGameTime()`, not day time or wall
clock: unloaded constructs continue while the server simulation runs; closing
or pausing that simulation does not advance orbits. Wall-clock offline progress
would be a separate policy, not implicitly enabled.

## Next integrations, kept separate from the foundation

1. Implemented: allocate high Overworld cells, retain owner/connected group
   membership, and move assemblies with same-level transforms. Strict isolation
   between cells remains pending. Native plots and seat trees are retained.
2. Implemented: measure non-gravitational owner motion at Sable physics-step boundaries,
   transfer it to virtual motion, and apply the counter-impulse to free objects. Joint rotors and angular motion remain
   native. A seated/standing passenger attached to the owner is not free debris.
3. At actual native unload, capture free constructions' absolute state and
   stable bindings before releasing their active cell. Region isolation also
   needs tracking/render/interaction filters; large real spacing alone is not a
   complete isolation mechanism.
4. Active-cell inward crossing detection and pending-contact persistence are implemented.
   A future persisted event queue for dormant objects should track the next atmosphere/surface crossing or
   attractor change. A dormant object approaching the planet must produce an
   event even without an observer. Rescheduling needs stale-event cancellation
   after maneuvers. This cannot be replaced by checking all objects every tick.
5. On activation, evaluate the saved trajectory at the current game time and
   restore local simulation. Surface entry maps the cosmic position and velocity
   into a consistent surface chart and prepares all destinations before moving
   the cell's content. Projection/radius/entry-height policy is not defined by
   the mathematical kernel.

A two-body point-mass trajectory is not an atmosphere, terrain collision system,
N-body solver or a license to propagate through a planet's center. The future
surface-event scheduler must intercept those crossings. We do not yet simulate
unobserved engines, onboard production or collisions between dormant objects.

## Validation

38 unit tests (including 11 new space tests) and 9 native server GameTests pass
with Sable/Create/Cruising. Coverage includes circular/eccentric/escape orbits,
near-parabolic/radial arcs, energy/angular momentum, forward/backward evaluation,
long-gap vs incremental queries, maneuver continuity and relative counter-impulses.

Native tests cover NBT binary round trips, the actual Overworld SavedData
factory, writing a record to the world data file and restoring from that file,
independent detachment, unchanged owner epochs and rejection of invalid data.
NeoForge writes SavedData asynchronously; the disk fixture explicitly waits for
its IO worker before reading the file. This is a disk restoration test, not a
claim that an already-unloaded Sable ship has been reactivated in gameplay.

The optional Orekit comparison and inspected transport behavior are documented
in [cosmonautics-analysis.md](cosmonautics-analysis.md).
