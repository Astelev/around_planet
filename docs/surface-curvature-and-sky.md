# Curved surface and shared sky — 0.11.0-alpha.12

## World Curvature reference

Inspected the user-supplied `World Curvature.zip` by Numpy_0 (MIT, 2026).
It contains GLSL 1.20 terrain/entity vertex passes with a fixed radius of 1000
and a simple drop `horizontalDistance² / (2 * radius)`. Its legacy fragment
passes include their own lighting/brightness adjustments. This implementation
uses the curvature principle with the **configured planet radius**, preserving
Minecraft lighting and current render backends rather than installing the pack.
The provided MIT notice is retained in `META-INF/licenses/World-Curvature.txt`.

The bend is only visual. World coordinates, blocks, terrain generation, collision,
ship physics, orbit charts and seams are unchanged. It is the local parabolic
approximation from the reference, not a new spherical topology.

## Coherent geometry

Recover camera-relative world orientation from each vertex's view position,
apply the distance-based vertical drop, then use that pass's own projection.
Bending before projection avoids dependence on the camera's pitch/yaw and on
per-renderer near/far clip distances. Do not reconstruct distant world points
from clip coordinates: this loses precision and breaks passes with different
projections.

- Stock Minecraft world shaders cover chunks, ordinary entities, block entities,
  particles and retained terrain meshes. Screen-space/HUD/hand and postprocessing
  shaders are excluded by render scope/program selection.
- Sodium 0.8.13 terrain receives the same view-space operation.
- DH 3.3.3 OpenGL terrain uses the same parabola on its projection position, keeping
  original fragment-space coordinates for chunk-ownership and seam masks. Its
  built-in independent earth bend is disabled only for our active pass, avoiding
  double curvature. DH settings files are not overwritten.
- Cruising's cheap fallback surface uses the same camera-relative drop before its
  DH projection. Original palette/coverage coordinates remain unchanged. The four
  outer quads are tessellated at a target 512-block spacing, bounded to 64 cells
  per axis; the existing central biome grid is retained. This adds no generator
  requests and requires updating only Around Planet.
- Veil's vanilla dynamic-buffer recompilation is patched too: it rereads resources
  and would otherwise discard the original shader modification. Uniform locations
  are invalidated when those programs are relinked.
- Sable's dynamic Veil ship shaders receive the same operation and uniforms;
  plain native world shaders follow the stock route. Geometry is never copied
  or removed to produce curvature.

Simple Clouds geometry/shadow/postprocessing stays in its own native pipeline.
It is not bent by these terrain shaders. Rewriting that pipeline produced invalid
cloud geometry during development; the release deliberately excludes it. Custom
Flywheel/other mod renderers may need their own adapters. An active Iris shader
pack owns the surface visuals instead; our surface bend and sky are disabled
while it is active. Tested optional rendering targets are Sodium/DH OpenGL.

This is a vertex displacement: very coarse quads remain coarse, and vanilla
frustum/occlusion decisions still use unbent geometry. The modest near-field bend
is tiny at ordinary chunk distance, but distant renderer/culling integration may
need further refinement in larger/custom packs. It does not create missing DH
terrain or replace the generator.

## Sky

The surface shares the cached deterministic star mesh and the space sky's round
Sun style. Round shaded Moon rendering retains the ordinary eight phases.
Sun/Moon and stars follow Overworld time; weather dims them. Sunrise/sunset color
and the apparent horizon respond to direction/observer height. Nether/End,
underwater/lava/powder-snow sky handling are retained.

Sky and fog begin darkening smoothly at Y=1600, reaching the space look at the
configured ascent threshold (2208 by default). Stars become visible as the high
sky darkens. Terrain/block lighting is not reduced by this sky fade. The curve
of the sky horizon is only used when surface curvature is enabled.

In `<world>/serverconfig/around_planet-server.toml`:

```toml
[surfaceVisuals]
curvature = true
sky = true
darkenFromY = 1600
```

Surface visuals apply in the enabled Overworld planet, outside registered space
cells. In space the existing inertial Sun, atmospheric shell and procedural globe
remain unchanged. The surface time cycle does not rotate the fixed cosmic Sun.
No additional shader-pack dependency is needed.

## Checks

90 unit tests include radius scaling, rotational symmetry, altitude fade, Sun
clock/horizon math and shader selection/patch idempotence. 21 native server tests
and 10 standalone server tests retain prior seams, bearings, seats, atmospheres,
orbit/persistence and migration coverage.

Separate real clients exercise the stock renderer without optional mods and
Sodium/DH/Simple Clouds/Sable. Captures include a clear round Sun, full Moon/stars,
curved versus disabled curvature and dark high-altitude sky. The harness disables cloud options for captures and restores the Simple Clouds
setting on exit; optional cloud compositing can still obscure distant terrain. The stock-only harness
isolates its subscriber so it does not load
optional Sable/DH test classes. Production artifacts exclude all smoke classes.
The full space ascent/sky/map/seated-return smoke is retained as a regression.

Alpha.12 additionally checks that the actual Cruising fallback GPU program receives
active curvature with Sodium, DH, Sable/Veil and Simple Clouds installed.
