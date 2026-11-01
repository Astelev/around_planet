# Distant Horizons flight LODs

Optional client integration for **DH 3.3.3 / Minecraft 1.21.1**. It activates only
while AeroSpace's server-authorized temporary sky view is active. Sable flight
and ordinary terrain generation do not depend on DH being installed.

## Coarse first, refine after slowing

During fast flight, the nearest LOD cells are at least 16 blocks wide by default.
DH's normal quadtree requests and renders that coarse data; farther, already
coarser levels stay coarse. Native DH data generation, storage, lighting and
mesh building still perform the actual work. No empty terrain is submitted as
generated DH data, and no DH settings are overwritten.

After five seconds below 20 blocks/second, the minimum cell width drops from
16 to 8 blocks. It then drops to 4, 2 and 1 at ten-second intervals. Native
quality settings remain a lower bound on quality: the adapter never asks for
finer LODs than the user normally requested. Resuming fast flight immediately
returns to the coarse floor. Leaving sky mode restores ordinary behavior.

DH's `pauseIfMovingQuickly` gate is bypassed only during this sky view, including
the stationary refinement period. Its generation-plan setting, server-load
protection and queue backpressure remain in force. Use `SURFACE_THEN_CHUNKS` or
`SURFACE_ONLY` to benefit from rough surface generation; `CHUNKS_ONLY` still
requires the chunk generator and `DISABLED` remains disabled.

The client config is `config/aerospace-client.toml`, section
`distantHorizonsFlight`:

| Setting | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Enable this optional integration |
| `coarseDetail` | `4` | Cruise cell width is `2^value` blocks |
| `fastSpeed` | `20.0` | Speed that restarts coarse mode, blocks/second |
| `quietSeconds` | `5` | Delay before the first refinement |
| `refineSeconds` | `10` | Time between later refinements |
| `diagnostics` | `true` | F3 status and a log line at most every 30 seconds |
| `fallbackPlane` | `true` | Fill otherwise empty terrain pixels with a visual plane |
| `fallbackHeight` | `60` | Height of the temporary plane |
| `fallbackColor` | `0x718a54` | Neutral RGB color until an observed biome color is available |
| `fallbackBiomeColors` | `true` | Reuse biome samples already calculated by DH |

## Temporary visual floor

During sky flight, a single fullscreen pass intersects downward camera rays with
Y=60. It draws **only** if both vanilla depth and DH's depth are clear at that
pixel. Real rendered terrain wins at any height, including valleys below Y=60;
the plane cannot paint over ready LODs, ships, entities or ordinary chunks.
Nothing is written to world depth, Minecraft chunks or the DH database. Missing
pixels disappear automatically as actual geometry starts rendering there.

The pass uses DH's inverse view/projection and fades toward the current fog color
at distance. It leaves the sky above the horizon untouched. Standard OpenGL with
forward or reversed depth is supported, including reversed DH with conventional
Minecraft depth. Unknown shader-pack framebuffers and shared vanilla/DH depth
textures are skipped rather than modifying an unsupported render pipeline.

Biome colors are opportunistic, not an additional biome-map generator. The
integration observes sparse points from DH's **existing** rough-surface biome
queries. Water biomes use water color, cold biomes use a snow tint, and other
biomes use their grass color. A bounded 34×34 grid of 256-block cells is uploaded
at most five times a second, apart from moving the grid or resetting the world.
Unknown cells retain the neutral color. No chunk requests, density calculations,
DB reads, server jobs, or extra biome queries are added to obtain colors.
The palette clears on flight/world exit and dimension change.

## Why generation may stop while stationary

Inspection of the supplied DH JAR found no requirement for an actual terrain
chunk under the player in its client quadtree/request path. The client path
tests that a player and render state exist, then requests data using camera X/Z.
The rough generator uses the server seed/density functions rather than a loaded
player chunk. This is static evidence, not a reproduction of every modpack stall.

DH does have multiple independent generation gates:

- Camera speed above 20 blocks/second when `pauseIfMovingQuickly` is enabled.
- In singleplayer, `limitIfServerUnhealthy` requires mean tick time **below
  25 ms** and p99 tick time **below 50 ms**. Stable 20 TPS alone does not satisfy
  this stricter condition. Minimizing rendering or pausing the local world may
  change the observed timing and allow background work to resume.
- Chunk-to-LOD builder and data-processing queue backpressure.
- A disabled generation plan, read-only DH data, or simply no pending tasks.

While flying, F3 includes `AeroSpace LOD` with the current minimum cell width and
the latest DH generation-gate result:

| Gate | Meaning |
| --- | --- |
| `ALLOWED` | DH permitted work; this does **not** prove tasks are running |
| `SERVER_LOAD` | The native server-health gate blocked work |
| `CHUNK_BACKLOG` | The native chunk-builder backlog blocked work |
| `UNKNOWN` | No recent gate observation, for example an empty generation queue |

The next line shows integrated-server mean/p99 times. These metrics deliberately
match DH's calculation. The adapter does not disable health protection to hide
load; compare the status before/after minimizing the window to diagnose that
case. Multiplayer server timing is not available to the client.

## Development verification

Build and unit tests: `./gradlew build`.

With the supplied DH JAR available locally:

```sh
./gradlew -PintegrationRuntime -PdhJar=/absolute/path/DistantHorizons-3.3.3-1.21.1.jar runClient
```

The development-only smoke source set verifies the actual transformed DH
quadtree, motion-pause override, native gate diagnostics and exit restoration:

```sh
./gradlew -PintegrationRuntime -PdhJar=/absolute/path/DistantHorizons-3.3.3-1.21.1.jar \
  -PdhClientSmoke -PquickPlayWorld=AeroSpace-DH-Smoke runClient
```

Prepare a disposable singleplayer save named `AeroSpace-DH-Smoke` first. The
smoke test exits the client when successful and logs `AEROSPACE_DH_SMOKE PASS`.
Its source set is not included in the published mod JAR. Tests are not a
performance benchmark: flight speed, world generator, DH render distance, CPU
budget and modpack load determine whether new surface can keep up in practice.

The client smoke test also reads back pixels from small offscreen buffers:
missing terrain is filled; existing vanilla depth, existing DH depth, an upward
view, and a camera below the plane remain unchanged. It checks that world depth
is preserved, covers both normal and reversed depth, and verifies that the actual
DH render hook executes.
