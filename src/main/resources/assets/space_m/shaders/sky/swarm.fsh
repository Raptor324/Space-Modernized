#version 150
// Original: assets/hbm/shaders/swarm.frag (NTM Space), mechanisch nach GLSL 150 (sR1c_shaders.py)
in vec4 texCoord0;
out vec4 fragColor;

in vec3 vPosition;
in vec4 vColor;

void main() {
	fragColor = vColor;
}
