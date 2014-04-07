int getValue(float p0, float p1, float p2, float p3, float x) {
    return p1 + 0.5 * x*(p2 - p0 + x*(2.0*p0 - 5.0*p1 + 4.0*p2 - p3 + x*(3.0*(p1 - p2) + p3 - p0)));    
}

int interpolate(const float2 coords, read_only image2d_t image) {
    const float ix = floor(coords.x);
    const float dx = coords.x - ix;
    
    const float iy = floor(coords.y);
    const float dy = coords.y - iy;            
    
    float arr0 = getValue((float2)(ix-1, iy-1)).x, (float2)(ix, iy-1)).x, (float2)(ix+1, iy-1)).x, (float2)(ix+2, iy-1)).x, dy);
    float arr1 = getValue((float2)(ix-1, iy)).x, (float2)(ix, iy)).x, (float2)(ix+1, iy)).x, (float2)(ix+2, iy)).x, dy);
    float arr2 = getValue((float2)(ix-1, iy+1)).x, (float2)(ix, iy+1)).x, (float2)(ix+1, iy+1)).x, (float2)(ix+2, iy+1)).x, dy);
    float arr3 = getValue((float2)(ix-1, iy+2)).x, (float2)(ix, iy+2)).x, (float2)(ix+1, iy+2)).x, (float2)(ix+2, iy+2)).x, dy);
    
    return getValue(arr0, arr1, arr2, arr3, dx);
}