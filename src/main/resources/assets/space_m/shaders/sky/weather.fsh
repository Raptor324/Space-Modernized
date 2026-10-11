#version 150
// Original WeatherProviderCelestial.enableTextureAlphaTint(): GL_COMBINE mit RGB = Vertexfarbe (GL_REPLACE aus
// GL_PRIMARY_COLOR) und Alpha = Texturalpha * Vertexalpha; Einheit 1 (Lichtkarte) moduliert wie gewohnt.
uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in vec4 texCoord0;
in vec4 vertexColor;
in vec2 lightCoord;
in float vertexDistance;

out vec4 fragColor;

void main() {
	vec4 tex = texture(Sampler0, texCoord0.xy);
	vec4 light = texture(Sampler2, clamp(lightCoord / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
	vec4 color = vec4(vertexColor.rgb * light.rgb, tex.a * vertexColor.a);
	if (color.a < 0.1) discard;
	if (vertexDistance > FogStart) {
		float fogValue = vertexDistance < FogEnd ? smoothstep(FogStart, FogEnd, vertexDistance) : 1.0;
		color.rgb = mix(color.rgb, FogColor.rgb, fogValue * FogColor.a);
	}
	fragColor = color;
}
