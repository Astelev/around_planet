#version 150
in vec3 rayDirection;
uniform vec3 SkyColor;
uniform vec3 SunDirection;
uniform float SunAngularRadius;
uniform float Darkness;
uniform float Horizon;
uniform vec3 MoonCenter;
uniform float MoonRadius;
uniform vec3 MoonTextureX;
uniform vec3 MoonTextureZ;
uniform float Visibility;
uniform int Layer;
uniform vec4 CloudFog;
uniform sampler2D PlanetAtlas;
uniform int HasAtlas;
uniform float PlanetBlend;
uniform float PlanetObserver;
uniform vec3 PlanetEast;
uniform vec3 PlanetUp;
uniform vec3 PlanetSouth;
// Unit sphere in the local east/up/south frame; shares the orbit atlas chart.
vec4 ground(vec3 ray) {
    float b=PlanetObserver*ray.y;
    float discriminant=b*b-(PlanetObserver*PlanetObserver-1.0);
    if(b>=0.0 || discriminant<0.0 || PlanetBlend<=0.0)return vec4(0.0);
    float hit=-b-sqrt(max(0.0,discriminant));
    vec3 normal=normalize(vec3(0.0,PlanetObserver,0.0)+ray*hit);
    vec3 fixedNormal=normalize(PlanetEast*normal.x+PlanetUp*normal.y+PlanetSouth*normal.z);
    vec2 uv=vec2(atan(fixedNormal.x,fixedNormal.y)/6.28318530718+.5,asin(clamp(fixedNormal.z,-1.0,1.0))/3.14159065359+.5);
    vec3 albedo=HasAtlas!=0?texture(PlanetAtlas,uv).rgb:vec3(.28,.40,.24);
    float light=.045+.955*max(0.0,dot(normal,SunDirection));
    float limb=pow(1.0-max(0.0,dot(normal,-ray)),4.0);
    vec3 color=mix(albedo*light,vec3(.15,.32,.55)*light,limb*.35);
    color=mix(color,CloudFog.rgb,CloudFog.a);
    return vec4(color,1.0);
}
out vec4 fragColor;
vec3 background(vec3 ray) {
    float band=exp(-abs(ray.y-Horizon)*7.0);
    vec3 color=mix(SkyColor*0.62,SkyColor,band);
    float sunset=pow(1.0-min(1.0,abs(SunDirection.y)*3.0),3.0);
    vec2 side=SunDirection.xz;side/=max(length(side),0.0001);
    vec2 look=ray.xz;look/=max(length(look),0.0001);
    color+=vec3(0.6,0.15,0.025)*band*sunset*pow(max(0.0,dot(side,look)),8.0)*Visibility;
    return mix(color*mix(1.0,0.008,Darkness),CloudFog.rgb,CloudFog.a);
}
void main() {
    vec3 ray=normalize(rayDirection);
    vec4 planet=ground(ray);
    vec3 bg=mix(background(ray),planet.rgb,planet.a*PlanetBlend);
    if(Layer==0){fragColor=vec4(bg,1.0);return;}
    float above=smoothstep(Horizon-0.015,Horizon+0.015,ray.y);
    float sunDot=dot(ray,SunDirection);
    float sunDisk=smoothstep(cos(SunAngularRadius*1.08),cos(SunAngularRadius*.92),sunDot);
    // Compact corona plus a broad, smooth atmospheric aureole (stronger near the ground).
    float air=1.0-Darkness;
    float scaledSunDot=(sunDot-1.0)*(.013*.013)/max(.000001,SunAngularRadius*SunAngularRadius);
    float sunHalo=exp(scaledSunDot*1800.0)*0.48
                 +exp(scaledSunDot*180.0)*0.18*air
                 +exp(scaledSunDot*35.0)*0.055*air;
    float sunAlpha=min(1.0,sunDisk+sunHalo)*Visibility*above;
    vec3 color=mix(bg,vec3(1.0,0.95,0.78),sunAlpha);
    float alpha=max(max(1.0-above,sunAlpha),planet.a*PlanetBlend);
    float along=dot(ray,MoonCenter);
    vec3 perpendicular=MoonCenter-ray*along;
    float d2=dot(perpendicular,perpendicular)/max(MoonRadius*MoonRadius,0.0001);
    if(MoonRadius>0.0 && along>0.0 && d2<1.0){
        float hit=along-MoonRadius*sqrt(max(0.0,1.0-d2));
        vec3 normal=normalize(ray*hit-MoonCenter);
        float light=0.045+0.955*max(0.0,dot(normal,SunDirection));
        float nx=dot(normal,MoonTextureX),nz=dot(normal,MoonTextureZ);
        float albedo=(155.0+20.0*sin(nx*43.0)*cos(nz*31.0))/255.0;
        // Daylight is scattered in front of the Moon too: its unlit hemisphere must not
        // paint a black disc over the blue sky. Fade this veil continuously with altitude.
        float daylight=smoothstep(-0.10,0.25,SunDirection.y)*(1.0-Darkness);
        float lunarTransmission=mix(1.0,0.22,daylight);
        vec3 moonColor=mix(bg,vec3(albedo*light),lunarTransmission);
        float a=(1.0-smoothstep(0.98,1.0,d2))*Visibility*above;
        color=mix(color,moonColor,a);alpha=max(alpha,a);
    }
    if(alpha<0.0005)discard;
    // Convert the already-composited color to the straight-alpha source for the sky backdrop.
    fragColor=vec4((color-bg*(1.0-alpha))/max(alpha,0.00001),alpha);
}
