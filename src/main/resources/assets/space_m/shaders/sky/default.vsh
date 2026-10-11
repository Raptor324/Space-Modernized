#version 150
// Original: assets/hbm/shaders/default.vert (NTM Space). Feste Pipeline -> Core-Profil:
// gl_Vertex = Position, gl_MultiTexCoord0 = UV0, gl_Color = Color, gl_ModelViewProjectionMatrix = ProjMat * ModelViewMat.
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vPosition;
out vec4 texCoord0;
out vec4 vertexColor;

void main() {
	vPosition = Position;
	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
	texCoord0 = vec4(UV0, 0.0, 1.0);
	vertexColor = Color;
}
