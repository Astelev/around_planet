#version 150
uniform sampler2D DhDepth;
uniform sampler2D VanillaDepth;
uniform sampler2D BiomePalette;
uniform sampler2D SurfaceLightmap;
uniform mat4 InverseDhViewProjection;
uniform mat4 DhViewProjection;
uniform vec3 CameraPosition;
uniform float PlaneHeight;
uniform float MaxDistance;
uniform vec2 PaletteOrigin;
uniform float PaletteSpan;
uniform vec3 FallbackColor;
uniform float DhClearDepth;
uniform float VanillaClearDepth;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    // Never compete with genuine LOD data, at any height.
    if (texture(DhDepth, texCoord).r != DhClearDepth) discard;
    if (texture(VanillaDepth, texCoord).r != VanillaClearDepth) discard;
    vec4 a = InverseDhViewProjection * vec4(texCoord * 2.0 - 1.0, 0.25, 1.0);
    vec4 b = InverseDhViewProjection * vec4(texCoord * 2.0 - 1.0, 0.75, 1.0);
    vec3 ray = b.xyz / b.w - a.xyz / a.w;
    if (DhClearDepth < 0.5) ray = -ray;
    if (ray.y >= -0.000001 || CameraPosition.y <= PlaneHeight) discard;
    vec3 origin = a.xyz / a.w;
    vec3 delta = origin + ray * ((PlaneHeight - CameraPosition.y - origin.y) / ray.y);
    if (length(delta.xz) > MaxDistance) discard;
    vec2 paletteUV = (CameraPosition.xz + delta.xz - PaletteOrigin) / PaletteSpan;
    vec3 color = FallbackColor;
    if (all(greaterThanEqual(paletteUV, vec2(0.0))) && all(lessThan(paletteUV, vec2(1.0))))
        color = texture(BiomePalette, paletteUV).rgb;
    // DH lightmap coordinates: sky first, block second. Open surface: sky=15, block=0.
    color *= texture(SurfaceLightmap, vec2(15.5 / 16.0, 0.5 / 16.0)).rgb;
    fragColor = vec4(color, 1.0);
    // Camera-relative geometry in the SAME projection/depth target as real LODs.
    // Native DH fog and far fading now see a surface at Y=60, not a far-depth overlay.
    vec4 clip = DhViewProjection * vec4(delta, 1.0);
    float depth = clip.z / clip.w * 0.5 + 0.5;
    if (depth <= 0.0 || depth >= 1.0) discard;
    gl_FragDepth = depth;
}
