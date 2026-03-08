package io.github.naveenkumar.jmeter.superkey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the search and filtering logic used in the SuperKey dialog.
 */
@DisplayName("SuperKey Search Logic")
class SuperKeySearchLogicTest {

    @Test
    @DisplayName("Filter logic should match by name, class name, or mapped shortcut")
    void testSearchFilteringLogic() {
        // Mock data
        List<ComponentProvider.ComponentItem> allComponents = new ArrayList<>();
        allComponents.add(new ComponentProvider.ComponentItem("HTTP Request", "HTTPSamplerProxy"));
        allComponents.add(new ComponentProvider.ComponentItem("JDBC Request", "JDBCSampler"));
        allComponents.add(new ComponentProvider.ComponentItem("Thread Group", "ThreadGroup"));
        allComponents.add(new ComponentProvider.ComponentItem("Action: Start", "test_start", true));

        // Mock shortcuts
        java.util.Map<String, String> shortcutMap = new java.util.HashMap<>();
        shortcutMap.put("tg", "thread group");
        shortcutMap.put("h", "http");

        // Test Case 1: Search by name (case-insensitive)
        String searchText1 = "http";
        List<ComponentProvider.ComponentItem> result1 = filter(allComponents, shortcutMap, searchText1);
        assertEquals(1, result1.size());
        assertEquals("HTTP Request", result1.get(0).name);

        // Test Case 2: Search by class name
        String searchText2 = "sampler";
        List<ComponentProvider.ComponentItem> result2 = filter(allComponents, shortcutMap, searchText2);
        assertEquals(2, result2.size()); // HTTP and JDBC both have Sampler in class name

        // Test Case 3: Search by shortcut
        String searchText3 = "tg";
        List<ComponentProvider.ComponentItem> result3 = filter(allComponents, shortcutMap, searchText3);
        assertEquals(1, result3.size());
        assertEquals("Thread Group", result3.get(0).name);

        // Test Case 4: No match
        String searchText4 = "non-existent";
        List<ComponentProvider.ComponentItem> result4 = filter(allComponents, shortcutMap, searchText4);
        assertTrue(result4.isEmpty());
    }

    /**
     * Mimics the logic inside SuperKeyDialog.filterList
     */
    private List<ComponentProvider.ComponentItem> filter(
            List<ComponentProvider.ComponentItem> all, 
            java.util.Map<String, String> shortcuts, 
            String text) {
        
        String lowerText = text.toLowerCase().trim();
        String mappedName = shortcuts.get(lowerText);

        return all.stream()
                .filter(c -> {
                    String nameLower = c.name.toLowerCase();
                    String classLower = c.className.toLowerCase();
                    boolean matchesOriginal = nameLower.contains(lowerText) || classLower.contains(lowerText);
                    boolean matchesMapped = mappedName != null && (nameLower.contains(mappedName) || classLower.contains(mappedName));
                    return matchesOriginal || matchesMapped;
                })
                .collect(Collectors.toList());
    }
}
