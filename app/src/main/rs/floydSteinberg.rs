#pragma version(1)
#pragma rs java_package_name(com.mitteloupe.photostyle.renderscript)
#pragma rs_fp_full

rs_allocation errorMap;
rs_allocation palette;
float4 paletteFloats[256];
uint paletteSize;

void prepare(uint inputPaletteSize) {
    paletteSize = inputPaletteSize;
    for (uint i = 0; i < paletteSize; i++) {
        paletteFloats[i] = rsUnpackColor8888(rsGetElementAt_uchar4(palette, i));
        paletteFloats[i].a = 1.f;
    }
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

uchar4 RS_KERNEL applyColor(uchar4 in, uint32_t x, uint32_t y) {
    float4 colorFloat4 = rsGetElementAt_float4(errorMap, x, y);
    return rsPackColorTo8888(colorFloat4);
//    float4 colorFloat = rsUnpackColor8888((uchar4){ red, green, blue, 255 });
//    return rsPackColorTo8888(findClosestPaletteColor(colorFloat, paletteFloats));
}

void calculateError(rs_allocation source) {
    uint32_t width = rsAllocationGetDimX(source);
    uint32_t height = rsAllocationGetDimY(source);

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            float4 color = rsUnpackColor8888(rsGetElementAt_uchar4(source, x, y));
            rsSetElementAt_float4(errorMap, color, x, y);
        }
    }

    float4 colorFloat;
    float4 newColorFloat;
    float quantError;
    float correction1;
    float4 pixel1;
    float correction2;
    float4 pixel2;
    float correction3;
    float4 pixel3;
    float correction4;
    float4 pixel4;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            colorFloat = rsGetElementAt_float4(errorMap, x, y);
            newColorFloat = findClosestPaletteColor((float4){ colorFloat.r, colorFloat.g, colorFloat.b, 1.f }, paletteFloats);

            for (int channel = 0; channel < 3; channel++) {
                quantError = colorFloat[channel] - newColorFloat[channel];
                colorFloat[channel] = newColorFloat[channel];

                if (x + 1 < width) {
                    correction1 = (quantError * 7.f) / 16.f;
                    pixel1 = rsGetElementAt_float4(errorMap, x + 1, y);
                    pixel1[channel] += correction1;
                    rsSetElementAt_float4(errorMap, pixel1, x + 1, y);
                }
                if (y + 1 < height) {
                    correction2 = (quantError * 5.f) / 16.f;
                    pixel2 = rsGetElementAt_float4(errorMap, x, y + 1);
                    pixel2[channel] += correction2;
                    rsSetElementAt_float4(errorMap, pixel2, x, y + 1);

                    if (x - 1 > 0) {
                        correction3 = (quantError * 3.f) / 16.f;
                        pixel3 = rsGetElementAt_float4(errorMap, x - 1, y + 1);
                        pixel3[channel] += correction3;
                        rsSetElementAt_float4(errorMap, pixel3, x - 1, y + 1);

                        if (x + 1 < width) {
                            correction4 = quantError / 16.f;
                            pixel4 = rsGetElementAt_float4(errorMap, x + 1, y + 1);
                            pixel4[channel] += correction4;
                            rsSetElementAt_float4(errorMap, pixel4, x + 1, y + 1);
                        }
                    }
                }
            }

            rsSetElementAt_float4(errorMap, colorFloat, x, y);
        }
    }
}