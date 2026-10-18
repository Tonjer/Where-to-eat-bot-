
package bot.wheretoeat.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;

    public TelegramBot(String botUsername, String botToken) {
        super(botToken);
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Игнорируем события, которые не содержат обычного текстового сообщения.
        if (update == null
                || !update.hasMessage()
                || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        System.out.println("Получено сообщение: " + text);

        String answer;

        if (text.equals("/start")) {
            answer = """
                    Привет! Я бот «Где поесть?».

                    Я помогу тебе найти заведение по типу кухни,
                    блюдам и среднему чеку.

                    Напиши /help, чтобы увидеть доступные команды.
                    """;
        } else if (text.equals("/help")) {
            answer = """
                    Доступные команды:

                    /start — начать работу
                    /help — помощь
                    /all — показать все заведения

                    Поиск по фильтрам подключим следующим этапом.
                    """;
        } else if (text.equals("/all")) {
            answer = "Пока команда /all не подключена к базе ресторанов.";
        } else {
            answer = "Я пока не знаю такую команду. Напиши /help.";
        }

        sendText(chatId, answer);
    }

    private void sendText(long chatId, String text) {
        SendMessage message = new SendMessage(
                String.valueOf(chatId),
                text
        );

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Не удалось отправить сообщение в Telegram.");
            e.printStackTrace();
        }
    }
}
