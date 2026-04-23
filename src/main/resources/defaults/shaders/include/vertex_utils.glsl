#define PI 3.14159

int id(vec4 color) {
    color = round(color * 255);
    return int(color.r * 0x10000 + color.g * 0x100 + color.b);
}

int id(ivec2 uv)
{
    return id(texelFetch(Sampler0, uv, 0));
}

mat2 mat2_rotate_z(float radians) {
    return mat2(
        cos(radians), -sin(radians),
        sin(radians), cos(radians)
    );
}
