package io.github.naveenkumar.jmeter.superkey.pro;

import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StyleManager — reads the active Pro dialog style from JMeter properties.
 *
 * <p>
 * The user activates a style by adding to {@code user.properties}:
 * 
 * <pre>
 *   # Values: sharp | pill | floating_shadow
 *   superkey.dialog.style=pill
 * </pre>
 *
 * <p>
 * When the property is absent or invalid this class returns {@code null}
 * and the OSS default rounded-rectangle is used unchanged.
 *
 * <p>
 * <b>Note:</b> This class lives in the {@code pro} package and is excluded
 * from the OSS JAR by the Maven {@code oss} build profile.
 * OSS code must never import this class directly — use
 * {@code LicenseBridge.getDialogStyle()} via reflection instead.
 */
public final class StyleManager {

    private static final Logger log = LoggerFactory.getLogger(StyleManager.class);

    /** JMeter property key that controls the dialog style. */
    public static final String STYLE_PROP_KEY = "superkey.dialog.style";

    private StyleManager() {
        /* utility class */ }

    /**
     * Returns the active {@link DialogStyle}, or {@code null} when the property
     * is absent, blank, or set to an unrecognised value.
     */
    public static DialogStyle getActiveStyle() {
        try {
            String raw = JMeterUtils.getProperty(STYLE_PROP_KEY);
            DialogStyle style = DialogStyle.fromProperty(raw);
            if (style != null) {
                log.info("SuperKey Pro: dialog style set to '{}'", style);
            }
            return style;
        } catch (Exception e) {
            log.debug("SuperKey Pro: could not read dialog style property", e);
            return null;
        }
    }

    /**
     * Returns the active style name as a plain {@link String} (e.g.
     * {@code "PILL"}),
     * or {@code null} when no valid style is configured.
     *
     * <p>
     * This method is called via reflection from {@code LicenseBridge} so that
     * OSS source files never need a compile-time dependency on this class.
     */
    public static String getActiveStyleName() {
        DialogStyle style = getActiveStyle();
        return style == null ? null : style.name();
    }
}
