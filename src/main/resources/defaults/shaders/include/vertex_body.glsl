#define FL_ROTATE 1

vec2 texSize = textureSize(Sampler0, 0);
ivec2 uv = ivec2(UV0 * texSize);

const vec2 corners[] = vec2[](vec2(0, 0), vec2(0, 1), vec2(1, 1), vec2(1, 0));
#ifdef UNREL_ID //We can't rely on gl_VertexID as is cause of merged buffer
    #ifdef GL_ARB_shader_draw_parameters //Take a shortcut if gpu can do it
int idx = gl_VertexID - gl_BaseVertexARB;
    #else //Take by uv
int idx = 0;
if (texSize != vec2(256))
{
    if (uv == ivec2(0, 128)) idx = 0;
    if (uv == ivec2(128, 128)) idx = 1;
    if (uv == ivec2(128, 0)) idx = 2;
    if (uv == ivec2(0, 0)) idx = 3;
}
    #endif
#else
int idx = gl_VertexID;
#endif

vec2 corner = corners[idx % 4];
vec2 corner2 = corners[(idx + 1) % 4];
ivec2 mapUV = uv - ivec2(corner2 * 128);

vec4 testColor = texelFetch(Sampler0, uv, 0);
int idTex = id(uv);

custom = 0;

vec3 local = transpose(mat3(ModelViewMat)) * vec3(1, 0, 0);
float yaw = atan(local.z, local.x);
mat2 rotAngle = mat2(1, 0, 0, 1);

