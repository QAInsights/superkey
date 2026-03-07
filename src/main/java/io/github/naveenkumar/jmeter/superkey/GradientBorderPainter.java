package io.github.naveenkumar.jmeter.superkey;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Shared utility that paints the spinning gradient border effect.
 *
 * <p>
 * Used by both the OSS {@code AnimatedBorderPanel} and the Pro
 * {@code StyledDialogPanel} to avoid duplicating the gradient logic.
 *
 * <p>
 * This class is stateless — callers manage their own animation timers
 * and pass the current {@code angle} and {@code alpha} on each frame.
 */
public final class GradientBorderPainter {

    /** Google-inspired gradient colours. */
    private static final Color[] COLORS = {
            new Color(66, 133, 244), // blue
            new Color(234, 67, 53), // red
            new Color(251, 188, 5), // yellow
            new Color(52, 168, 83), // green
            new Color(66, 133, 244), // blue (wrap)
    };
    private static final float[] FRACTIONS = { 0f, 0.25f, 0.5f, 0.75f, 1f };

    /** Animation timing constants (shared so both panels behave identically). */
    public static final int ANIM_DURATION = 1500; // 1.5 s spin
    public static final int ANIM_FADE = 1000; // 1 s fade-out
    public static final float ANGLE_STEP = 0.15f;

    private GradientBorderPainter() {
        /* utility */ }

    /**
     * Paints a spinning gradient border.
     *
     * @param g2    graphics context (caller must set antialiasing)
     * @param angle current rotation angle in radians
     * @param alpha current opacity (1.0 = fully opaque, 0 = invisible)
     * @param x     left edge of the border rectangle
     * @param y     top edge of the border rectangle
     * @param w     width of the border rectangle
     * @param h     height of the border rectangle
     * @param arc   corner arc (0 → sharp rectangle)
     */
    public static void paint(Graphics2D g2, float angle, float alpha,
            int x, int y, int w, int h, int arc) {
        if (alpha <= 0f)
            return;

        Composite origComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float r = (float) Math.hypot(cx, cy);

        float x1 = cx + (float) (Math.cos(angle) * r);
        float y1 = cy + (float) (Math.sin(angle) * r);
        float x2 = cx + (float) (Math.cos(angle + Math.PI) * r);
        float y2 = cy + (float) (Math.sin(angle + Math.PI) * r);

        LinearGradientPaint paint = new LinearGradientPaint(
                new Point2D.Float(x1, y1),
                new Point2D.Float(x2, y2),
                FRACTIONS, COLORS);
        g2.setPaint(paint);
        g2.setStroke(new BasicStroke(6.0f));

        if (arc <= 0) {
            g2.drawRect(x, y, w, h);
        } else {
            g2.drawRoundRect(x, y, w, h, arc, arc);
        }

        g2.setComposite(origComposite);
    }

    /**
     * Computes the fade-out alpha for the given elapsed time.
     *
     * @param elapsed milliseconds since animation start
     * @return alpha in [0, 1], or -1 if the animation is finished
     */
    public static float computeAlpha(long elapsed) {
        if (elapsed > ANIM_DURATION + ANIM_FADE)
            return -1f;
        if (elapsed <= ANIM_DURATION)
            return 1f;
        float a = 1f - ((float) (elapsed - ANIM_DURATION) / ANIM_FADE);
        return Math.max(0f, Math.min(1f, a));
    }
}
