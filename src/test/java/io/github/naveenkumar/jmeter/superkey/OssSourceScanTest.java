package io.github.naveenkumar.jmeter.superkey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scans every OSS Java source file (outside the pro/ package) and asserts
 * that none of them directly import anything from the pro package.
 *
 * <p>
 * This is the most important safety net — it catches the mistake of
 * adding {@code import io.github.naveenkumar.jmeter.superkey.pro.*}
 * to an OSS file, which would break the OSS build.
 *
 * <p>
 * Run with: {@code mvn test} (both OSS and Pro profiles)
 */
@DisplayName("OSS Source Integrity — no direct pro.* imports in OSS files")
class OssSourceScanTest {

    private static final String OSS_SOURCE_ROOT = "src/main/java/io/github/naveenkumar/jmeter/superkey";

    private static final String PRO_PACKAGE_IMPORT = "import io.github.naveenkumar.jmeter.superkey.pro.";

    private static final String PRO_SOURCE_DIR = "pro";

    @Test
    @DisplayName("No OSS source file should directly import from the pro package")
    void ossFilesMustNotImportProPackage() throws IOException {
        Path sourceRoot = Paths.get(OSS_SOURCE_ROOT);
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    // Skip files inside the pro/ directory — they are allowed to use pro classes
                    .filter(p -> !p.toString().replace("\\", "/").contains("/" + PRO_SOURCE_DIR + "/"))
                    .forEach(p -> {
                        try {
                            List<String> lines = Files.readAllLines(p);
                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i).trim();
                                if (line.startsWith(PRO_PACKAGE_IMPORT)) {
                                    violations.add(String.format(
                                            "  VIOLATION in %s at line %d:%n    %s%n    → Use LicenseBridge.isPro() instead.",
                                            p, i + 1, line));
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Could not read file: " + p, e);
                        }
                    });
        }

        assertTrue(violations.isEmpty(),
                "\n\n🚨 OSS source files must NOT import pro.* classes directly!\n"
                        + "Use LicenseBridge.isPro() for license checks instead.\n\n"
                        + String.join("\n", violations) + "\n");
    }
}