// if (texSize == vec2(128) && id(ivec2(0)) == 0xFF0000 && id(ivec2(1, 0)) == 0x597D27 && id(ivec2(2, 0)) == 0x3737DC) //Map
if (id(mapUV + ivec2(0)) == 0xFF0000 && id(mapUV + ivec2(1, 0)) == 0x597D27 && id(mapUV + ivec2(2, 0)) == 0x3737DC) //Map
{
    int meta = id(mapUV + ivec2(3, 0));
    bool isRight = meta == 0x3F6EDC || meta == 0x006A00;
    bool isRound = meta == 0x006A00 || meta == 0x5E2872;
    if (isRound)
        rotAngle = mat2_rotate_z(yaw);


    float rot_offset = (MAP_CROP_RADIUS + 1) / 128.0;
    vec2 map = rotAngle * (corner2 - rot_offset) + rot_offset;
    map *= MAP_SIZE;

    if (isRight)
        map = map + MAP_OFFSET * vec2(-1, 1) - vec2(MAP_SIZE.x - (63 - MAP_CROP_RADIUS) / 64.0 * MAP_SIZE.x, 0);
    else
        map = map + MAP_OFFSET;
    
    #ifdef MAP_ABSOLUTE_SIZES
    map *= vec2(2, -2) / ScreenSize;
    #else
    map *= vec2(1, -ProjMat[1][1]/ProjMat[0][0]);
    #endif

    gl_Position = vec4(map + vec2(isRight? 1 : -1, 1), MAP_DEPTH, 1);
    vertexColor = vec4(1);
    custom = isRound ? 2 : 1;
    uvCoord = corner2 * 128;

    sphericalVertexDistance = 0;
    cylindricalVertexDistance = 0;
}
else if (texSize == vec2(256) && round(testColor.a * 255) == 3 && ((idTex & 0xffff) == 0x0100)) //Markers
{
#ifndef GL_ARB_shader_draw_parameters //Checking color if GPU doesn't have extension
    idx = int(round(testColor.r * 255)) - 1;
    corner = corners[idx % 4];
#endif
    vec2 scaleData = round(texelFetch(Sampler0, uv + ivec2(0, 1 - corner.y * 2), 0).rg * 255);

    int meta = int(testColor.r*255-1) / 4;
    bool isRight = meta % 2 == 0;
    bool isRound = (meta / 2) % 2 != 0;

    vec2 pos = Color.rg;
    if (isRound)
    {
        rotAngle = mat2_rotate_z(yaw);
        pos -= 0.5;
        pos = normalize(pos) * clamp(length(pos), 0.0, MAP_CROP_RADIUS / 128.0) + 0.5;
    }

    float angle = -Color.b * 2 * PI;

    float offset = isRound ? (1.0 + MAP_CROP_RADIUS) / 128.0 : 0.5;

    vec2 map = rotAngle * (mat2_rotate_z(angle) * ((corner - 0.5) / 64 * scaleData) + pos - 0.5) + offset;
    map *= MAP_SIZE;

    if (isRight)
        map = map + MAP_OFFSET * vec2(-1, 1) - vec2(MAP_SIZE.x - (63 - MAP_CROP_RADIUS) / 64.0 * MAP_SIZE.x, 0);
    else
        map = map + MAP_OFFSET;

    #ifdef MAP_ABSOLUTE_SIZES
    map *= vec2(2, -2) / ScreenSize;
    #else
    map *= vec2(1, -ProjMat[1][1]/ProjMat[0][0]);
    #endif

    gl_Position = vec4(map + vec2(isRight? 1 : -1, 1), MARKER_DEPTH, 1);
    vertexColor = vec4(1);
    
    sphericalVertexDistance = 0;
    cylindricalVertexDistance = 0;
}
else if (texSize == vec2(256) && round(testColor.a * 255) == 3 && ((idTex & 0xffff) == 0x0200)) //Square border
{
#ifndef GL_ARB_shader_draw_parameters //Checking color if GPU doesn't have extension
    idx = int(round(testColor.r * 255)) - 1;
    corner = corners[idx % 4];
#endif
    custom = 4;

    int meta = int(testColor.r*255-1) / 4;
    bool isRight = meta % 2 == 0;
    vec3 scaleData = round(texelFetch(Sampler0, uv + ivec2(0, 1 - corner.y * 2), 0).rgb * 255);
    int metaS = int(scaleData.b);


    ivec2 stp = ivec2(round(UV0 * 256) - scaleData.xy * corner);

    //R = offset.x + 127
    //G = offset,y + 127
    //B = target map content size
    vec4 meta1 = round(texelFetch(Sampler0, stp + ivec2(0, 2), 0) * 255);
    
    float angle = -Color.b * 2 * PI;
    if ((metaS & FL_ROTATE) != 0)
        angle -= yaw;

    vec2 border = scaleData.xy - vec2(meta1.b + 3, meta1.b + 1);
    texCoord0 = UV0 + vec2(1 - corner.x * 2, 0) / texSize;

    box = vec4(stp + vec2(1, 0), scaleData.xy - vec2(2, 0));
    b_meta = vec3(meta1.b, 0, 0);
    uvCoord = corner;

    vec2 map = (mat2_rotate_z(angle) * (corner * (1 + border / 127 * 2) - border / 127.0 - 1 + meta1.rg/127.0 - 0.5) + 0.5) * MAP_SIZE;

    if (isRight)
        map = map + MAP_OFFSET * vec2(-1, 1) - vec2(MAP_SIZE.x, 0) + vec2(-1 /256.0 + (127 - MAP_CONTENT_SIZE) / 256.0) * vec2(1,-1) * MAP_SIZE;
    else
        map = map + MAP_OFFSET + vec2(-(127 - MAP_CONTENT_SIZE) / 256.0) * MAP_SIZE;

    #ifdef MAP_ABSOLUTE_SIZES
    map *= vec2(2, -2) / ScreenSize;
    #else
    map *= vec2(1, -ProjMat[1][1]/ProjMat[0][0]);
    #endif

    gl_Position = vec4(map + vec2(isRight? 1 : -1, 1), MAP_DEPTH * (1 + 0.0001 * (Color.r-0.5)), 1);
    vertexColor = vec4(1);
    
    sphericalVertexDistance = 0;
    cylindricalVertexDistance = 0;
}
else if (texSize == vec2(256) && round(testColor.a * 255) == 3 && ((idTex & 0xffff) == 0x0300)) //Round border
{
#ifndef GL_ARB_shader_draw_parameters //Checking color if GPU doesn't have extension
    idx = int(round(testColor.r * 255)) - 1;
    corner = corners[idx % 4];
#endif
    custom = 3;

    int meta = int(testColor.r*255-1) / 4;
    bool isRight = meta % 2 == 0;
    vec2 scaleData = round(texelFetch(Sampler0, uv + ivec2(1 - corner.x * 2, 0), 0).rg * 255) + 1;

    ivec2 stp = ivec2(round(UV0 * 256) - scaleData * corner);

    //R  = flags
    //GB = total length
    vec4 meta1 = round(texelFetch(Sampler0, stp + ivec2(2, 0), 0) * 255);
    
    //G = inner offset
    vec4 meta2 = round(texelFetch(Sampler0, stp + ivec2(3, 0), 0) * 255);
    float lenData = meta1.g * 0x100 + meta1.b;
    vec2 widthData = meta2.gb;
    int flags = int(meta1.r);

    int partCount = int(ceil(lenData / 256));
    float width = ((scaleData.y - 2) / partCount) - meta2.g;

    b_meta = vec3(lenData, widthData);

    uvCoord = (corner - 0.5) * (1 + width / 4.0) * (128.0 / MAP_CONTENT_SIZE);

    float angle = -Color.b * 2 * PI;

    if ((flags & FL_ROTATE) != 0)
        angle -= yaw;

    uvCoord = mat2_rotate_z(angle) * uvCoord;

    box = vec4(stp + vec2(0, 1), scaleData - vec2(0, 2));    

    vec2 map = (corner * (1 + width / 4.0) - width / 8.0) * MAP_SIZE;

    if (isRight)
        map = map + MAP_OFFSET * vec2(-1, 1) - vec2(MAP_SIZE.x, 0) + vec2(-1 /256.0 + (127 - MAP_CONTENT_SIZE) / 256.0) * vec2(1,-1) * MAP_SIZE;
    else
        map = map + MAP_OFFSET + vec2(1 /256.0 - (127 - MAP_CONTENT_SIZE) / 256.0) * MAP_SIZE;

    #ifdef MAP_ABSOLUTE_SIZES
    map *= vec2(2, -2) / ScreenSize;
    #else
    map *= vec2(1, -ProjMat[1][1]/ProjMat[0][0]);
    #endif

    gl_Position = vec4(map + vec2(isRight? 1 : -1, 1), MAP_DEPTH * (1 + 0.0001 * (Color.r-0.5)), 1);
    vertexColor = vec4(1);
    
    sphericalVertexDistance = 0;
    cylindricalVertexDistance = 0;
}
