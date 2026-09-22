#version 330
#define UNREL_ID
#define NO_PARAMS
#define ADD_SHIFT

#define MAP_DEPTH 0.9999
#define MARKER_DEPTH 0.9999

#extension GL_ARB_separate_shader_objects : require

#include <nminimap:config.glsl>

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:globals.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 3) in ivec2 UV2;
#endif

uniform sampler2D Sampler0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

layout(location = 4) flat out int custom;
layout(location = 5) out vec2 uvCoord;
layout(location = 6) flat out vec3 b_meta;
layout(location = 7) flat out vec4 box;

#include <nminimap:vertex_utils.glsl>

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    
    // vec2 texSize = textureSize(Sampler0, 0);
    // ivec2 uv = ivec2(UV0 * texSize);
    // vec4 testColor = texelFetch(Sampler0, uv, 0);
    // int idTex = id(uv);

    // const vec2 corners[] = vec2[](vec2(0, 0), vec2(0, 1), vec2(1, 1), vec2(1, 0));

    // if (texSize == vec2(256) && round(testColor.a * 255) == 3 && ((idTex & 0xffff) == 0x0300))
    // {
    //     vec2 corner = corners[gl_VertexIndex % 4];

    //     vertexColor = vec4(corner, 0, 1);
    // }

    #include <nminimap:vertex_body.glsl>
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;
}
