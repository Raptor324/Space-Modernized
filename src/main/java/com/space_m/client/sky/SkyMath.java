package com.space_m.client.sky;

/** Die Rechenhelfer aus {@code BobMathUtil}/{@code Library}, die der Himmel benutzt (1:1). */
public final class SkyMath {
    private SkyMath() {}

    /** {@code BobMathUtil.lerp(delta, start, end)}. */
    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    /** {@code BobMathUtil.clerp}: Winkel-Lerp ueber den kuerzeren Weg (Grad). */
    public static double clerp(double delta, double start, double end) {
        double angle = ((((end - start) % 360) + 540) % 360) - 180;
        return start + angle * delta;
    }

    /** {@code BobMathUtil.clampedLerp(start, end, delta)}. */
    public static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0D) return start;
        if (delta > 1.0D) return end;
        return lerp(delta, start, end);
    }

    /** {@code BobMathUtil.remap01_clamp(num, min, max)}. */
    public static float remap01_clamp(float num, float min1, float max1) {
        float v = (num - min1) / (max1 - min1);
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /** {@code Library.smoothstep(t, edge0, edge1)}. */
    public static double smoothstep(double t, double edge0, double edge1) {
        t = Math.max(0.0, Math.min(1.0, (t - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }
}
