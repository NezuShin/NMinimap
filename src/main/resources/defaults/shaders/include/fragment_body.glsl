#ifndef PI
#define PI 3.14159
#endif

if (
    (custom == 1 && (uvCoord.x < 1.0 || uvCoord.y < 1.0 || uvCoord.x > MAP_CONTENT_SIZE || uvCoord.y > MAP_CONTENT_SIZE)) ||
    (custom == 2 && length(uvCoord - 1 - MAP_CROP_RADIUS) > MAP_CROP_RADIUS)
)
    discard;
else if (custom == 3) //Round border
{
    int partCount = int(ceil(b_meta.x / 256));
    float width = ((box.w) / (partCount + 1));
    vec2 normCoords = uvCoord;
    vec2 coords = vec2(atan(-normCoords.x, normCoords.y) / PI * 0.5 + 0.5, 16 + (width+1 - b_meta.y)/4.0 - length(normCoords) * 32);

    if (coords.y < 0 || coords.y > 1)
        discard;

    float part = floor(coords.x * b_meta.x / 256);
    float inpart = fract(coords.x * b_meta.x / 256);

    color = texelFetch(Sampler0, ivec2(box.xy
        + vec2(inpart, coords.y) * vec2(box.z, (width + 1))
        + vec2(0, part * (width + 2))
    ), 0);
}
else if (custom == 4) //Square border
{
    vec2 uv = box.xy;

    float crop = (127 - MAP_CONTENT_SIZE) / (2 + 7/128.0);

    if (uvCoord.x < 0.5)
        uv.x += uvCoord.x * box.z - crop;
    else
        uv.x += (uvCoord.x - 0.5) * box.z + box.z * 0.5 + crop;

    if (uvCoord.y < 0.5)
        uv.y += uvCoord.y * box.w - crop;
    else
        uv.y += (uvCoord.y - 0.5) * box.w + box.w * 0.5 + crop;

    if (uv != clamp(uv, box.xy, box.xy + box.zw))
        discard;

    color = texelFetch(Sampler0, ivec2(uv), 0);
}