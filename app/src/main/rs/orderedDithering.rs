#pragma version(1)
#pragma rs java_package_name(com.mitteloupe.photostyle.renderscript)
#pragma rs_fp_full

#include "colorDistance.rsh"
#include "palette.rsh"

static float ditheringMatrix[64];

float scale;

const int PATTERN_BAYER = 0;
const int PATTERN_HALFTONE = 1;
const int PATTERN_STRIPS = 2;
const int PATTERN_LINEAR = 3;

typedef enum {
    RS_ORDERED_DITHERING_BAYER = PATTERN_BAYER,
    RS_ORDERED_DITHERING_HALFTONE = PATTERN_HALFTONE,
    RS_ORDERED_DITHERING_STRIPS = PATTERN_STRIPS,
    RS_ORDERED_DITHERING_LINEAR = PATTERN_LINEAR
} rs_ordered_dithering_pattern;

static void initBayer() {
    float ditheringMatrixData[64] = {
         0.f, 48.f, 12.f, 60.f,  3.f, 51.f, 15.f, 63.f,
        32.f, 16.f, 44.f, 28.f, 35.f, 19.f, 47.f, 31.f,
         8.f, 56.f,  4.f, 52.f, 11.f, 59.f,  7.f, 55.f,
        40.f, 24.f, 36.f, 20.f, 43.f, 27.f, 39.f, 23.f,
         2.f, 50.f, 14.f, 62.f,  1.f, 49.f, 13.f, 61.f,
        34.f, 18.f, 46.f, 30.f, 33.f, 17.f, 45.f, 29.f,
        10.f, 58.f,  6.f, 54.f,  9.f, 57.f,  5.f, 53.f,
        42.f, 26.f, 38.f, 22.f, 41.f, 25.f, 37.f, 21.f
    };
    for (uint i = 0; i < 64; i++) {
        ditheringMatrix[i] = ditheringMatrixData[i];
    }
}

static void initHalftone() {
    float ditheringMatrixData[64] = {
        24.f, 10.f, 12.f, 26.f, 35.f, 47.f, 49.f, 37.f,
         8.f,  0.f,  2.f, 14.f, 45.f, 59.f, 61.f, 51.f,
        22.f,  6.f,  4.f, 16.f, 43.f, 57.f, 63.f, 53.f,
        30.f, 20.f, 18.f, 28.f, 33.f, 41.f, 55.f, 39.f,
        34.f, 46.f, 48.f, 36.f, 25.f, 11.f, 13.f, 27.f,
        44.f, 58.f, 60.f, 50.f,  9.f,  1.f,  3.f, 15.f,
        42.f, 56.f, 62.f, 52.f, 23.f,  7.f,  5.f, 17.f,
        32.f, 40.f, 54.f, 38.f, 31.f, 21.f, 19.f, 29.f
    };
    for (uint i = 0; i < 64; i++) {
        ditheringMatrix[i] = ditheringMatrixData[i];
    }
}

static void initStrips() {
    float ditheringMatrixData[64] = {
         8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,
        24.f, 24.f, 24.f, 24.f, 24.f, 24.f, 24.f, 24.f,
        32.f, 32.f, 32.f, 32.f, 32.f, 32.f, 32.f, 32.f,
        64.f, 64.f, 64.f, 64.f, 64.f, 64.f, 64.f, 64.f,
         8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,
        24.f, 24.f, 24.f, 24.f, 24.f, 24.f, 24.f, 24.f,
        32.f, 32.f, 32.f, 32.f, 32.f, 32.f, 32.f, 32.f,
        64.f, 64.f, 64.f, 64.f, 64.f, 64.f, 64.f, 64.f
    };
    for (uint i = 0; i < 64; i++) {
        ditheringMatrix[i] = ditheringMatrixData[i];
    }
}

static void initLinear() {
    float ditheringMatrixData[64] = {
         8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,
        22.f, 22.f, 24.f, 23.f, 24.f, 24.f, 23.f, 24.f,
         8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,
        24.f, 24.f, 23.f, 24.f, 22.f, 22.f, 24.f, 23.f,
         8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,
        23.f, 24.f, 22.f, 22.f, 24.f, 23.f, 24.f, 24.f,
         8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,  8.f,
        24.f, 23.f, 24.f, 24.f, 23.f, 24.f, 22.f, 22.f,
    };
    for (uint i = 0; i < 64; i++) {
        ditheringMatrix[i] = ditheringMatrixData[i];
    }
}

void prepare(rs_ordered_dithering_pattern pattern) {
    preparePalette();

    if (pattern == RS_ORDERED_DITHERING_HALFTONE) {
        initHalftone();
    } else if (pattern == RS_ORDERED_DITHERING_STRIPS) {
        initStrips();
    } else if (pattern == RS_ORDERED_DITHERING_LINEAR) {
        initLinear();
    } else {
        initBayer();
    }

    scale = 256.f / (float)paletteSize;
}

uchar4 RS_KERNEL bayer(uchar4 in, uint32_t x, uint32_t y) {
    float ditherMapValue = ditheringMatrix[x % 8 + (y % 8) * 8] / 64.f - 0.5f;
    float scaledDitherValue = ditherMapValue * scale;
    float4 colorFloat = rsUnpackColor8888(in);
    colorFloat += scaledDitherValue / 255.f;
    if (colorFloat.r > 1.f) colorFloat.r = 1.f;
    if (colorFloat.g > 1.f) colorFloat.g = 1.f;
    if (colorFloat.b > 1.f) colorFloat.b = 1.f;
    return rsPackColorTo8888(findClosestPaletteColor(colorFloat, paletteFloats));
}
