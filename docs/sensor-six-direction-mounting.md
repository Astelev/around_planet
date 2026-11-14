# Six-direction orbital sensor (0.11.0-alpha.17)

The sensor's physical top is now the ship's forward axis. A fixed white cap marks
it; the moving white/yellow needle still points to the selected target. Placement
uses the clicked support face, allowing floor, ceiling and all four wall mounts.
The base sits against the support, with the top pointing away from it. Blockstate
models, animated geometry and collision shapes rotate together.

Four lateral redstone outputs indicate the direction in which the nose deviates
from the selected target. For example, a target to the left means the nose is to
the right of it and the right-side port powers. Target angles in the screen/navball
still describe where the target lies. The axial top/base ports never power. The
output frame follows the mount and the ship's native orientation. Adjacent block
updates include all six sides so wall/ceiling wiring receives updates.

The hemisphere and markers in the optional AeroEngine helmet use this same
sensor frame; they no longer assume a horizontal forward vector. Modes, scaling,
AeroEngine binding and optional dependencies are unchanged.

Existing sensor block entities without the mount-layout marker migrate upright
once on server tick; their settings remain intact. Their former horizontal facing
no longer defines the nose. Existing redstone wiring must be reviewed because
both the reference axis and the signal convention intentionally change.

Tests cover all six pure frames, deviation signs, axial exclusion, alignment and
deadband. Native Sable tests cover real oriented ship readings, wall/ceiling output
sides and legacy upright migration. A separate client smoke renders all six poses
and checks synchronized blockstates and collision shapes.

Validation: 112 unit tests and all 25 native server tests pass. The real
Sodium/DH/Simple Clouds/AeroEngine client completed the six-pose smoke; the
resulting screenshot was inspected. Test classes are excluded from the JAR.
