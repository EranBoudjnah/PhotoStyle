rs_allocation palette;
float4 paletteFloats[4096];
uint paletteSize;

void preparePalette() {
    paletteSize = rsAllocationGetDimX(palette);
    for (uint i = 0; i < paletteSize; i++) {
        paletteFloats[i] = rsUnpackColor8888(rsGetElementAt_uchar4(palette, i));
        paletteFloats[i].a = 1.f;
    }
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
