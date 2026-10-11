#version 150
// Original: assets/hbm/shaders/swarm.vert (NTM Space), GLSL 130 -> 150 (feste Attribute wie default.vsh).
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float iTime;

out vec3 vPosition;
out vec4 vColor;
out vec4 texCoord0;

float hash(float x){ return fract(cos(x*124.123)*412.0); }

void main() {
	vPosition = Position;
	vColor = Color;

	float v = float(gl_VertexID);
	float t = v + iTime;
	float r = hash(v);
	float r2 = hash(v + 0.5);
	float r3 = hash(v + 0.75);

	float y = (cos(t) * r + sin(t) * (1.0 - r)) * 0.2 * r2 * r2;
	float o = cos(y * 2.0) * (1.0 - ((r3 - 0.5) * 0.05));

	gl_Position = ProjMat * ModelViewMat * (vec4(Position.x * o, y, Position.z * o, 1.0));
	texCoord0 = vec4(UV0, 0.0, 1.0);
}
