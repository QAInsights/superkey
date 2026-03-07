package io.github.naveenkumar.jmeter.superkey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    @BeforeEach
    void clearCache() {
        try {
            java.lang.reflect.Field field = LicenseBridge.class.getDeclaredField("cachedResult");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("LicenseBridge.isPro() must never throw an exception")
    void isProMustNeverThrow() {
        assertDoesNotThrow(LicenseBridge::isPro,
                "LicenseBridge.isPro() threw an exception — it must always be safe to call.");
    }

    @Test
    @DisplayName("LicenseBridge.isPro() must return false when no license is configured (OSS mode / CI)")
    void isProReturnsFalseWithoutLicense() throws IOException {
        File licFile = new File("superkey.lic");
        boolean backedUp = false;
        if (licFile.exists()) {
            System.out.println("DEBUG: Temporary moving superkey.lic for the duration of this test...");
            Files.move(licFile.toPath(), new File("superkey.lic.bak").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            backedUp = true;
        }

        try {
            // Need to clear the cache in LicenseManager via reflection because it might
            // have run already
            try {
                java.lang.reflect.Field field = Class
                        .forName("io.github.naveenkumar.jmeter.superkey.pro.LicenseManager")
                        .getDeclaredField("cachedResult");
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception ignored) {
            }

            boolean result = LicenseBridge.isPro();
            assertFalse(result,
                    "LicenseBridge.isPro() returned true without any license configured. "
                            + "The OSS build must never activate Pro mode by default.");
        } finally {
            if (backedUp) {
                Files.move(new File("superkey.lic.bak").toPath(), licFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
