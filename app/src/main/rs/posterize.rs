#pragma version(1)
#pragma rs java_package_name(com.mitteloupe.photostyle.renderscript)
#pragma rs_fp_full

int32_t posterizeValue;
float numOfAreas;
float numOfValues;

void setParams(int32_t value) {
    posterizeValue = value;

    float floatValue = (float)posterizeValue;
    numOfAreas = 256.0f / floatValue;
    numOfValues = 255.0f / (floatValue - 1.f);
}

uchar4 RS_KERNEL posterize(uchar4 in, uint32_t x, uint32_t y) {
    uchar4 out = in;

    float redAreaFloat = (float)in.r / numOfAreas;
    float newRed = (int32_t)redAreaFloat * numOfValues;
    float greenAreaFloat = (float)in.g / numOfAreas;
    float newGreen = (int32_t)greenAreaFloat * numOfValues;
    float blueAreaFloat = (float)in.b / numOfAreas;
    float newBlue = (int32_t)blueAreaFloat * numOfValues;

    out.r = (int32_t)newRed;
    out.g = (int32_t)newGreen;
    out.b = (int32_t)newBlue;

    return out;

/*
    uchar4 refpix = rsGetElementAt_uchar4(inputImage, x, y);
    float pixelIntensity = dot(rsUnpackColor8888(refpix).rgb, mono);
    if ((pixelIntensity <= intensityHigh) && (pixelIntensity >= intensityLow)) {
        return color;
    } else {
        return v_in;
    }
    */
}