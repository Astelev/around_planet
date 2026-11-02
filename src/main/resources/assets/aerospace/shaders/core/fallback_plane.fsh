#version 150
uniform sampler2D DhDepth;
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
out vec4 fragColor;
void main() {
    vec2 screenUV = gl_FragCoord.xy / vec2(textureSize(DhDepth, 0));
    if (texture(DhDepth, screenUV).r != DhClearDepth) discard;
    if (texture(VanillaDepth, screenUV).r != VanillaClearDepth) discard;
    if (length(surfacePosition.xz) > MaxDistance) discard;
    vec2 paletteUV = (CameraPosition.xz + surfacePosition.xz - PaletteOrigin) / PaletteSpan;
    vec3 color = FallbackColor;
    if (all(greaterThanEqual(paletteUV, vec2(0.0))) && all(lessThan(paletteUV, vec2(1.0))))
        color = texture(BiomePalette, paletteUV).rgb;
    // Biome grass/water colors are tints, brighter than a textured LOD surface.
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luminance), color, 0.8) * ColorBrightness;
    // Vanilla lightmap upload: X = block light, Y = sky light.
    // Open surface has no emissive block light and full exposure to the sky.
    color *= texture(SurfaceLightmap, vec2(0.5 / 16.0, 15.5 / 16.0)).rgb;
    fragColor = vec4(color, 1.0);
    // Depth is rasterized directly from the world-space Y=60 quad, like real LODs.
}
