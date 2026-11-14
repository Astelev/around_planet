# RSU model, horizon roll and wrench interaction (0.11.0-alpha.19)

The artificial horizon in the AeroEngine helmet now mirrors its horizontal roll
component. Pitch and the forward/back hemisphere remain unchanged. This is a
visual correction to the sphere background: the selected target's bearing, the
sensor's physical needle and redstone control signals retain their existing
conventions. The texture remains batched and limited to one upload per game tick.

The RSU has a purple central housing, four hollow gray exhaust tubes, raised rims
and inset dark throats, inspired by the supplied Gadgets and Gizmos reference.
Small gold/copper/red/green tabs still identify the four frequency channels. The
model supports all six placement directions. The 8×4×8 hitbox and four lateral
force axes are unchanged.

Sensor, creative thruster, RSU and angular damper configuration now opens only
when **both hands are empty**. Held items pass through to normal item interaction,
including an offhand tool. With Create installed, all four blocks implement its
native wrench interface: ordinary wrench clicks rotate them, and Create's normal
sneak-wrench behavior remains available. Rotation retains the block entity and
its sensor mode, RSU frequencies, thrust setting and FE buffer. The API mixin is
excluded when Create is absent, preserving standalone startup.

Regression tests cover horizontal horizon reflection with unchanged pitch,
held-item/empty-hand interaction, and actual Create wrench rotation of all four
blocks while preserving configuration. The client smoke uses the ordinary player
interaction path to open screens, verifies held items do not open them, sends
frequency/slider packets, and tests the real wrench without opening a screen.

Validation: 118 unit tests, 32 native Sable/Create/AeroEngine server tests and
15 standalone server tests pass. The real-client smoke passes for held items,
empty-hand screens, frequency packets and native Create wrench rotation. The
new RSU model was inspected in a close-up client screenshot.
