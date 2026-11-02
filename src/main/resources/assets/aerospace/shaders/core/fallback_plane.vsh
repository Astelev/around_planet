#version 150
in vec3 Position;
uniform mat4 DhViewProjection;
uniform vec3 SurfaceOffset;
out vec3 surfacePosition;
void main() {
    surfacePosition = Position + SurfaceOffset;
    gl_Position = DhViewProjection * vec4(surfacePosition, 1.0);
}
