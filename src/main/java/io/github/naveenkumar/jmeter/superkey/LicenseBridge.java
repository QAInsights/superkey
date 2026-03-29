package io.github.naveenkumar.jmeter.superkey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LicenseBridge — a reflection-based bridge that OSS code can safely call
 * without having a compile-time dependency on the {@code pro} package.
 *
 * <p>
 * Flow:
 * <ul>
 * <li><b>OSS JAR</b>: {@code LicenseManager} class is absent →
 * {@code ClassNotFoundException} is caught → {@code isPro()} returns
 * {@code false}.</li>
 * <li><b>Pro JAR</b>: {@code LicenseManager} class is present →
 * reflection invokes it → returns the real license check result.</li>
 * </ul>
 *
 * <p>
 * <b>Rule:</b> OSS source files must NEVER directly import anything from
 * the {@code pro} package. Always go through this bridge instead.
 */
public final class LicenseBridge {

    private static final Logger log = LoggerFactory.getLogger(LicenseBridge.class);
    private static final String LICENSE_MANAGER_CLASS = "io.github.naveenkumar.jmeter.superkey.pro.LicenseManager";
    private static final String STYLE_MANAGER_CLASS = "io.github.naveenkumar.jmeter.superkey.pro.StyleManager";
    private static final String UPGRADE_BANNER_CLASS = "io.github.naveenkumar.jmeter.superkey.pro.ProUpgradeBanner";

    /** Cached result — license status doesn't change during a JMeter session. */
    private static Boolean cachedResult = null;

    private LicenseBridge() {
        /* utility class */ }

    /**
     * Returns {@code true} when the Pro JAR is loaded AND a valid license is found.
     * Safe to call from any OSS class — will never throw.
     */
    public static boolean isPro() {
        if (cachedResult != null) {
            return cachedResult;
        }
        try {
            // Check if LicenseManager is on the classpath (Pro JAR)
            Class<?> cls = Class.forName(LICENSE_MANAGER_CLASS);
            // In some build environments, we might want to return false if JMeter isn't
            // initialized
            // or if we specifically want to skip Pro logic during certain tests.
            cachedResult = (Boolean) cls.getMethod("isPro").invoke(null);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Pro package not present — this is the OSS JAR, expected behaviour
            // OSS mode
            cachedResult = false;
        } catch (Exception e) {
            log.warn("SuperKey: unexpected error checking Pro license via bridge", e);
            cachedResult = false;
        }
        return cachedResult;
    }

    /**
     * Returns the active Pro dialog style name (e.g. {@code "SHARP"},
     * {@code "PILL"},
     * {@code "FLOATING_SHADOW"}), or {@code null} when:
     * <ul>
     * <li>the Pro JAR is not loaded (OSS mode), or</li>
     * <li>no {@code superkey.dialog.style} property is configured.</li>
     * </ul>
     *
     * <p>
     * Safe to call from any OSS class — will never throw.
     */
    public static String getDialogStyle() {
        if (!isPro()) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(STYLE_MANAGER_CLASS);
            return (String) cls.getMethod("getActiveStyleName").invoke(null);
        } catch (ClassNotFoundException e) {
            // Pro package not present — OSS JAR, expected behaviour
            return null;
        } catch (Exception e) {
            log.warn("SuperKey: unexpected error reading dialog style via bridge", e);
            return null;
        }
    }

    /**
     * Returns a JComponent showing a Pro upgrade banner for the given feature.
     * If the Pro JAR is loaded, returns the real ProUpgradeBanner.
     * If running in OSS mode, returns a simple fallback panel.
     *
     * @param featureName Name of the feature to display in the banner.
     */
    public static javax.swing.JComponent getUpgradeBanner(String featureName) {
        try {
            Class<?> cls = Class.forName(UPGRADE_BANNER_CLASS);
            return (javax.swing.JComponent) cls.getConstructor(String.class).newInstance(featureName);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // OSS Fallback: Return a simple warning panel
            javax.swing.JPanel fallback = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
            fallback.setBackground(new java.awt.Color(30, 30, 40));
            fallback.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new java.awt.Color(99, 102, 241), 1, true),
                    javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12)));

            javax.swing.JLabel message = new javax.swing.JLabel(
                    "<html><body style='color: rgb(200,200,220); font-family: SansSerif; font-size: 12px;'>"
                            + "<b style='color: rgb(129,140,248);'>" + featureName
                            + "</b> is a <b>SuperKey Pro</b> feature.</body></html>");

            fallback.add(new javax.swing.JLabel("🔒"), java.awt.BorderLayout.WEST);
            fallback.add(message, java.awt.BorderLayout.CENTER);
            return fallback;
        } catch (Exception e) {
            log.warn("SuperKey: error creating upgrade banner via bridge", e);
            return new javax.swing.JPanel();
        }
    }
}
