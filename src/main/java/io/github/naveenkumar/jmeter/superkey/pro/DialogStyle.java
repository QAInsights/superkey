package io.github.naveenkumar.jmeter.superkey.pro;

/**
 * DialogStyle — the three Pro-only visual themes for the SuperKey dialog.
 *
 * <p>
 * Activated via the JMeter property {@code superkey.dialog.style}:
 * <ul>
 * <li>{@code sharp} → {@link #SHARP} (hard 90° corners, flat fill)</li>
 * <li>{@code pill} → {@link #PILL} (fully-rounded oval, macOS Spotlight
 * style)</li>
 * <li>{@code floating_shadow} → {@link #FLOATING_SHADOW} (rounded rect +
 * drop-shadow, Linear/Notion style)</li>
 * </ul>
 *
 * <p>
 * When the property is absent the OSS default rounded-rectangle style is used.
 *
 * <p>
 * <b>Note:</b> This class lives in the {@code pro} package and is excluded
 * from the OSS JAR by the Maven {@code oss} build profile.
 */
public enum DialogStyle {

    /**
     * Sharp Rectangle — hard 90° corners, flat dark background, 1px solid border.
     * Inspired by classic Windows / IntelliJ-style toolbars.
     */
    SHARP,

    /**
     * Pill / Fully Rounded — the dialog arc equals the full height, creating
     * a macOS-Spotlight-style search pill.
     */
    PILL,

    /**
     * Floating with Shadow — softly rounded corners (arc = 16) with a
     * translucent multi-layer drop-shadow, inspired by Linear and Notion command
     * palettes.
     */
    FLOATING_SHADOW;

    /**
     * Parses the {@code superkey.dialog.style} property value (case-insensitive).
     *
     * @param value the raw property string, may be {@code null}
     * @return the matching {@link DialogStyle}, or {@code null} if value is blank /
     *         unrecognised
     */
    public static DialogStyle fromProperty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toLowerCase()) {
            case "sharp" -> SHARP;
            case "pill" -> PILL;
            case "floating_shadow" -> FLOATING_SHADOW;
            default -> null;
        };
    }
}
