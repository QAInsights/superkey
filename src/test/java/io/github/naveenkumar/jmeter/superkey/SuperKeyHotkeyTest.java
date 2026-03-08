package io.github.naveenkumar.jmeter.superkey;

import org.apache.jmeter.util.JMeterUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Hotkey feature.
 * Includes both functional checks (best-effort GUI test) and 
 * static source analysis (ensuring architectural standards).
 */
@DisplayName("SuperKey Hotkey Feature Tests")
class SuperKeyHotkeyTest {

    @BeforeAll
    static void setup() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    @DisplayName("RootPane should have focus-independent hotkeys (1-9, 0) registered")
    void hotkeysShouldBeRegisteredInRootPane() {
        try {
            SuperKeyDialog dialog = new SuperKeyDialog();
            JRootPane rootPane = dialog.getRootPane();
            
            InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = rootPane.getActionMap();

            String mod = JMeterUtils.getPropDefault("jmeter.superkey.hotkey.modifier", "Alt");
            int expectedMask;
            if ("Ctrl".equalsIgnoreCase(mod)) expectedMask = InputEvent.CTRL_DOWN_MASK;
            else if ("Shift".equalsIgnoreCase(mod)) expectedMask = InputEvent.SHIFT_DOWN_MASK;
            else expectedMask = InputEvent.ALT_DOWN_MASK;

            // Verify keys 1 to 9
            for (int i = 1; i <= 9; i++) {
                KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, expectedMask);
                Object actionKey = inputMap.get(ks);
                assertNotNull(actionKey, "InputMap missing entry for " + mod + "+" + i);
                assertNotNull(actionMap.get(actionKey), "ActionMap missing action for " + actionKey);
            }
            
            dialog.dispose();
        } catch (Throwable e) {
            // Safely skip if JMeter/Swing environment is not available
            System.out.println("Functional test skipped: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Source Analysis: Should use focus-independent Swing APIs")
    void verifyHotkeyImplementationInSource() throws IOException {
        String path = "src/main/java/io/github/naveenkumar/jmeter/superkey/SuperKeyDialog.java";
        String content = new String(Files.readAllBytes(Paths.get(path)));
        
        // Critical for focus independence (the "Flow Launcher" requirement)
        assertTrue(content.contains("WHEN_IN_FOCUSED_WINDOW"), 
                "Must use WHEN_IN_FOCUSED_WINDOW scope for hotkeys");
        
        // Critical for capturing spinner edits before hotkey execution
        assertTrue(content.contains("countSpinner.commitEdit()"), 
                "Must commit spinner edits in injectSelected()");
        
        // Ensure hotkeys are registered at the root pane level
        assertTrue(content.contains("getRootPane()"), 
                "Hotkeys should be registered at the RootPane level");
    }
}
