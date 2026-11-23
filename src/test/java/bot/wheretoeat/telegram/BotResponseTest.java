package bot.wheretoeat.telegram;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BotResponseTest {
    @Test
    void emptyReturnsNoMessageAndNullCallbackAnswer(){
        BotResponse response = BotResponse.empty();

        assertTrue(response.getMessages().isEmpty());
        assertNull(response.getCallbackAnswer());
    }

    @Test
    void ofTextIgnoresNullAndBlankStrings(){
        assertTrue(BotResponse.ofText(null).getMessages().isEmpty());
        assertTrue(BotResponse.ofText("   ").getMessages().isEmpty());

        BotResponse valid = BotResponse.ofText("Hello");
        assertEquals(1, valid.getMessages().size());
        assertEquals("Hello", valid.getMessages().getFirst().text());
        assertNull(valid.getMessages().getFirst().keyboard());
        assertNull(valid.getCallbackAnswer());
    }

    @Test
    void ofMessageAttachesInlineKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        BotResponse response = BotResponse.ofMessage("Choose", keyboard);

        assertEquals(1, response.getMessages().size());
        assertEquals("Choose", response.getMessages().getFirst().text());
        assertSame(keyboard, response.getMessages().getFirst().keyboard());
    }

    @Test
    void ofCallbackAndMessagesCombinesCallbackAnswerWithMessages() {
        List<BotResponse.OutgoingMessage> source = new ArrayList<>();
        source.add(new BotResponse.OutgoingMessage("Msg 1"));

        BotResponse response = BotResponse.ofCallbackAndMessages("Done", source);
        source.add(new BotResponse.OutgoingMessage("Msg 2"));

        assertEquals("Done", response.getCallbackAnswer());
        assertEquals(1, response.getMessages().size());
        assertThrows(UnsupportedOperationException.class, () -> response.getMessages().add(new BotResponse.OutgoingMessage("Msg 3")));
    }

    @Test
    void ofCallbackCreatesCallbackOnlyResponse() {
        BotResponse response = BotResponse.ofCallback("Ack");

        assertEquals("Ack", response.getCallbackAnswer());
        assertTrue(response.getMessages().isEmpty());
    }
}
