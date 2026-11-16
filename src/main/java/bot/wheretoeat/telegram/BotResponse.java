package bot.wheretoeat.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotResponse {

    public record OutgoingMessage(String text, InlineKeyboardMarkup keyboard) {
        public OutgoingMessage(String text) {
            this(text, null);
        }
    }

    private final List<OutgoingMessage> messages;
    private final String callbackAnswer;

    public BotResponse(List<OutgoingMessage> messages, String callbackAnswer) {
        this.messages = messages == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(messages));
        this.callbackAnswer = callbackAnswer;
    }

    public static BotResponse empty() {
        return new BotResponse(List.of(), null);
    }

    public static BotResponse ofText(String text) {
        if (text == null || text.isBlank()) {
            return empty();
        }
        return new BotResponse(List.of(new OutgoingMessage(text)), null);
    }

    public static BotResponse ofMessage(String text, InlineKeyboardMarkup keyboard) {
        return new BotResponse(List.of(new OutgoingMessage(text, keyboard)), null);
    }

    public static BotResponse ofMessages(List<OutgoingMessage> messages) {
        return new BotResponse(messages, null);
    }

    public static BotResponse ofCallback(String callbackAnswer) {
        return new BotResponse(List.of(), callbackAnswer);
    }

    public static BotResponse ofCallbackAndMessages(String callbackAnswer, List<OutgoingMessage> messages) {
        return new BotResponse(messages, callbackAnswer);
    }

    public List<OutgoingMessage> getMessages() { return messages; }
    public String getCallbackAnswer() { return callbackAnswer; }
}