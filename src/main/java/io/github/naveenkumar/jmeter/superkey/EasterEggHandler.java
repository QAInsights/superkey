package io.github.naveenkumar.jmeter.superkey;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class EasterEggHandler {

    private static final Map<String, Consumer<JDialog>> EGGS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final String AUTHOR = "NaveenKumar Namachivayam";
    private static final String VERSION = "1.0.0";

    static {
        // --- Hello / Hi ---
        EGGS.put("hello", d -> showMessage(d,
                "<html><body style='font-family:SansSerif;font-size:13px;padding:8px;'>"
                        + "<div style='text-align:center'>&#128075; Hello there,<br><b>Performance Engineer!</b><br><br>"
                        + "May your tests always pass. &#128640;</div></body></html>",
                "SuperKey says Hi!"));
        EGGS.put("hi", d -> showMessage(d,
                "<html><body style='font-family:SansSerif;font-size:13px;padding:8px;'>"
                        + "<div style='text-align:center'>&#128075; Hey!<br><br>"
                        + "Ready to stress test some servers? &#128526;</div></body></html>",
                "SuperKey says Hi!"));

        // --- Coffee ---
        EGGS.put("coffee", d -> showCoffeeAnimation(d));

        // --- Matrix Rain ---
        EGGS.put("matrix", d -> showMatrixRain(d));

        // --- 42 ---
        EGGS.put("42", d -> showMessage(d,
                "<html><body style='font-family:SansSerif;font-size:13px;padding:8px;'>"
                        + "<div style='text-align:center'>"
                        + "<b style='font-size:28px;'>42</b><br><br>"
                        + "The Answer to Life, the Universe,<br>and Everything.<br><br>"
                        + "<i>&mdash; The Hitchhiker's Guide to the Galaxy</i>"
                        + "</div></body></html>",
                "Deep Thought"));

        // --- Flip ---
        EGGS.put("flip", d -> showFlip(d));

        // --- jmeter rocks ---
        EGGS.put("jmeter rocks", d -> showFireworks(d));

        // --- superkey ---
        EGGS.put("superkey", d -> showMessage(d,
                "<html>"
                        + "<body style='font-family:SansSerif; font-size:13px; padding:8px;'>"
                        + "<div style='text-align:center; margin-bottom:10px;'>"
                        + "  <b style='font-size:16px;'>&#128273; SuperKey Plugin</b>"
                        + "</div>"
                        + "<table cellpadding='4' style='margin:0 auto;'>"
                        + "  <tr><td align='right'><b>Author:</b></td><td>" + AUTHOR + "</td></tr>"
                        + "  <tr><td align='right'><b>Version:</b></td><td>" + VERSION + "</td></tr>"
                        + "  <tr><td align='right'><b>Built with:</b></td><td>&#10084;&#65039; for JMeter power users</td></tr>"
                        + "</table>"
                        + "<div style='text-align:center; margin-top:12px;'>"
                        + "  <i>You found one. &#127881;</i>"
                        + "</div>"
                        + "</body></html>",
                "You found me!"));

        // --- stress ---
        EGGS.put("stress", d -> showMessage(d,
                "<html><body style='font-family:SansSerif;font-size:13px;padding:8px;'>"
                        + "<div style='text-align:center'>&#128517; Feeling stressed?<br><br>"
                        + "<b>Pro tip:</b> Have you tried turning it off<br>and on again?<br><br>"
                        + "&#129496; Take a deep breath.<br>Your tests will pass.</div></body></html>",
                "Stress Relief 101"));

        // --- konami: handled via key sequence in SuperKeyDialog, here just the payload
        // ---
        EGGS.put("konami", d -> showKonamiConfetti(d));
    }

    /**
     * Checks if the typed text is an easter egg trigger. Returns true if handled.
     */
    public static boolean check(String lowerText, JDialog parent) {
        Consumer<JDialog> egg = EGGS.get(lowerText);
        if (egg != null) {
            egg.accept(parent);
            return true;
        }
        return false;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void showMessage(JDialog parent, String html, String title) {
        // html content is already a full html string, don't double-wrap it
        JLabel label = new JLabel(html, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setPreferredSize(new java.awt.Dimension(340, 200));
        JOptionPane.showMessageDialog(parent, label, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Coffee Animation ──────────────────────────────────────────────────────
    private static void showCoffeeAnimation(JDialog parent) {
        String[] frames = {
                "( )\n ||\n ||\n___||___\n☕ Brewing",
                "(  )\n ||\n ||\n___||___\n☕ Brewing.",
                "(   )\n ||\n ||\n___||___\n☕ Brewing..",
                "(    )\n ||\n ||\n___||___\n☕ Brewing...",
                "☕ Done! Tests are ready.",
        };
        JLabel label = new JLabel("<html><pre>" + frames[0] + "</pre></html>", SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", Font.PLAIN, 14));

        final int[] idx = { 0 };
        Timer t = new Timer(350, null);
        t.addActionListener(e -> {
            idx[0]++;
            if (idx[0] < frames.length) {
                label.setText("<html><pre>" + frames[idx[0]] + "</pre></html>");
            } else {
                t.stop();
            }
        });
        t.start();

        JOptionPane.showMessageDialog(parent, label, "☕ Coffee Break", JOptionPane.PLAIN_MESSAGE);
        t.stop();
    }

    // ── Matrix Rain ───────────────────────────────────────────────────────────
    private static void showMatrixRain(JDialog parent) {
        JDialog overlay = new JDialog(parent, true);
        overlay.setUndecorated(true);
        overlay.setSize(parent.getSize());
        overlay.setLocationRelativeTo(parent);
        overlay.setBackground(new Color(0, 0, 0, 200));

        int cols = overlay.getWidth() / 14;
        int[] yPos = new int[cols];
        for (int i = 0; i < cols; i++)
            yPos[i] = RANDOM.nextInt(overlay.getHeight() / 14);

        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setFont(new Font("Monospaced", Font.BOLD, 13));
                for (int i = 0; i < cols; i++) {
                    // Bright head
                    g2.setColor(new Color(180, 255, 180));
                    char ch = (char) (0x30A0 + RANDOM.nextInt(96));
                    g2.drawString(String.valueOf(ch), i * 14, yPos[i] * 14);
                    // Dim trail
                    g2.setColor(new Color(0, 200, 0, 180));
                    for (int j = 1; j <= 5 && yPos[i] - j >= 0; j++) {
                        char trail = (char) (0x30A0 + RANDOM.nextInt(96));
                        g2.drawString(String.valueOf(trail), i * 14, (yPos[i] - j) * 14);
                    }
                    yPos[i]++;
                    if (yPos[i] * 14 > getHeight())
                        yPos[i] = 0;
                }
                g2.dispose();
            }
        };
        canvas.setBackground(Color.BLACK);
        canvas.setOpaque(true);

        JLabel exitHint = new JLabel("Press ESC or click to close", SwingConstants.CENTER);
        exitHint.setForeground(new Color(0, 255, 0));
        exitHint.setFont(new Font("Monospaced", Font.BOLD, 12));
        overlay.setLayout(new java.awt.BorderLayout());
        overlay.add(canvas, java.awt.BorderLayout.CENTER);
        overlay.add(exitHint, java.awt.BorderLayout.SOUTH);

        Timer anim = new Timer(60, e -> canvas.repaint());
        anim.start();

        // Auto-close after 5s
        Timer closer = new Timer(5000, e -> {
            anim.stop();
            overlay.dispose();
        });
        closer.setRepeats(false);
        closer.start();

        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                anim.stop();
                closer.stop();
                overlay.dispose();
            }
        });
        canvas.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    anim.stop();
                    closer.stop();
                    overlay.dispose();
                }
            }
        });
        canvas.setFocusable(true);
        overlay.setVisible(true);
        canvas.requestFocus();
    }

    // ── Table Flip ────────────────────────────────────────────────────────────
    private static void showFlip(JDialog parent) {
        String[] stages = {
                "ヽ(°□°ヽ)",
                "ヽ(°□° )ヽ",
                "(╯°□°）╯",
                "(╯°□°）╯︵ ┻",
                "(╯°□°）╯︵ ┻━",
                "(╯°□°）╯︵ ┻━┻",
        };
        JLabel label = new JLabel(stages[0], SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", Font.BOLD, 22));

        final int[] i = { 0 };
        Timer t = new Timer(200, null);
        t.addActionListener(e -> {
            if (i[0] < stages.length)
                label.setText(stages[i[0]++]);
            else
                t.stop();
        });
        t.start();
        JOptionPane.showMessageDialog(parent, label, "Frustration Level: MAX", JOptionPane.PLAIN_MESSAGE);
        t.stop();
    }

    // ── Fireworks ─────────────────────────────────────────────────────────────
    private static void showFireworks(JDialog parent) {
        JDialog overlay = new JDialog(parent, true);
        overlay.setUndecorated(true);
        overlay.setSize(parent.getSize());
        overlay.setLocationRelativeTo(parent);

        List<float[]> particles = new ArrayList<>(); // x,y,vx,vy,life,r,g,b

        Runnable burst = () -> {
            float bx = 50 + RANDOM.nextFloat() * (overlay.getWidth() - 100);
            float by = 50 + RANDOM.nextFloat() * (overlay.getHeight() / 2f);
            float r = RANDOM.nextFloat(), g = RANDOM.nextFloat(), b = RANDOM.nextFloat();
            for (int k = 0; k < 40; k++) {
                double ang = RANDOM.nextDouble() * Math.PI * 2;
                float sp = 1 + RANDOM.nextFloat() * 4;
                particles.add(new float[] { bx, by,
                        (float) (Math.cos(ang) * sp), (float) (Math.sin(ang) * sp),
                        1f, r, g, b });
            }
        };

        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (float[] p : particles) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, p[4])));
                    g2.setColor(new Color(p[5], p[6], p[7]));
                    g2.setStroke(new BasicStroke(2));
                    g2.fillOval((int) p[0] - 3, (int) p[1] - 3, 6, 6);
                }
                // Title
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                FontMetrics fm = g2.getFontMetrics();
                String msg = "🎆 JMeter Rocks! 🎆";
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() - 30);
                g2.dispose();
            }
        };
        canvas.setBackground(new Color(15, 15, 30));
        overlay.setLayout(new java.awt.BorderLayout());
        overlay.add(canvas, java.awt.BorderLayout.CENTER);

        Timer anim = new Timer(40, e -> {
            particles.removeIf(p -> p[4] <= 0);
            for (float[] p : particles) {
                p[0] += p[2];
                p[1] += p[3];
                p[3] += 0.05f;
                p[4] -= 0.018f;
            }
            // random bursts
            if (RANDOM.nextInt(5) == 0)
                burst.run();
            canvas.repaint();
        });
        // Initial burst
        for (int k = 0; k < 3; k++)
            burst.run();
        anim.start();

        Timer closer = new Timer(4000, e -> {
            anim.stop();
            overlay.dispose();
        });
        closer.setRepeats(false);
        closer.start();

        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                anim.stop();
                closer.stop();
                overlay.dispose();
            }
        });

        overlay.setVisible(true);
    }

    // ── Konami Confetti ───────────────────────────────────────────────────────
    public static void showKonamiConfetti(JDialog parent) {
        JDialog overlay = new JDialog(parent, true);
        overlay.setUndecorated(true);
        overlay.setSize(parent.getSize());
        overlay.setLocationRelativeTo(parent);

        List<float[]> pieces = new ArrayList<>(); // x,y,vx,vy,rot,rotV,life,r,g,b
        for (int k = 0; k < 80; k++) {
            pieces.add(new float[] {
                    RANDOM.nextFloat() * overlay.getWidth(),
                    -RANDOM.nextInt(overlay.getHeight()),
                    -1.5f + RANDOM.nextFloat() * 3,
                    1 + RANDOM.nextFloat() * 3,
                    RANDOM.nextFloat() * 360,
                    -5 + RANDOM.nextFloat() * 10,
                    1f,
                    RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat()
            });
        }

        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (float[] p : pieces) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, p[6])));
                    g2.setColor(new Color(p[7], p[8], p[9]));
                    g2.rotate(Math.toRadians(p[4]), p[0], p[1]);
                    g2.fillRect((int) p[0], (int) p[1], 10, 5);
                    g2.rotate(-Math.toRadians(p[4]), p[0], p[1]);
                }
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                String msg = "⬆⬆⬇⬇⬅➡⬅➡ B A  — CHEAT ACTIVATED!";
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() - 20);
                g2.dispose();
            }
        };
        canvas.setBackground(new Color(20, 20, 40));
        overlay.setLayout(new java.awt.BorderLayout());
        overlay.add(canvas, java.awt.BorderLayout.CENTER);

        Timer anim = new Timer(30, e -> {
            for (float[] p : pieces) {
                p[0] += p[2];
                p[1] += p[3];
                p[4] += p[5];
                p[6] -= 0.008f;
            }
            canvas.repaint();
        });
        anim.start();

        Timer closer = new Timer(4000, e -> {
            anim.stop();
            overlay.dispose();
        });
        closer.setRepeats(false);
        closer.start();

        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                anim.stop();
                closer.stop();
                overlay.dispose();
            }
        });

        overlay.setVisible(true);
    }
}
