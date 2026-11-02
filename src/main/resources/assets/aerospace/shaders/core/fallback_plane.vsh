#version 150
in vec3 Position;
uniform mat4 DhViewProjection;
out vec3 surfacePosition;
void main() {
    surfacePosition = Position;
    gl_Position = DhViewProjection * vec4(Position, 1.0);
}
