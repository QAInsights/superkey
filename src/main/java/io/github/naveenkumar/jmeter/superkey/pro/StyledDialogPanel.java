package io.github.naveenkumar.jmeter.superkey.pro;

import io.github.naveenkumar.jmeter.superkey.GradientBorderPainter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * StyledDialogPanel — a {@link JPanel} that paints one of the three Pro
 * dialog styles defined by {@link DialogStyle}.
 *
 * <ul>
 * <li><b>SHARP</b> — hard 90° corners, flat dark background, 1 px solid
 * border</li>
 * <li><b>PILL</b> — fully-rounded oval, soft light gradient, macOS Spotlight
 * look</li>
 * <li><b>FLOATING_SHADOW</b> — rounded rect (arc=16) with a multi-layer
 * drop-shadow,
 * Linear / Notion command-palette aesthetic</li>
 * </ul>
 *
 * <p>
 * The {@link DialogStyle#SHARP} and {@link DialogStyle#PILL} styles are
 * fully opaque; {@link DialogStyle#FLOATING_SHADOW} requires the host dialog
 * to be non-opaque so the transparent shadow padding is visible.
 *
 * <p>
 * <b>Note:</b> This class lives in the {@code pro} package and is excluded
 * from the OSS JAR by the Maven {@code oss} build profile.
 */
public final class StyledDialogPanel extends JPanel {

    // -----------------------------------------------------------------------
    // LaF-aware colours — resolved once at construction time from UIManager
    // so the panel matches whichever JMeter theme is active.
    // -----------------------------------------------------------------------

    /** Primary background from the current Look and Feel. */
    private final Color bgBase;
    /** Slightly lighter variant for gradient top / highlights. */
    private final Color bgLighter;
    /** Slightly darker variant for gradient bottom. */
    private final Color bgDarker;
    /** Border colour derived from the LaF separator / control-shadow tone. */
    private final Color borderColor;

    /** Shadow tones for FLOATING_SHADOW (always semi-transparent black). */
    private static final Color[] SHADOW_COLORS = {
            new Color(0, 0, 0, 20),
            new Color(0, 0, 0, 50),
            new Color(0, 0, 0, 80),
            new Color(0, 0, 0, 100),
    };

    /**
     * Padding around the content area used to paint the shadow for FLOATING_SHADOW.
     */
    private static final int SHADOW_PAD = 12;

    // Corner arcs per style (for PILL we compute arc at paint time from height)
    private static final int ARC_SHARP = 0;
    private static final int ARC_FLOATING = 16;

    // -----------------------------------------------------------------------
    private final DialogStyle style;

    /**
     * Creates a new panel using the given {@link DialogStyle}.
     *
     * @param style the Pro dialog style to render (must not be {@code null})
     */
    public StyledDialogPanel(DialogStyle style) {
        super(new BorderLayout());
        this.style = style;

        // Resolve colours from the active LaF once.
        Color panelBg = javax.swing.UIManager.getColor("Panel.background");
        if (panelBg == null)
            panelBg = getBackground();
        this.bgBase = panelBg;
        this.bgLighter = brighter(panelBg, 15);
        this.bgDarker = darker(panelBg, 15);

        Color sep = javax.swing.UIManager.getColor("Separator.foreground");
        if (sep == null)
            sep = javax.swing.UIManager.getColor("controlShadow");
        if (sep == null)
            sep = Color.GRAY;
        this.borderColor = sep;

        setOpaque(false); // we paint our own background

        // Inner padding so child components stay inside the styled region
        int pad = (style == DialogStyle.FLOATING_SHADOW) ? SHADOW_PAD + 4 : 4;
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        // Animated gradient border — delegates to shared GradientBorderPainter
        animTimer = new javax.swing.Timer(30, e -> {
            long elapsed = System.currentTimeMillis() - animStartTime;
            if (elapsed > GradientBorderPainter.ANIM_DURATION + GradientBorderPainter.ANIM_FADE) {
                animTimer.stop();
                repaint();
                return;
            }
            animAngle += GradientBorderPainter.ANGLE_STEP;
            if (animAngle > Math.PI * 2)
                animAngle -= Math.PI * 2;
            repaint();
        });
    }

    // -----------------------------------------------------------------------
    // Animated gradient border (state only — painting delegated to
    // GradientBorderPainter)
    // -----------------------------------------------------------------------

    private float animAngle = 0;
    private long animStartTime;
    private javax.swing.Timer animTimer;

    @Override
    public void addNotify() {
        super.addNotify();
        animStartTime = System.currentTimeMillis();
        animTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        animTimer.stop();
    }

    /** Brighten a colour by the given amount (0-255). */
    private static Color brighter(Color c, int amount) {
        return new Color(
                Math.min(255, c.getRed() + amount),
                Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount));
    }

    /** Darken a colour by the given amount (0-255). */
    private static Color darker(Color c, int amount) {
        return new Color(
                Math.max(0, c.getRed() - amount),
                Math.max(0, c.getGreen() - amount),
                Math.max(0, c.getBlue() - amount));
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            switch (style) {
                case SHARP -> paintSharp(g2);
                case PILL -> paintPill(g2);
                case FLOATING_SHADOW -> paintFloatingShadow(g2);
                default -> paintSharp(g2); // safety — treat unknown style as sharp
            }

            // Animated gradient border overlay (same Google-colours spin as OSS)
            paintAnimatedBorder(g2);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the spinning gradient border, delegating to
     * {@link GradientBorderPainter}. Only the shape parameters
     * (inset, arc) are style-specific.
     */
    private void paintAnimatedBorder(Graphics2D g2) {
        long elapsed = System.currentTimeMillis() - animStartTime;
        float alpha = GradientBorderPainter.computeAlpha(elapsed);
        if (alpha <= 0f)
            return;

        int w = getWidth();
        int h = getHeight();

        // Compute style-specific border rectangle + arc
        int bx, by, bw, bh, arc;
        switch (style) {
            case SHARP -> {
                bx = 2;
                by = 2;
                bw = w - 4;
                bh = h - 4;
                arc = 0;
            }
            case PILL -> {
                bx = 2;
                by = 2;
                bw = w - 4;
                bh = h - 4;
                arc = (h <= 60) ? h : 24;
            }
            case FLOATING_SHADOW -> {
                int p = SHADOW_PAD;
                bx = p + 2;
                by = p + 2;
                bw = w - 2 * p - 4;
                bh = h - 2 * p - 4;
                arc = ARC_FLOATING;
            }
            default -> {
                bx = 2;
                by = 2;
                bw = w - 4;
                bh = h - 4;
                arc = 20;
            }
        }

        GradientBorderPainter.paint(g2, animAngle, alpha, bx, by, bw, bh, arc);
    }

    // -----------------------------------------------------------------------
    // Style: SHARP — flat rectangle, hard corners
    // -----------------------------------------------------------------------

    private void paintSharp(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();

        // Solid fill
        g2.setColor(bgBase);
        g2.fillRect(0, 0, w, h);

        // 1 px border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRect(0, 0, w - 1, h - 1);
    }

    // -----------------------------------------------------------------------
    // Style: PILL — fully-rounded oval, gradient fill
    // -----------------------------------------------------------------------

    private void paintPill(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        // Collapsed (search bar only): true pill (arc = height).
        // Expanded (results visible): softly rounded rect so the results
        // area doesn't become a 300-px tall oval.
        int arc = (h <= 60) ? h : 24;

        // Gradient fill — top to bottom
        GradientPaint gradient = new GradientPaint(0, 0, bgLighter, 0, h, bgDarker);
        g2.setPaint(gradient);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        // Subtle inner highlight on the top edge (gives depth)
        g2.setPaint(new GradientPaint(
                0, 0, new Color(255, 255, 255, 18),
                0, h / 3, new Color(255, 255, 255, 0)));
        g2.fillRoundRect(1, 1, w - 2, h / 2, arc, arc);

        // Border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
    }

    // -----------------------------------------------------------------------
    // Style: FLOATING_SHADOW — rounded rect with multi-layer drop shadow
    // -----------------------------------------------------------------------

    private void paintFloatingShadow(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        int pad = SHADOW_PAD;
        int arc = ARC_FLOATING;

        // Paint layered shadows — each layer is inset by 1 px from the edge
        for (int i = 0; i < SHADOW_COLORS.length; i++) {
            int offset = SHADOW_COLORS.length - i;
            g2.setColor(SHADOW_COLORS[i]);
            g2.fillRoundRect(
                    pad - offset,
                    pad - offset + 3, // slight downward bias (elevation feel)
                    w - 2 * pad + 2 * offset,
                    h - 2 * pad + 2 * offset,
                    arc + 4,
                    arc + 4);
        }

        // Main card background
        g2.setColor(bgBase);
        g2.fillRoundRect(pad, pad, w - 2 * pad, h - 2 * pad, arc, arc);

        // Subtle top-edge specular highlight
        g2.setPaint(new GradientPaint(
                pad, pad, new Color(255, 255, 255, 15),
                pad, pad + 12, new Color(255, 255, 255, 0)));
        g2.fillRoundRect(pad + 1, pad + 1, w - 2 * pad - 2, 20, arc, arc);

        // Border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(pad, pad, w - 2 * pad - 1, h - 2 * pad - 1, arc, arc);
    }

    // -----------------------------------------------------------------------
    // Helper: expose the window shape clip for the host JDialog
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link java.awt.Shape} that should be applied to the host
     * {@code JDialog} via {@code setShape()} for this style.
     *
     * <p>
     * For {@link DialogStyle#FLOATING_SHADOW} the shape is slightly inset
     * to keep the transparent shadow area outside the clip.
     *
     * @param dialogWidth  the total dialog width
     * @param dialogHeight the total dialog height
     * @return the shape to use, or {@code null} to leave the dialog unclipped
     */
    public java.awt.Shape getDialogClipShape(int dialogWidth, int dialogHeight) {
        return switch (style) {
            case SHARP -> null; // rectangular — no clip needed
            case PILL -> new RoundRectangle2D.Double(
                    0, 0, dialogWidth, dialogHeight, dialogHeight, dialogHeight);
            case FLOATING_SHADOW -> new RoundRectangle2D.Double(
                    0, 0, dialogWidth, dialogHeight, dialogWidth, dialogHeight); // full transparent
        };
    }
}
