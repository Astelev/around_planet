# Larger exhaust with advected gas (0.11.0-alpha.22)

Engine exhaust is approximately four times as long as alpha.21; RSU exhaust is
approximately three times as long. At default full signal, the engine reaches
about 6 blocks and RSU about 1.25 blocks; their capped maxima are 12 and 2.4.
The bright core and expanding cone remain separate. Active minimum cone lengths
are now 3.2 blocks for the engine and 0.72 for RSU, with no plume when switched off.
The core widens gently after the nozzle and the cone's far radius is larger.
Finite render bounds expand to cover the complete visible effect.

A shared GPU material animates streaks and density variations over those same
16 quads per nozzle. Its longitudinal coordinate is in blocks; sampling at
`distance - 24 × seconds` makes the pattern travel **away from the nozzle at
24 blocks/s**. Circumferential variation gives the streaks a gas-like texture.
It uses no fire animation, particle objects, uploaded texture frames or new
per-nozzle draw calls. The gas opacity still fades along the cone. Core opacity
is reduced to keep the moving density pattern visible instead of saturating the
whole core to solid white.

Time comes from the client world clock with partial ticks and is shared across
the batched draw. The periodic field repeats cleanly at the daily time wrap.
The material is emissive, additive, depth-tested and does not write depth.
It uses Minecraft fog and the existing Around Planet curvature path. Physics,
redstone behavior, fuel/FE rules, sensor and wrench controls are unchanged.

Validation covers larger default/capped sizes, bounded render extent, low-thrust
cone floors, power-off and opacity. Native integration regressions remain. The
actual Sodium/DH/Simple Clouds/AeroEngine client loads the shader, verifies that
flow time advances, and captures full/low-thrust, power-off and wall-mounted jets.

Release validation: 123 unit tests, 32 native Sable/Create/AeroEngine server
tests and 15 standalone server tests pass. The final GPU-shader client smoke
passes, and the moving surface pattern was checked in separate full-thrust
frames as well as low-thrust/off/wall captures. Test classes are excluded.
