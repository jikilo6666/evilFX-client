package dev.evilfx.config;

/** Screen anchor points. The offset always pushes the element inward from the anchored edge. */
public enum Anchor {
    TOP_LEFT(0f, 0f), TOP_CENTER(0.5f, 0f), TOP_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f), CENTER(0.5f, 0.5f), CENTER_RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f), BOTTOM_CENTER(0.5f, 1f), BOTTOM_RIGHT(1f, 1f);

    private final float fx;
    private final float fy;

    Anchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }

    public int resolveX(int screenW, int boxW, int offset) {
        int base = Math.round((screenW - boxW) * fx);
        return fx == 1f ? base - offset : base + offset;
    }

    public int resolveY(int screenH, int boxH, int offset) {
        int base = Math.round((screenH - boxH) * fy);
        return fy == 1f ? base - offset : base + offset;
    }
}
