#version 150
uniform vec3 SunDirection;
in vec3 normal;
in vec4 baseColor;
out vec4 fragColor;
void main(){float light=0.045+0.955*max(0.0,dot(normalize(normal),SunDirection));fragColor=vec4(baseColor.rgb*light,baseColor.a);}
