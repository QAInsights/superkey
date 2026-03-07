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
}
