#version 150
#moj_import <fog.glsl>
uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
uniform float FlowTime;
in float vertexDistance;
in vec4 vertexColor;
in vec2 gasCoord;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(mod(p,64.0),vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);}
void main(){
    float angle=gasCoord.x*6.28318530718;
    // A fixed pattern at q travels toward increasing distance, away from the nozzle, at 24 m/s.
    float q=gasCoord.y-FlowTime*24.0;
    float cloud=noise(vec2(sin(angle*3.0)*2.0+cos(angle)*3.0,q*2.0));
    float band=0.5+0.5*sin(q*3.14159265359+sin(angle*5.0)*1.4);
    band=band*band*band*band*band;
    float ribbon=0.5+0.5*sin(angle*7.0+q*1.07992247467);
    float flow=0.25+0.55*cloud+0.6*band*(0.5+0.5*ribbon);
    vec4 color=vertexColor*ColorModulator;
    color.rgb*=0.65+0.55*flow;
    color.a*=0.12+0.85*smoothstep(0.20,1.10,flow);
    fragColor=linear_fog(color,vertexDistance,FogStart,FogEnd,FogColor);
}
