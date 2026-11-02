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
| `fallbackPlane` | `true` | Draw a temporary surface in DH's terrain pass |
| `fallbackHeight` | `60` | Height of the temporary plane |
| `fallbackColor` | `0x718a54` | Neutral RGB color until a biome sample is available |
| `fallbackBrightness` | `0.55` | Surface brightness multiplier, before world lighting |
| `fallbackBiomeColors` | `true` | Budgeted independent biome sampling in singleplayer |
| `nearbyGenerationPriority` | `true` | Rank queued DH tasks by distance to their bounds during sky flight |

## Temporary visual floor

During sky flight, the temporary surface is inserted into **DH's terrain target**
after opaque LODs and before SSAO, transparency, fog and compositing. It is a
world-space quad at Y=60, projected directly with DH's forward view/projection
matrix and its exact camera position, just like native terrain buffers. No inverse
screen-ray reconstruction or separately sampled Minecraft camera is involved.
The rasterizer writes the actual depth of that surface into the DH render buffer. Native DH fog and far fading therefore see its proper distance
and height. Surface colors are multiplied by DH's open-sky lightmap, so the plane
responds to day/night, weather and the lightmap's other adjustments. Biome tints
are reduced to 80% saturation and 55% brightness by default to better match
textured LOD terrain; `fallbackBrightness` controls the brightness multiplier.

The plane is drawn only where both existing LOD depth and vanilla depth are
clear. Real rendered terrain, including valleys below Y=60, wins. Sky above the
horizon remains untouched. A GPU depth copy avoids sampling the same depth texture
being written; it is reused and resized with the viewport. There is no CPU
readback in production. The DH render buffer is cleared normally each frame;
no fake terrain data is stored in chunks or the DH database.

Standard OpenGL forward/reversed depth is supported, including reversed DH with
conventional Minecraft depth. A shared vanilla/DH depth texture or a target whose
depth attachment differs from DH's active depth texture is skipped. Shader-pack
compatibility beyond this standard path is not established.

## Independent biome colors

In singleplayer, a low-priority background worker directly samples the integrated
server's biome source and climate sampler at the plane height. This **does not**
request chunks or run the terrain's final-density or surface-generation passes.
The bounded palette has 66×66 cells of 128 blocks, about 4.2 km in each direction.
Missing colors are sampled nearest-first, starting under the camera.

At most one batch runs at a time, with no queued backlog. Each client tick can
start up to 64 samples with a cooperative 2 ms budget. A single ongoing lookup
cannot be interrupted; subsequent lookups are deferred once the budget expires.
Neutral cells remain visible immediately if sampling cannot keep up with flight.
The small color texture refreshes at most five times a second, except when the
camera moves the grid or the world changes. F3 reports known palette cells.

Oceans/rivers use water color; deserts/beaches use sand or stone; badlands use a
terracotta tint; cold biomes use snow; other biomes use their grass color. These
are coarse biome colors, not a prediction of individual blocks or terrain height.
The integration can also reuse existing DH biome observations. Multiplayer
clients cannot independently sample an undisclosed world seed: without available
samples, their cells remain neutral. No seed request or server protocol is added.

The palette clears on flight/world exit and dimension change. In-flight jobs
carry a world epoch, so old-world results cannot color a new world.

## Nearby generation priority

DH 3.3.3 already sorts retrieval requests by distance. Its generation queue,
however, ranks a task using **distance to the section's center divided by its
detail level**. A coarse, large section can receive a discount, while a section
covering the player can be penalized because its center is far away.

During sky flight, the optional adapter ranks waiting generation tasks by
horizontal distance to the section's **bounds**, without the coarse discount.
A section containing the target comes first; equal distances prefer the smaller
section. This operates on DH's current generation target, so it follows movement.
Already running jobs finish normally, and native generation/load limits remain
in force. Leaving sky flight restores DH's original ranking. This cannot make
all nearby LODs appear immediately: generation, data processing and mesh building
still take time and have their own queues.

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
view, and a camera below the plane remain unchanged. Written DH depth is
unprojected to verify Y=60. Normal/reversed depth and a dark lightmap are checked.
The test verifies the transformed native generation-queue ranking in and out of
sky flight, then waits for the actual DH surface hook and independent biome
sampling to run. These tests do not benchmark an entire flight or guarantee
compatibility with shader packs.
