#pragma version(1)
#pragma rs java_package_name(com.mitteloupe.photostyle.renderscript)
#pragma rs_fp_full

#include "colorDistance.rsh"
#include "palette.rsh"

rs_allocation errorMap;

void prepare() {
    preparePalette();
}

uchar4 RS_KERNEL initErrorMap(uchar4 in, uint32_t x, uint32_t y) {
    float4 color = rsUnpackColor8888(in);
    rsSetElementAt_float4(errorMap, color, x, y);
    return in;
}

uchar4 RS_KERNEL applyColor(uchar4 in, uint32_t x, uint32_t y) {
    float4 colorFloat4 = rsGetElementAt_float4(errorMap, x, y);
    return rsPackColorTo8888(colorFloat4);
}

void calculateError(rs_allocation source) {
    uint32_t width = rsAllocationGetDimX(source);
    uint32_t height = rsAllocationGetDimY(source);

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
            newColorFloat = findClosestPaletteColor(colorFloat, paletteFloats);

            if (x + 1 < width) {
                pixel1 = rsGetElementAt_float4(errorMap, x + 1, y);
            }
            if (y + 1 < height) {
                pixel2 = rsGetElementAt_float4(errorMap, x, y + 1);

                if (x - 1 > 0) {
                    pixel3 = rsGetElementAt_float4(errorMap, x - 1, y + 1);

                    if (x + 1 < width) {
                        pixel4 = rsGetElementAt_float4(errorMap, x + 1, y + 1);
                    }
                }
            }

            for (int channel = 0; channel < 3; channel++) {
                quantError = colorFloat[channel] - newColorFloat[channel];
                colorFloat[channel] = newColorFloat[channel];

                if (x + 1 < width) {
                    correction1 = (quantError * 7.f) / 16.f;
                    pixel1[channel] += correction1;
                }
                if (y + 1 < height) {
                    correction2 = (quantError * 5.f) / 16.f;
                    pixel2[channel] += correction2;

                    if (x - 1 > 0) {
                        correction3 = (quantError * 3.f) / 16.f;
                        pixel3[channel] += correction3;

                        if (x + 1 < width) {
                            correction4 = quantError / 16.f;
                            pixel4[channel] += correction4;
                        }
                    }
                }
            }

            rsSetElementAt_float4(errorMap, colorFloat, x, y);

            if (x + 1 < width) {
                rsSetElementAt_float4(errorMap, pixel1, x + 1, y);
            }
            if (y + 1 < height) {
                rsSetElementAt_float4(errorMap, pixel2, x, y + 1);

                if (x - 1 > 0) {
                    rsSetElementAt_float4(errorMap, pixel3, x - 1, y + 1);

                    if (x + 1 < width) {
                        rsSetElementAt_float4(errorMap, pixel4, x + 1, y + 1);
                    }
                }
            }
        }
    }
}