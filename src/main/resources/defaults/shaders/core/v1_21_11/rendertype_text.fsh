#version 330

#moj_import <nminimap:config.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 uvCoord;
in vec2 texCoord0;

flat in int custom;

out vec4 fragColor;

void main() {

    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;

    #moj_import <nminimap:fragment_body.glsl>
    
    if (color.a < 0.1) {
        discard;
    }

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
