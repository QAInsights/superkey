package io.github.naveenkumar.jmeter.superkey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Inspects the built JAR artifacts to enforce OSS/Pro class separation.
 *
 * <p>
 * These tests run <em>after</em> the JAR is built (i.e. during
 * {@code mvn verify} or {@code mvn package -P oss -DrunJarTests=true}).
 * They are skipped by default so they don't slow down {@code mvn test}.
 *
 * <p>
 * Run explicitly:
 * 
 * <pre>
 *   # Verify OSS JAR has no pro classes:
 *   mvn package -P oss -DrunJarTests=true
 *
 *   # Verify Pro JAR has pro classes:
 *   mvn package -P pro -DrunJarTests=true
 * </pre>
 */
@DisplayName("JAR Integrity — OSS JAR must not contain pro classes")
class OssJarIntegrityIT {

    private static final String PRO_CLASS_PREFIX = "io/github/naveenkumar/jmeter/superkey/pro/";
    private static final String TARGET_DIR = "target";

    // -------------------------------------------------------------------------
    // OSS JAR tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("OSS JAR must NOT contain any classes from the pro package")
    @EnabledIfSystemProperty(named = "runJarTests", matches = "true")
    void ossJarMustNotContainProClasses() throws IOException {
        Path ossJar = findJar("oss");

        List<String> proClasses = new ArrayList<>();
        try (JarFile jar = new JarFile(ossJar.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(PRO_CLASS_PREFIX) && entry.getName().endsWith(".class")) {
                    proClasses.add(entry.getName());
                }
            }
        }

        assertTrue(proClasses.isEmpty(),
                "\n\n🚨 OSS JAR contains Pro classes — this is a build misconfiguration!\n"
                        + "The following classes must NOT be in the OSS JAR:\n"
                        + String.join("\n  ", proClasses) + "\n"
                        + "\nFix: ensure the OSS Maven profile excludes src/**/pro/** correctly.\n");
    }

    @Test
    @DisplayName("OSS JAR must contain LicenseBridge (the OSS bridge class)")
    @EnabledIfSystemProperty(named = "runJarTests", matches = "true")
    void ossJarMustContainLicenseBridge() throws IOException {
        Path ossJar = findJar("oss");
        assertJarContains(ossJar,
                "io/github/naveenkumar/jmeter/superkey/LicenseBridge.class",
                "OSS JAR is missing LicenseBridge — it must be present in both OSS and Pro JARs.");
    }

    // -------------------------------------------------------------------------
    // Pro JAR tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Pro JAR must contain LicenseManager (the pro license class)")
    @EnabledIfSystemProperty(named = "runJarTests", matches = "true")
    void proJarMustContainLicenseManager() throws IOException {
        Path proJar = findJar("pro");
        assertJarContains(proJar,
                PRO_CLASS_PREFIX + "LicenseManager.class",
                "Pro JAR is missing LicenseManager — the pro package was not compiled.");
    }

    @Test
    @DisplayName("Pro JAR must also contain all OSS classes (it is a superset)")
    @EnabledIfSystemProperty(named = "runJarTests", matches = "true")
    void proJarMustContainOssClasses() throws IOException {
        Path proJar = findJar("pro");
        String ossRoot = "io/github/naveenkumar/jmeter/superkey/";
        String[] expectedOssClasses = {
                ossRoot + "LicenseBridge.class",
                ossRoot + "SuperKeyDialog.class",
                ossRoot + "SuperKeyInjector.class",
                ossRoot + "SuperKeyMenuCreator.class",
                ossRoot + "ComponentProvider.class",
                ossRoot + "EasterEggHandler.class",
        };
        for (String cls : expectedOssClasses) {
            assertJarContains(proJar, cls,
                    "Pro JAR is missing OSS class: " + cls + " — Pro must be a superset of OSS.");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Path findJar(String classifier) {
        Path targetDir = Paths.get(TARGET_DIR);
        String suffix = "-" + classifier + ".jar";
        try (var stream = java.nio.file.Files.list(targetDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(suffix))
                    .findFirst()
                    .orElseGet(() -> {
                        fail("Could not find *" + suffix + " in " + targetDir.toAbsolutePath()
                                + ". Run 'mvn package -P " + classifier + "' first.");
                        return null;
                    });
        } catch (IOException e) {
            return fail("Could not list target directory: " + e.getMessage());
        }
    }

    private void assertJarContains(Path jarPath, String entryName, String message) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            assertFalse(jar.stream()
                    .filter(e -> e.getName().equals(entryName))
                    .findFirst()
                    .isEmpty(), message + "\n  JAR: " + jarPath + "\n  Missing: " + entryName);
        }
    }
}
