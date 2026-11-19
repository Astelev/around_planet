#version 150
in vec3 Position;
uniform mat4 InverseProjection;
uniform mat4 RayTransform;
out vec3 rayDirection;
void main() {
    vec4 viewRay = InverseProjection * vec4(Position.xy, 1.0, 1.0);
    rayDirection = (RayTransform * vec4(viewRay.xyz, 0.0)).xyz;
    gl_Position = vec4(Position.xy, 1.0, 1.0);
}
