# Animated exhaust and horizon rollback (0.11.0-alpha.20)

[Alpha.21](two-stage-exhaust.md) replaces the fire sprites below with a bright
active core and an expanding, fading gas cone. The horizon rollback remains.

The artificial horizon uses the original alpha.18 up-vector projection again.
Alpha.19's horizontal reflection is removed. Target bearings, sensor axes and
redstone control logic were not changed by this rollback.

Creative thrusters now show an orange animated exhaust, and each powered RSU
nozzle shows its own blue exhaust. The effect uses **two crossed full-bright atlas
sprite quads per active nozzle**, without spawning or simulating particles. The
existing animated Minecraft fire/soul-fire sprites provide the frames. The normal
sprite wrapper also marks them active for Sodium's visible-texture animation
tracking, without adding a Sodium dependency or a separate texture uploader.

Exhaust starts at the nozzle opening and follows the block's mounting frame and
Sable's normal block-entity render transform. RSU jets follow the model's lightly
splayed tubes. The existing four lateral force axes remain unchanged. Jet length,
width and opacity respond to effective thrust and redstone throttle, with bounded
size and a small smooth pulse. Zero signal or zero configured thrust draws no jet
geometry. There are no per-nozzle particle lists, timers or background threads.
The effect is visible up to 96 blocks, with expanded finite render bounds for jets
outside the original block. It is emissive geometry, not a new world-light source.

Power changes synchronize to the client on the next block tick (normally up to
50 ms), rather than the old half-second interval, so short firing bursts are
visible. FE-only updates remain limited to twice per second, and idle blocks do
not transmit unchanged state. No new recurring animation packets are sent: sprite
frames and the small geometric pulse run entirely on the client.

Validation covers bounded flame sizes, inactive/invalid inputs, signal/thrust
response and animation. Native physics and wrench tests are retained. A real
Sodium/DH/Simple Clouds/AeroEngine client checks individually powered RSU nozzles,
vertical and horizontal creative engines, no geometry after power-off, and resumed
exhaust after wall mounting; powered/off/wall screenshots were inspected.

Release checks: 121 unit tests, 32 native Sable/Create/AeroEngine server tests
and 15 standalone server tests pass. The final animated-sprite client smoke
passes under Sodium with the normal sprite wrapper. Test classes are excluded
from the production artifact.
