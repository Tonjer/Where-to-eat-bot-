
package bot.wheretoeat.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final TelegramCommandHandler commandHandler;
    private final TelegramMessageSender messageSender;

    public TelegramBot(
            String botUsername,
            String botToken,
            TelegramCommandHandler commandHandler
    ) {
        super(botToken);
        this.botUsername = botUsername;
        this.commandHandler = commandHandler;
        this.messageSender = new TelegramMessageSender(this);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null
                || !update.hasMessage()
                || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        System.out.println("Получено сообщение: " + text);

        String answer = commandHandler.handleMessage(chatId, text);
        messageSender.sendMessage(chatId, answer);
    }
}
