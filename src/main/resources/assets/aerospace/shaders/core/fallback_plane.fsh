#version 150
uniform sampler2D DhDepth;
uniform sampler2D BiomePalette;
uniform mat4 InverseDhViewProjection;
uniform vec3 CameraPosition;
uniform float PlaneHeight;
uniform float FogDistance;
uniform vec2 PaletteOrigin;
uniform float PaletteSpan;
uniform vec3 FallbackColor;
uniform vec3 PlaneFogColor;
uniform float DhClearDepth;
uniform float VanillaClearDepth;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    // Match DH's own definition of an undrawn depth pixel exactly.
    if (texture(DhDepth, texCoord).r != DhClearDepth) discard;
    // Two finite samples avoid the infinite far plane used by reversed-Z DH.
    // Subtraction removes view translation/bobbing from the ray direction.
    vec4 a = InverseDhViewProjection * vec4(texCoord * 2.0 - 1.0, 0.25, 1.0);
    vec4 b = InverseDhViewProjection * vec4(texCoord * 2.0 - 1.0, 0.75, 1.0);
    vec3 ray = (b.xyz / b.w - a.xyz / a.w) * (DhClearDepth == 0.0 ? -1.0 : 1.0);
    if (ray.y >= -0.000001 || CameraPosition.y <= PlaneHeight) discard;
    vec3 delta = ray * ((PlaneHeight - CameraPosition.y) / ray.y);
    vec2 worldXZ = CameraPosition.xz + delta.xz;
    vec2 paletteUV = (worldXZ - PaletteOrigin) / PaletteSpan;
    vec3 color = FallbackColor;
    if (all(greaterThanEqual(paletteUV, vec2(0.0))) && all(lessThan(paletteUV, vec2(1.0)))) {
        color = texture(BiomePalette, paletteUV).rgb;
    }
    float fog = smoothstep(FogDistance * 0.65, FogDistance, length(delta.xz));
    fragColor = vec4(mix(color, PlaneFogColor, fog), 1.0);
    // Do not compete with real terrain, including valleys BELOW the plane.
    // The renderer tests equal-to-clear against vanilla depth and never writes it.
    gl_FragDepth = VanillaClearDepth;
}
