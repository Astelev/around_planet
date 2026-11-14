#version 150
in vec3 Position;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float PlanetSpin;
out vec3 normal;
out vec4 baseColor;
void main(){float c=cos(PlanetSpin),s=sin(PlanetSpin);normal=vec3(c*Position.x+s*Position.y,-s*Position.x+c*Position.y,Position.z);baseColor=Color;gl_Position=ProjMat*ModelViewMat*vec4(normal,1.0);}
