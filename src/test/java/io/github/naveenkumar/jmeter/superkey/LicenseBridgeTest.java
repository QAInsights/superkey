package io.github.naveenkumar.jmeter.superkey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests for {@link LicenseBridge} — verifies it behaves correctly in both
 * OSS and Pro JAR contexts.
 *
 * <p>
 * <b>When run with the OSS profile</b> ({@code mvn test -P oss}):
 * {@code LicenseManager} is not on the classpath → {@code isPro()} must return
 * {@code false}.
 *
 * <p>
 * <b>When run with the Pro profile</b> ({@code mvn test -P pro}):
 * {@code LicenseManager} <em>is</em> on the classpath but no license is
 * configured
 * in CI → {@code isPro()} still returns {@code false} (no valid license
 * present).
 *
 * <p>
 * In both cases the method must never throw.
 */
@DisplayName("LicenseBridge — safe license check in OSS and Pro contexts")
class LicenseBridgeTest {

    @Test
    @DisplayName("LicenseBridge.isPro() must never throw an exception")
    void isProMustNeverThrow() {
        assertDoesNotThrow(LicenseBridge::isPro,
                "LicenseBridge.isPro() threw an exception — it must always be safe to call.");
    }

    @Test
    @DisplayName("LicenseBridge.isPro() must return false when no license is configured (OSS mode / CI)")
    void isProReturnsFalseWithoutLicense() {
        // Ensure no license key is set via system property during this test
        String originalKey = System.getProperty("superkey.license.key");
        System.clearProperty("superkey.license.key");

        try {
            boolean result = LicenseBridge.isPro();
            assertFalse(result,
                    "LicenseBridge.isPro() returned true without any license configured. "
                            + "The OSS build must never activate Pro mode by default.");
        } finally {
            // Restore if it was set before
            if (originalKey != null) {
                System.setProperty("superkey.license.key", originalKey);
            }
        }
    }

    @Test
    @DisplayName("LicenseBridge class must be loadable in both OSS and Pro builds")
    void licenseBridgeClassMustExist() {
        assertNotNull(LicenseBridge.class,
                "LicenseBridge must always be present — it lives in the OSS package.");
    }
}
