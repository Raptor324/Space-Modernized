#version 150
// Ersatz fuer die feste Pipeline bei GL_POINTS mit gebundener Textur (Original: Tessellator.startDrawing(GL_POINTS)
// ohne UV -> Texturkoordinate (0,0) * glColor), z.B. "charged dust" in SkyProviderCelestial.renderSunset.
uniform sampler2D Sampler0;
in vec4 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
	fragColor = texture(Sampler0, texCoord0.xy) * vertexColor;
}
