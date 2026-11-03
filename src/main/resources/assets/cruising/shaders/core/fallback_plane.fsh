#version 150
uniform sampler2D DhDepth;
uniform sampler2D WorldCoverage;
uniform int HasWorldCoverage;
uniform sampler2D VanillaDepth;
uniform sampler2D BiomePalette;
uniform sampler2D SurfaceLightmap;
uniform vec3 CameraPosition;
uniform float MaxDistance;
uniform vec2 PaletteOrigin;
uniform float PaletteSpan;
uniform vec3 FallbackColor;
uniform float DhClearDepth;
uniform float VanillaClearDepth;
uniform float ColorBrightness;
in vec3 surfacePosition;
in vec2 surfaceSamplePosition;
out vec4 fragColor;
void main() {
    vec2 screenUV = gl_FragCoord.xy / vec2(textureSize(DhDepth, 0));
    if (texture(VanillaDepth, screenUV).r != VanillaClearDepth) discard;
    if (length(surfacePosition.xz) > MaxDistance) discard;
    vec2 paletteUV = (CameraPosition.xz + surfaceSamplePosition - PaletteOrigin) / PaletteSpan;
    vec3 color = FallbackColor;
    if (all(greaterThanEqual(paletteUV, vec2(0.0))) && all(lessThan(paletteUV, vec2(1.0)))) {
        if (HasWorldCoverage != 0 && texture(WorldCoverage,paletteUV).r > 0.5) discard;
        vec4 tile = texture(BiomePalette, paletteUV);
        // A fully owned tile owns its sky pixels as well as blocks.
        if (tile.a < 0.5) discard;
        color = tile.rgb;
    } else if (texture(DhDepth, screenUV).r != DhClearDepth) discard;
    // Inside the coverage atlas, unknown tiles use ordinary DH depth testing:
    // a nearer fallback can occlude a farther LOD, but never the same ready area.
    // Biome grass/water colors are tints, brighter than a textured LOD surface.
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luminance), color, 0.8) * ColorBrightness;
    // Vanilla lightmap upload: X = block light, Y = sky light.
    // Open surface has no emissive block light and full exposure to the sky.
    color *= texture(SurfaceLightmap, vec2(0.5 / 16.0, 15.5 / 16.0)).rgb;
    fragColor = vec4(color, 1.0);
    // Depth is rasterized from world-space tile geometry, like real LODs.
}
