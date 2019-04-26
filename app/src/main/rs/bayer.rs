#pragma version(1)
#pragma rs java_package_name(com.mitteloupe.photostyle.renderscript)
#pragma rs_fp_full

float bayerMatrix[64] = {
     0.f, 48.f, 12.f, 60.f,  3.f, 51.f, 15.f, 63.f,
    32.f, 16.f, 44.f, 28.f, 35.f, 19.f, 47.f, 31.f,
     8.f, 56.f,  4.f, 52.f, 11.f, 59.f,  7.f, 55.f,
    40.f, 24.f, 36.f, 20.f, 43.f, 27.f, 39.f, 23.f,
     2.f, 50.f, 14.f, 62.f,  1.f, 49.f, 13.f, 61.f,
    34.f, 18.f, 46.f, 30.f, 33.f, 17.f, 45.f, 29.f,
    10.f, 58.f,  6.f, 54.f,  9.f, 57.f,  5.f, 53.f,
    42.f, 26.f, 38.f, 22.f, 41.f, 25.f, 37.f, 21.f
};

rs_allocation palette;
float4 paletteFloats[256];
uint paletteSize;
float scale;

void prepare(uint inputPaletteSize) {
    paletteSize = inputPaletteSize;
    for (uint i = 0; i < paletteSize; i++) {
        paletteFloats[i] = rsUnpackColor8888(rsGetElementAt_uchar4(palette, i));
        paletteFloats[i].a = 1.f;
    }

    scale = 256.f / (float)paletteSize;
}

static float colorCCIR601Distance(float4 a, float4 b) {
    float luma1 = a.r * 299.f + a.g * 587.f + a.b * 114.f;
    float luma2 = b.r * 299.f + b.g * 587.f + b.b * 114.f;
    float lumaDifference = (luma1 - luma2) / 1000.f;

    float redDifference = a.r - b.r;
    float greenDifference = a.g - b.g;
    float blueDifference = a.b - b.b;
    return (redDifference * redDifference * 0.299f +
            greenDifference * greenDifference * 0.587f +
            blueDifference * blueDifference * 0.114f) * 0.75f +
            lumaDifference * lumaDifference;
}

static float4 findClosestPaletteColor(float4 color, float4 paletteFloats[]) {
    float4 minColor = paletteFloats[0];
    float minDistance = colorCCIR601Distance(color, minColor);

    for (uint i = 1; i < paletteSize; i++) {
        float4 currentColor = paletteFloats[i];
        float colorDistance = colorCCIR601Distance(color, currentColor);
        if (colorDistance < minDistance) {
            minDistance = colorDistance;
            minColor = currentColor;
        }
    }

    return minColor;
}

uchar4 RS_KERNEL bayer(uchar4 in, uint32_t x, uint32_t y) {
    float ditherMapValue = bayerMatrix[x % 8 + (y % 8) * 8] / 64.f - 0.5f;
    float scaledDitherValue = ditherMapValue * scale;
    float4 colorFloat = rsUnpackColor8888(in);
    colorFloat += scaledDitherValue / 255.f;
    if (colorFloat.r > 1.f) colorFloat.r = 1.f;
    if (colorFloat.g > 1.f) colorFloat.g = 1.f;
    if (colorFloat.b > 1.f) colorFloat.b = 1.f;
    return rsPackColorTo8888(findClosestPaletteColor(colorFloat, paletteFloats));
}
