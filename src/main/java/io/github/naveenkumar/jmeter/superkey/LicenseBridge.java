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
            Class<?> cls = Class.forName(LICENSE_MANAGER_CLASS);
            cachedResult = (Boolean) cls.getMethod("isPro").invoke(null);
        } catch (ClassNotFoundException e) {
            // Pro package not present — this is the OSS JAR, expected behaviour
            cachedResult = false;
        } catch (Exception e) {
            log.warn("SuperKey: unexpected error checking Pro license via bridge", e);
            cachedResult = false;
        }
        return cachedResult;
    }
}
