#version 150
uniform vec3 SunDirection;
in vec3 normal;
in vec3 fixedNormal;
uniform sampler2D PlanetAtlas;
uniform int HasAtlas;
in vec4 baseColor;
out vec4 fragColor;
void main(){float light=0.045+0.955*max(0.0,dot(normalize(normal),SunDirection));vec3 n=normalize(fixedNormal);vec2 uv=vec2(atan(n.x,n.y)/6.28318530718+.5,asin(clamp(n.z,-1.0,1.0))/3.14159065359+.5);vec3 albedo=HasAtlas!=0?texture(PlanetAtlas,uv).rgb:baseColor.rgb;fragColor=vec4(albedo*light,1.0);}
