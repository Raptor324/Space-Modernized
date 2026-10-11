#version 150
// Wetter von NTM Space (WeatherProviderCelestial): feste Pipeline mit Lichtkarte (setBrightness) und Nebel.
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 texCoord0;
out vec4 vertexColor;
out vec2 lightCoord;
out float vertexDistance;

void main() {
	vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
	gl_Position = ProjMat * viewPos;
	vertexDistance = length(viewPos.xyz);
	texCoord0 = vec4(UV0, 0.0, 1.0);
	vertexColor = Color;
	lightCoord = UV2;
}
