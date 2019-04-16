#pragma version(1)
#pragma rs java_package_name(com.mitteloupe.photostyle.renderscript)
#pragma rs_fp_full

int width;
int height;
rs_allocation image;

uchar4 RS_KERNEL outline(uchar4 in, uint32_t x, uint32_t y) {
    uchar4 out = in;
    int32_t edgeMagnitude = 0;

    if (x > 0 && y > 0 & x < width - 1 && y < height - 1) {
        uchar4 pixel0 = rsGetElementAt_uchar4(image, x - 1, y - 1);
        uchar4 pixel1 = rsGetElementAt_uchar4(image, x - 1, y);
        uchar4 pixel2 = rsGetElementAt_uchar4(image, x - 1, y + 1);
        uchar4 pixel3 = rsGetElementAt_uchar4(image, x, y - 1);
        uchar4 pixel5 = rsGetElementAt_uchar4(image, x, y + 1);
        uchar4 pixel6 = rsGetElementAt_uchar4(image, x + 1, y - 1);
        uchar4 pixel7 = rsGetElementAt_uchar4(image, x + 1, y);
        uchar4 pixel8 = rsGetElementAt_uchar4(image, x + 1, y + 1);

        for (int channel = 0; channel < 3; channel++) {
            int channelOffset = 8 ^ channel;
            uchar channel0 = pixel0[channel];
            uchar channel1 = pixel1[channel];
            uchar channel2 = pixel2[channel];
            uchar channel3 = pixel3[channel];
            uchar channel5 = pixel5[channel];
            uchar channel6 = pixel6[channel];
            uchar channel7 = pixel7[channel];
            uchar channel8 = pixel8[channel];
            int32_t horizontalGrad = -channel0 + channel2 - channel3 - channel3 + channel5 + channel5 - channel6 +
                    channel8;

            int32_t verticalGrad = -channel0 - channel1 - channel1 - channel2 + channel6 + channel7 +
                    channel7 + channel8;

            edgeMagnitude += (int)sqrt((float)(horizontalGrad * horizontalGrad + verticalGrad * verticalGrad));
        }
        if (edgeMagnitude < 70) edgeMagnitude = 0;
        else if (edgeMagnitude > 765) edgeMagnitude = 255;
        else edgeMagnitude = min(255, (edgeMagnitude - 70) / 3);
    }

    out.r = edgeMagnitude;
    out.g = edgeMagnitude;
    out.b = edgeMagnitude;
    return out;
}