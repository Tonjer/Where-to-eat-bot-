
package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantRepository;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final RestaurantRepository repository;

    public TelegramBot(
            String botUsername,
            String botToken,
            RestaurantRepository repository
    ) {
        super(botToken);
        this.botUsername = botUsername;
        this.repository = repository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, что пришло обычное текстовое сообщение.
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
                    """;

        } else if (text.equals("/all")) {
            answer = getAllRestaurants();

        } else {
            answer = "Я пока не знаю такую команду. Напиши /help.";
        }

        sendText(chatId, answer);
    }

    private String getAllRestaurants() {
        List<Restaurant> restaurants = repository.findAll();

        if (restaurants == null || restaurants.isEmpty()) {
            return "В базе пока нет заведений.";
        }

        StringBuilder result = new StringBuilder();
        result.append("Список заведений:\n\n");

        int count = 0;

        for (Restaurant restaurant : restaurants) {
            String restaurantText =
                    (count + 1) + ". " + restaurant.getName() + "\n"
                            + "Адрес: " + restaurant.getAddress() + "\n"
                            + "Кухня: " + restaurant.getCuisine() + "\n"
                            + "Средний чек: " + restaurant.getAverageCheck()
                            + "\n\n";

            // Оставляем запас до ограничения Telegram на длину сообщения.
            if (result.length() + restaurantText.length() > 3500) {
                result.append(
                        "Список сокращён: не все заведения поместились "
                                + "в одно сообщение."
                );
                break;
            }

            result.append(restaurantText);
            count++;
        }

        return result.toString();
    }

    private void sendText(long chatId, String text) {
        SendMessage message = new SendMessage(
                String.valueOf(chatId),
                text
        );

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println(
                    "Не удалось отправить сообщение в Telegram."
            );
            e.printStackTrace();
        }
    }
}
