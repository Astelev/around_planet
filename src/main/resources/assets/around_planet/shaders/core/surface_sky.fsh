#version 150
in vec3 rayDirection;
uniform vec3 SkyColor;
uniform vec3 SunDirection;
uniform float Darkness;
uniform float Horizon;
uniform float MoonPhase;
uniform float Visibility;
uniform int Layer;
out vec4 fragColor;
vec3 background(vec3 ray) {
    float band=exp(-abs(ray.y-Horizon)*7.0);
    vec3 color=mix(SkyColor*0.62,SkyColor,band);
    float sunset=pow(1.0-min(1.0,abs(SunDirection.y)*3.0),3.0);
    vec2 side=SunDirection.xz;side/=max(length(side),0.0001);
    vec2 look=ray.xz;look/=max(length(look),0.0001);
    color+=vec3(0.6,0.15,0.025)*band*sunset*pow(max(0.0,dot(side,look)),8.0)*Visibility;
    return color*mix(1.0,0.008,Darkness);
}
void main() {
    vec3 ray=normalize(rayDirection);
    vec3 bg=background(ray);
    if(Layer==0){fragColor=vec4(bg,1.0);return;}
    float above=smoothstep(Horizon-0.015,Horizon+0.015,ray.y);
    float sunDot=dot(ray,SunDirection);
    float sunDisk=smoothstep(cos(0.014),cos(0.012),sunDot);
    float sunHalo=exp((sunDot-1.0)*5000.0)*0.2;
    float sunAlpha=min(1.0,sunDisk+sunHalo)*Visibility*above;
    vec3 color=mix(bg,vec3(1.0,0.95,0.78),sunAlpha);
    float alpha=max(1.0-above,sunAlpha);
    vec3 moon=-SunDirection;
    vec3 right=vec3(0.0,0.0,1.0),up=normalize(cross(moon,right));
    vec2 coord=vec2(dot(ray,right),dot(ray,up))/sin(0.013);
    float d2=dot(coord,coord);
    if(dot(ray,moon)>0.0 && d2<1.0){
        vec3 normal=vec3(coord,sqrt(max(0.0,1.0-d2)));
        float phase=MoonPhase*0.7853981634;
        float light=max(0.0,dot(normal,vec3(sin(phase),0.0,cos(phase))));
        float speck=0.85+0.15*sin(coord.x*37.0+sin(coord.y*25.0))*sin(coord.y*29.0);
        vec3 moonColor=mix(vec3(0.004,0.006,0.012),vec3(0.8,0.84,0.92)*speck,light);
        float a=(1.0-smoothstep(0.94,1.0,d2))*Visibility*above;
        color=mix(color,moonColor,a);alpha=max(alpha,a);
    }
    if(alpha<0.0005)discard;
    // Convert the already-composited color to the straight-alpha source for the sky backdrop.
    fragColor=vec4((color-bg*(1.0-alpha))/max(alpha,0.00001),alpha);
}
