package bot.wheretoeat.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilitiesTest {
    @Test
    void isBlankHandlesNullEmptyAndWhitespace() {
        assertTrue(StringUtilities.isBlank(null));
        assertTrue(StringUtilities.isBlank(""));
        assertTrue(StringUtilities.isBlank("   "));
        assertFalse(StringUtilities.isBlank(" text "));
    }

    @Test
    void trimToNullTrimsTextAndConvertsBlankToNull() {
        assertEquals("hello", StringUtilities.trimToNull("  hello  "));
        assertNull(StringUtilities.trimToNull(null));
        assertNull(StringUtilities.trimToNull("   "));
    }

    @Test
    void containsIgnoreCaseFindsSubstringIgnoringCase() {
        assertTrue(StringUtilities.containsIgnoreCase("Japanese cuisine", "JAPAN"));
        assertFalse(StringUtilities.containsIgnoreCase("Italian", "Japan"));
    }

    @Test
    void containsIgnoreCaseReturnsFalseForNullArguments() {
        assertFalse(StringUtilities.containsIgnoreCase(null, "x"));
        assertFalse(StringUtilities.containsIgnoreCase("text", null));
        assertFalse(StringUtilities.containsIgnoreCase(null, null));
    }
}
