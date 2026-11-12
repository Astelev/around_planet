#version 150
in vec3 rayDirection;
uniform vec3 Observer;
uniform vec3 SunDirection;
uniform float OuterRadius;
out vec4 fragColor;
void main() {
    vec3 direction = normalize(rayDirection);
    float b = dot(Observer, direction);
    float origin2 = dot(Observer, Observer);
    float disc = b*b - origin2 + OuterRadius*OuterRadius;
    if (disc <= 0.0) discard;
    float nearAir = max(0.0, -b-sqrt(disc));
    float farAir = -b+sqrt(disc);
    float surfaceDisc = b*b-origin2+1.0;
    if (surfaceDisc > 0.0) {
        float ground = -b-sqrt(surfaceDisc);
        if (ground > 0.0) farAir = min(farAir, ground);
    }
    if (farAir <= nearAir) discard;
    float stepSize = (farAir-nearAir)/8.0;
    float thickness = max(OuterRadius-1.0, 0.00001);
    float opticalDepth = 0.0;
    vec3 scattered = vec3(0.0);
    for (int i=0; i<8; i++) {
        vec3 point = Observer + direction*(nearAir+(float(i)+0.5)*stepSize);
        float height = clamp((length(point)-1.0)/thickness, 0.0, 1.0);
        float density = exp(-4.0*height)*(1.0-smoothstep(0.0,1.0,height));
        float weight = density*stepSize;
        float sunlight = smoothstep(-0.18,0.3,dot(normalize(point),SunDirection));
        scattered += weight*mix(vec3(0.015,0.035,0.085),vec3(0.18,0.48,0.95),sunlight);
        opticalDepth += weight;
    }
    float alpha = 1.0-exp(-opticalDepth*1.5/thickness);
    if (alpha < 0.001) discard;
    fragColor = vec4(scattered/max(opticalDepth,0.000001),alpha);
}
