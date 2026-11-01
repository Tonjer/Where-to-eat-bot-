package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.model.Review;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;

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

        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        String username = update.getMessage().getFrom().getUserName();

        if (username == null || username.isBlank()) {
            username = update.getMessage().getFrom().getFirstName();
        }

        String text = update.getMessage().getText().trim();

        if (text.equalsIgnoreCase("/pending")) {
            sendPendingReviews(chatId, userId);
            return;
        }

        String answer = commandHandler.handleMessage(
                chatId, userId, username, text
        );

        if (answer != null && !answer.isBlank()) {
            messageSender.sendMessage(chatId, answer);
        }

        if (text.equalsIgnoreCase("/search")) {
            sendSearchResults(chatId);
        }
    }

    private void sendSearchResults(long chatId) {
        List<Restaurant> restaurants = commandHandler.getLastSearchResults(chatId);

        for (Restaurant restaurant : restaurants) {
            String text = (restaurants.indexOf(restaurant) + 1)
                    + ". " + restaurant.getName() + "\n"
                    + "📍 Адрес: " + restaurant.getAddress() + "\n"
                    + "🌍 Кухня: " + restaurant.getCuisine() + "\n"
                    + "🍽 Блюда: " + String.join(", ", restaurant.getDishes()) + "\n"
                    + "💰 Средний чек: " + restaurant.getAverageCheck() + " ₽";

            InlineKeyboardButton reviewButton = new InlineKeyboardButton("✍️ Оставить отзыв");

            reviewButton.setCallbackData("restaurant_review:" + restaurant.getId());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(reviewButton);

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(row);

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);

            messageSender.sendMessage(chatId, text, keyboard);
        }
    }

    private void sendPendingReviews(long chatId, long userId) {
        List<Review> reviews = commandHandler.getPendingReviewsForAdmin(userId);

        if (reviews == null) {
            messageSender.sendMessage(chatId, "У тебя нет доступа к административной панели.");
            return;
        }

        if (reviews.isEmpty()) {
            messageSender.sendMessage(chatId, "Новых отзывов пока нет.");
            return;
        }

        for (Review review : reviews) {
            String username = review.getUsername();

            if (username == null || username.isBlank()) {
                username = "Пользователь";
            }

            String text = "📋 Отзыв ожидает модерации\n\n"
                    + "🆔 ID: " + review.getId() + "\n"
                    + "🍽 ID ресторана: " + review.getRestaurantId() + "\n"
                    + "👤 Автор: " + username + "\n"
                    + "⭐ Оценка: " + review.getRating() + "/5\n"
                    + "💬 Отзыв: " + review.getText();

            InlineKeyboardButton approveButton = new InlineKeyboardButton("✅ Одобрить");
            approveButton.setCallbackData("approve:" + review.getId());

            InlineKeyboardButton rejectButton = new InlineKeyboardButton("❌ Отклонить");
            rejectButton.setCallbackData("reject:" + review.getId());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(approveButton);
            row.add(rejectButton);

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(row);

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(rows);

            messageSender.sendMessage(chatId, text, keyboard);
        }
    }

    private void handleCallback(Update update) {
        var callback = update.getCallbackQuery();

        long userId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        String callbackData = callback.getData();

        String answer;

        if (callbackData.startsWith("restaurant_review:")) {
            String restaurantId = callbackData.substring("restaurant_review:".length());

            answer = commandHandler.startReviewFromSearch(chatId, userId, restaurantId);

            messageSender.sendMessage(chatId, answer);

            answer = "Открываем написание отзыва.";
        } else {
            answer = commandHandler.handleAdminCallback(userId, callbackData);
        }

        AnswerCallbackQuery answerCallback = new AnswerCallbackQuery();
        answerCallback.setCallbackQueryId(callback.getId());
        answerCallback.setText(answer);
        answerCallback.setShowAlert(false);

        try {
            execute(answerCallback);
        } catch (Exception e) {
            System.err.println("Не удалось обработать нажатие кнопки.");
            e.printStackTrace();
        }
    }
}