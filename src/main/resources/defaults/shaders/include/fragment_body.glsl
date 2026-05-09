if (
    (custom == 1 && (uvCoord.x < 1.0 || uvCoord.y < 1.0)) ||
    (custom == 2 && length(uvCoord - 1 - MAP_CROP_RADIUS) > MAP_CROP_RADIUS)
)
    discard;
