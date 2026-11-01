
package bot.wheretoeat.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class TelegramMessageSender {

    private final TelegramLongPollingBot bot;

    public TelegramMessageSender(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage(
                String.valueOf(chatId),
                text
        );

        executeMessage(message);
    }
    public void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(keyboard);
        executeMessage(message);
    }
    private void executeMessage(SendMessage message) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Не удалось отправить сообщение в Telegram.");
            e.printStackTrace();
        }
    }
}
