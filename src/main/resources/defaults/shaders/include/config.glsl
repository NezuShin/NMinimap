// #define MAP_SIZE vec2(0.364, 0.364)
// #define MAP_OFFSET vec2(0.05025, 0.05025)

#define MAP_ABSOLUTE_SIZES
#define MAP_OFFSET vec2({offset-x}, {offset-y})
#define MAP_SIZE vec2(128*{scale})

#define MAP_CONTENT_SIZE {content}
#define MAP_CROP_RADIUS (MAP_CONTENT_SIZE/2.0)
