#version 150
in vec3 Position;
in vec2 UV0;
uniform mat4 DhViewProjection;
uniform vec3 SurfaceOffset;
out vec3 surfacePosition;
out vec2 surfaceSamplePosition;
void main() {
    surfacePosition = Position + SurfaceOffset;
    surfaceSamplePosition = UV0 + SurfaceOffset.xz;
    gl_Position = DhViewProjection * vec4(surfacePosition, 1.0);
}
