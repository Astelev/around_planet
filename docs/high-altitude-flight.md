# High-altitude flight: experimental implementation

Minecraft 1.21.1 / NeoForge 21.1.256. Integration targets Sable 2.0.6,
Create 6.0.10 and Create Aeronautics 1.3.2. SimulatedExtra 0.3-beta is included
in the integration test run, but is not a dependency. AeroWorks and AeroUtils
remain independent. AeroSpace must be installed on the server and clients.

## Behavior

- In the Overworld, the **lowest point of the entire ship** must be strictly
  above Y=356. A taller world additionally requires clearance above its build
  height and Sable's vertical collision prediction margin.
- Sable's own player-inhabited and force-loaded-sublevel decisions determine
  which ships remain active. Their terrain force-load requests are deferred at
  altitude. An unattended ship without a Sable loading ticket retains Sable's
  normal unloading behavior.
- High-altitude passengers stop contributing vanilla terrain generation and
  simulation tickets. Chunk tracking and entity tracking remain active.
  Ground-level players and other chunk loaders retain normal terrain loading.
- NeoForge's synchronous terrain lookup during movement is intercepted only for
  entities aboard eligible ships. It uses a real loaded chunk when available,
  otherwise a marked `TemporaryLevelChunk`.
- The server sends a dimension-specific temporary view to passengers. The client
  supplies marked empty chunks for missing columns in that view. Real loaded
  terrain always wins. These objects never enter real chunk storage or the
  chunk-generation pipeline and reject block writes and save flags.
- Temporary chunks do not represent generated world columns. They are not saved
  to region files, do not emit ordinary terrain `ChunkEvent.Load` notifications,
  and disappear when the view ends, the player moves away, or the level unloads.
  Each temporary cache retains at most 64 objects; avoiding disk persistence does
  not mean using zero RAM.

## Descent

Within 64 blocks of the threshold, descent faster than 1 block/second begins preparing the ship's
terrain footprint plus a one-chunk collision ring. Minecraft's existing ticket
and world-generation machinery performs the actual generation. At most four new
chunk requests and four new nonempty physics section uploads are admitted per
level per game tick; physics substeps cannot multiply those budgets.
Slower descent starts preparation when it reaches the handoff boundary.

The ship is held at its last safe pose if it would cross the threshold before
real terrain and its Sable collision sections are ready. This pauses that ship,
not the server's generation queue. Its saved velocities resume after preparation.
Passengers see “Preparing terrain for descent…” while waiting. On exit, ordinary
player tickets return and vanilla chunk delivery replaces temporary air.
Existing blocks and player edits are never regenerated or replaced with air.

Sable's internal APIs are version-specific: the optional dependency accepts
exactly 2.0.6. Other versions require revalidation. The server config is
`<world>/serverconfig/aerospace-server.toml`, section `highAltitudeFlight`:
`enabled = true`, `minAltitude = 356`. Disabling returns ordinary loading behavior.

## Current limits

- Experimental: automated server tests do not establish high-speed multiplayer
  or graphical-client performance. Test in a separate world before merging into
  an existing modpack.
- Only the Overworld is enabled. Flying without a Sable ship does not activate it.
- A descent footprint including its ring must fit in 256 columns. Larger ships
  are held above the threshold; disabling the feature restores ordinary loading.
- Constraint-connected ships, unusual passenger trackers, custom generators and
  abrupt teleports require manual validation. Teleports and commands that request
  real terrain retain their normal behavior.
- Distant Horizons is not a dependency. Temporary objects avoid the ordinary
  terrain-load notification path, but graphical LOD integration has not been
  tested. DH still needs real terrain data and may generate it independently;
  this mod does not make unseen LODs free or guarantee that DH keeps up.
- Vanilla player view generation resumes as a normal queue on descent. Other
  mods' explicit synchronous terrain requests are not globally intercepted.

## Validation

With JDK 21:

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew -PintegrationRuntime runGameTestServer
```

The opt-in integration runtime obtains pinned Sable, Create and Aeronautics
releases from Modrinth. To include a separately supplied SimulatedExtra JAR:

```sh
./gradlew -PintegrationRuntime -PextraIntegrationMod=/absolute/path/simulatedextra-0.3-beta.jar runGameTestServer
```

JUnit tests cover threshold boundaries, taller dimensions, invalid bounds,
view coordinates, queue deduplication/expiry and per-game-tick budgets.
GameTests check non-persistent temporary air and network serialization.
With Sable installed, additional tests exercise the actual injected request,
passenger eligibility, ordinary requests below the threshold, and rapid descent
over initially unloaded terrain with a non-player force-load ticket.

Manual acceptance checks still needed:

1. Fly a piloted ship rapidly above Y=356 into new terrain; compare tick timings
   against the feature disabled. Check Distant Horizons visuals separately.
2. Descend and verify waiting, terrain replacement, collisions and continued flight.
3. Keep another player below the same X/Z; their terrain and edits must remain real.
4. Rotate a tall ship, change dimensions, disconnect/reconnect and change render
   distance. Check that temporary views do not survive these transitions.
5. Repeat with a Sable force-loaded ship, connected ships, AeroWorks and AeroUtils.
