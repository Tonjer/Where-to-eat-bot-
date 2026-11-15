package bot.wheretoeat.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final TelegramCommandHandler commandHandler;
    private final TelegramMessageSender messageSender;

    public TelegramBot(String botUsername, String botToken, TelegramCommandHandler commandHandler) {
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
        if (update == null) {
            return;
        }

        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        if (message.getFrom() == null) {
            return;
        }

        long chatId = message.getChatId();
        long userId = message.getFrom().getId();

        String username = message.getFrom().getUserName();

        if (username == null || username.isBlank()) {
            username = message.getFrom().getFirstName();
        }

        String text = message.getText().trim();

        try {
            BotResponse response = commandHandler.routeMessage(chatId, userId, username, text);
            sendBotResponseMessages(chatId, response);
        } catch (Exception e) {
            System.err.println("Ошибка при обработке сообщения: " + e.getMessage());
            e.printStackTrace();
            messageSender.sendMessage(chatId, "Произошла ошибка при обработке запроса. Попробуйте позже.");
        }
    }

    private void handleCallback(Update update) {
        CallbackQuery callback = update.getCallbackQuery();
        if (callback == null || callback.getFrom() == null) {
            return;
        }

        String callbackId = callback.getId();
        long userId = callback.getFrom().getId();
        Long chatId = callback.getMessage() != null ? callback.getMessage().getChatId() : null;
        String callbackData = callback.getData();

        if (chatId == null || callbackData == null) {
            answerCallbackSilently(callbackId, "Некорректные данные кнопки.");
            return;
        }

        String callbackText = null;
        try {
            BotResponse response = commandHandler.routeCallback(chatId, userId, callbackData);
            callbackText = response.getCallbackAnswer();
            sendBotResponseMessages(chatId, response);
        } catch (Exception e) {
            System.err.println("Ошибка при обработке нажатия кнопки: " + e.getMessage());
            e.printStackTrace();
            callbackText = "Произошла ошибка при обработке действия.";
        } finally {
            answerCallbackSilently(callbackId, callbackText);
        }
    }

    private void sendBotResponseMessages(long chatId, BotResponse response) {
        if (response == null) {
            return;
        }
        for (BotResponse.OutgoingMessage msg : response.getMessages()) {
            messageSender.sendMessage(chatId, msg.text(), msg.keyboard());
        }
    }

    private void answerCallbackSilently(String callbackId, String answer) {
        if (callbackId == null) {
            return;
        }

        AnswerCallbackQuery answerCallback = new AnswerCallbackQuery();
        answerCallback.setCallbackQueryId(callbackId);
        if (answer != null || !answer.isBlank()) {
            answerCallback.setText(answer);
        }
        answerCallback.setShowAlert(false);

        try {
            execute(answerCallback);
        } catch (Exception e) {
            System.err.println("Не удалось подтвердить нажатие кнопки.");
            e.printStackTrace();
        }
    }
}