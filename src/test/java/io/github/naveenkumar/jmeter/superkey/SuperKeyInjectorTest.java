package io.github.naveenkumar.jmeter.superkey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SuperKeyInjector - tree insertion")
class SuperKeyInjectorTest {

    @BeforeAll
    static void setup() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    @DisplayName("injectComponent should not crash when GuiPackage is null (uninitialized JMeter)")
    void injectComponentNullSafety() {
        // GuiPackage.getInstance() will be null in a standard unit test.
        // We verify that calling these methods doesn't throw a NullPointerException.
        assertDoesNotThrow(() -> SuperKeyInjector.injectComponent("some.class.Name"));
        assertDoesNotThrow(() -> SuperKeyInjector.injectComponent("some.class.Name", 5));
    }
}
