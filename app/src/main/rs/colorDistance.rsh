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
