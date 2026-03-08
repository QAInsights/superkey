package io.github.naveenkumar.jmeter.superkey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ComponentItem Data Model")
class ComponentItemTest {

    @Test
    @DisplayName("ComponentItem should store name, className and isAction flag correctly")
    void componentItemShouldStoreData() {
        ComponentProvider.ComponentItem item = new ComponentProvider.ComponentItem("Test Name", "com.test.Class", true);
        assertEquals("Test Name", item.name);
        assertEquals("com.test.Class", item.className);
        assertTrue(item.isAction);
        assertEquals("Test Name", item.toString());
    }

    @Test
    @DisplayName("ComponentItem constructor without isAction should default to false")
    void componentItemDefaultConstructor() {
        ComponentProvider.ComponentItem item = new ComponentProvider.ComponentItem("Name", "Class");
        assertFalse(item.isAction);
    }
}
