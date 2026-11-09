package com.emqnuele.quickresourcepack.core.logic;

/** Radial menu maths. Item 0 sits at twelve o'clock and items go clockwise. */
public final class WheelGeometry {
    private WheelGeometry() {
    }

    /** Angle in radians of item {@code index}'s centre, screen space (y down). */
    public static double angleOf(int index, int count) {
        return -Math.PI / 2 + (Math.PI * 2 * index) / count;
    }

    /**
     * Which item the pointer at offset ({@code dx}, {@code dy}) from the centre selects.
     *
     * @return the index, or -1 inside the dead zone (meaning "cancel")
     */
    public static int indexAt(double dx, double dy, int count, double deadZone) {
        if (count <= 0 || Math.hypot(dx, dy) < deadZone) {
            return -1;
        }
        double sector = Math.PI * 2 / count;
        // shift by half a sector so each item owns the wedge centred on it
        double angle = Math.atan2(dy, dx) + Math.PI / 2 + sector / 2;
        double normalized = ((angle % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
        return Math.min(count - 1, (int) (normalized / sector));
    }

    /** Ring radius that keeps {@code count} icons of {@code itemSize} from overlapping. */
    public static double radiusFor(int count, double itemSize, double minRadius, double gap) {
        if (count <= 1) {
            return minRadius;
        }
        double needed = (itemSize + gap) / (2 * Math.sin(Math.PI / count));
        return Math.max(minRadius, needed);
    }
}
