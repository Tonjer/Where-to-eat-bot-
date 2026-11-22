package bot.wheretoeat.telegram;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ParsedCommandTest {
    @Test
    void parsesCommandAndArguments() {
        ParsedCommand parsed = ParsedCommand.parse("  /search Japanese Ramen  ");

        assertTrue(parsed.isCommand());
        assertEquals("/search Japanese Ramen", parsed.getRawText());
        assertEquals("/search", parsed.getCommand());
        assertEquals("Japanese Ramen", parsed.getArguments());
    }

    @Test
    void parsesCommandAddressedToBot() {
        ParsedCommand parsed = ParsedCommand.parse("/start@Where_ToEat_bot hello");

        assertEquals("/start", parsed.getCommand());
        assertFalse(parsed.isAddressedToOtherBot("Where_ToEat_bot"));
        assertTrue(parsed.isAddressedToOtherBot("AnotherBot"));
    }

    @Test
    void ordinaryTextIsNotACommand() {
        ParsedCommand parsed = ParsedCommand.parse("hello there");

        assertFalse(parsed.isCommand());
        assertNull(parsed.getCommand());
        assertEquals("hello there", parsed.getArguments());
    }

    @Test
    void nullAndBlankInputAreHandled() {
        assertFalse(ParsedCommand.parse(null).isCommand());
        assertFalse(ParsedCommand.parse("  ").isCommand());
    }

    @Test
    void commandNameIsNormalizedToLowerCase() {
        assertEquals("/help", ParsedCommand.parse("/HELP").getCommand());
    }
}
