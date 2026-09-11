if (
    (custom == 1 && (uvCoord.x < 1.0 || uvCoord.y < 1.0 || uvCoord.x > MAP_CONTENT_SIZE || uvCoord.y > MAP_CONTENT_SIZE)) ||
    (custom == 2 && length(uvCoord - 1 - MAP_CROP_RADIUS) > MAP_CROP_RADIUS)
)
    discard;
