const vec2 corners[] = vec2[](vec2(0, 0), vec2(0, 1), vec2(1, 1), vec2(1, 0));
vec2 texSize = textureSize(Sampler0, 0);
vec4 tex = round(texture(Sampler0, UV0 - corners[(gl_VertexID) % 4] * vec2(0.5, 0.5) / texSize) * 255);
int idTex = id(ivec2(UV0 * texSize));

custom = 0;

vec3 local = transpose(mat3(ModelViewMat)) * vec3(1, 0, 0);
float yaw = atan(local.z, local.x);
mat2 rotAngle = mat2(1, 0, 0, 1);

if (texSize == vec2(128) && id(ivec2(0)) == 0xFF0000 && id(ivec2(1, 0)) == 0x597D27 && id(ivec2(2, 0)) == 0x3737DC) //Map
{
    int meta = id(ivec2(3, 0));
    bool isRight = meta == 0x3F6EDC || meta == 0x006A00;
    bool isRound = meta == 0x006A00 || meta == 0x5E2872;
    if (isRound)
        rotAngle = mat2_rotate_z(yaw);

    vec2 map = rotAngle * (corners[(gl_VertexID + 1) % 4] - 0.5) + 0.5;
    map *= MAP_SIZE;

    if (isRight)
        map = map + MAP_OFFSET * vec2(-1, 1) - vec2(MAP_SIZE.x, 0);
    else
        map = map + MAP_OFFSET;
    gl_Position = vec4(vec2(1, -ProjMat[1][1]/ProjMat[0][0]) * map + vec2(isRight? 1 : -1, 1), -0.9999, 1);
    vertexColor = vec4(1);
    custom = isRound ? 2 : 1;

    sphericalVertexDistance = 0;
    cylindricalVertexDistance = 0;
}
else if (texSize == vec2(256) && tex.a == 3 && ((idTex & 0xffff) == 0x0100)) //Markers
{
    bool isRight = idTex == 0x010100 || idTex == 0x030100;
    bool isRound = idTex == 0x030100 || idTex == 0x040100;
    
    vec2 pos = Color.rg;
    if (isRound)
    {
        rotAngle = mat2_rotate_z(yaw);
        pos -= 0.5;
        pos = normalize(pos) * clamp(length(pos), 0, 0.5) + 0.5;
    }

    float angle = -Color.b * 2 * PI;

    vec2 map = rotAngle * (mat2_rotate_z(angle) * (corners[gl_VertexID % 4] - 0.5) / 64 * 5 + pos - 0.5) + 0.5;
    map *= MAP_SIZE;

    if (isRight)
        map = map + MAP_OFFSET * vec2(-1, 1) - vec2(MAP_SIZE.x, 0);
    else
        map = map + MAP_OFFSET;

    gl_Position = vec4(vec2(1, -ProjMat[1][1]/ProjMat[0][0]) * map + vec2(isRight? 1 : -1, 1), -1, 1);
    vertexColor = vec4(1);
    
    sphericalVertexDistance = 0;
    cylindricalVertexDistance = 0;
}
