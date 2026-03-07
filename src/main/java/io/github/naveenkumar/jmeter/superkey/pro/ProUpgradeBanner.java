package io.github.naveenkumar.jmeter.superkey.pro;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

/**
 * ProUpgradeBanner — shown inside the SuperKey dialog when the user
 * is running the OSS edition and tries to access a Pro-only feature.
 *
 * <p>
 * This class is in the {@code pro} package and will <em>not</em> be compiled
 * into the OSS JAR (it is excluded by the OSS Maven build profile).
 * It is only compiled into the Pro JAR, where it is used to render a tasteful
 * upgrade call-to-action when the license is absent.
 */
public final class ProUpgradeBanner extends JPanel {

    private static final Color BG_COLOR = new Color(30, 30, 40);
    private static final Color BORDER_COLOR = new Color(99, 102, 241); // indigo-500
    private static final Color LINK_COLOR = new Color(129, 140, 248); // indigo-400

    /** URL the "Upgrade" link points to. */
    private static final String UPGRADE_URL = "https://github.com/QAInsights/superkey-pro";

    public ProUpgradeBanner(String featureName) {
        super(new BorderLayout(8, 0));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // Lock icon
        JLabel iconLabel = new JLabel("🔒");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(LINK_COLOR);

        // Message
        JLabel messageLabel = new JLabel(
                "<html><body style='color: rgb(200,200,220); font-family: SansSerif; font-size: 12px;'>"
                        + "<b style='color: rgb(129,140,248);'>" + featureName
                        + "</b> is a <b>SuperKey Pro</b> feature.</body></html>");

        // Upgrade link
        JLabel upgradeLink = new JLabel("<html><u>Upgrade to Pro →</u></html>");
        upgradeLink.setForeground(LINK_COLOR);
        upgradeLink.setFont(new Font("SansSerif", Font.BOLD, 12));
        upgradeLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        upgradeLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openUpgradeUrl();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                upgradeLink.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                upgradeLink.setForeground(LINK_COLOR);
            }
        });

        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        textPanel.setOpaque(false);
        textPanel.add(messageLabel);
        textPanel.add(upgradeLink);

        add(iconLabel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    private static void openUpgradeUrl() {
        try {
            Desktop.getDesktop().browse(new URI(UPGRADE_URL));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Please visit: " + UPGRADE_URL,
                    "SuperKey Pro",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
