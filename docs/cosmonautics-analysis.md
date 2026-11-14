# Cosmonautics 26.08.307: inspected components

Inspected the user-supplied `cosmonautics-26.08.307.jar` using its metadata and
`javap -p -c`. This is static analysis, not a claim that the full mod was run or
that every advertised dependency works with our pack.

## Contents relevant to this project

- Mod IDs `rocketnautics` and `dimensional_sable`; the latter is version 1.0.5.
- Source URL in metadata: https://github.com/CosmonauticsTeam/Create-Cosmonautics
- Declared license: GPL v3.
- Shaded Orekit **13.1.4** and Hipparchus **4.0.3**, as recorded in their Maven
  `pom.properties` files. Orekit's Kepler orbit/time/frame machinery is already
  used, not merely included as an unused dependency.
- `DeepSpaceData`: persistent manager, shared deep-space dimension, allocated
  instances and universe time. `DeepSpaceInstance`: bounded physical regions,
  orbital state, pending physics and references to unloaded Sable storage.
- `DeepSpacePosition`: position/velocity plus an orbit, attractor/frame selection,
  time-dependent orbital evaluation and propagation between frames.

`DeepSpaceData.tick` increments universe time, loops over its instances and marks
SavedData dirty. `DeepSpaceInstance.tick` combines pending velocity changes using
mass weights, updates its orbital state and calls propagation. This differs from
our intended owner-anchored regions and dormant records with no per-object tick.

## Actual cross-dimension transfer

`SpaceTransitionHandler` calls `dev.egg.SubLevelWarper.WarpSubLevel`.
The warper obtains the connected chain, saves each plot with `SubLevelTemplate`,
allocates replacement sublevels in the destination container, maps old UUIDs and
plot offsets to their new equivalents, loads blocks/block entities, teleports
entities and finally removes the original sublevels.

`BlockEntityRegistry` contains explicit fixers for block positions, UUIDs and
connections, including bearings, pistons, rope/spring-related blocks and other
Create integrations. This is a useful catalogue of references that a serializer
must repair; copying block states alone would be insufficient.

Limits visible in this code path:

- Sublevel UUIDs change. Persistent orbital identity must not simply be an
  untracked native ship UUID if this path were used.
- Entity teleport calls `unRide`; this path does not establish preservation of
  our seat/passenger trees.
- Create kinematic contraptions are explicitly disassembled during capture.
- The entity-search AABB is constructed from a ship's dimensions around the
  supplied group origin. `inflate(1)` is called without using its returned AABB.
  These details need tests for distant sublevels and outboard passengers.
- The warper itself does not read/restore native linear and angular velocities;
  caller-side handling must be examined before assuming momentum preservation.

These are inspection findings, not independently reproduced gameplay bugs. The
JAR also includes classes under older Sable constraint package names; installing
it as a dependency needs a real compatibility test, not just its declared
`[2.0.3,)` version range.

## Decision after the design update

Space regions will live **high above the Overworld**, not in another dimension.
A same-level coordinate rebase can retain plots, native UUIDs, joint handles and
seat trees. Consequently the Dimensional Sable transfer path is not required for
the current design. Cosmonautics remains an architectural reference; its source,
mod classes and shaded libraries are not copied into our production artifact.

## Independent mathematical check

`tools/OrbitOracle.java` compares our universal-variable two-body propagator to
Orekit's `CartesianOrbit.shiftedBy` from the uploaded JAR. In 600 initial states
with forward/backward evaluation at 30 and 100,000 seconds (2,400 comparisons),
maximum errors were about **0.000124 blocks** and **6.65e-9 blocks/second**.
The sample includes bound and escape trajectories. This is numerical comparison,
not validation of the Cosmonautics gameplay code.

To reproduce after compiling main classes, with Java 21 and the provided JAR:

```sh
mkdir -p build/orbit-oracle/classes build/orbit-oracle/empty
javac -proc:none -processorpath build/orbit-oracle/empty \
  -d build/orbit-oracle/classes \
  -cp "build/classes/java/main:$COSMONAUTICS_JAR" tools/OrbitOracle.java
java -cp "build/orbit-oracle/classes:build/classes/java/main:$COSMONAUTICS_JAR" \
  io.github.astelev.around_planet.space.OrbitOracle
```

The explicit empty processor path avoids the supplied minimized JAR's dangling
compiler-plugin service entry. The oracle is optional tooling, not a Gradle or
runtime dependency, and none of its external classes are distributed with us.
