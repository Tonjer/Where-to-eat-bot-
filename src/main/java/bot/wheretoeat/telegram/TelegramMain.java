
package bot.wheretoeat.telegram;

import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.RestaurantRepositoryImpl;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramMain {

    public static void main(String[] args) {
        // Получаем токен из переменных окружения.
        String botToken = System.getenv("Telegram_Bot_Token");

        if (botToken == null || botToken.isBlank()) {
            System.err.println(
                    "Ошибка: не задана переменная окружения "
                            + "Telegram_Bot_Token."
            );
            return;
        }

        // Username твоего Telegram-бота без символа @.
        String botUsername = "Where_ToEat_bot";

        try {
            // Создаём репозиторий, который загружает рестораны из JSON.
            RestaurantRepository repository =
                    new RestaurantRepositoryImpl();

            // Подключаем приложение к Telegram Bot API.
            TelegramBotsApi botsApi =
                    new TelegramBotsApi(DefaultBotSession.class);

            // Регистрируем бота и передаём ему репозиторий.
            botsApi.registerBot(
                    new TelegramBot(
                            botUsername,
                            botToken,
                            repository
                    )
            );

            System.out.println("Telegram-бот запущен.");
            System.out.println("Имя бота: @" + botUsername);

        } catch (TelegramApiException e) {
            System.err.println(
                    "Не удалось запустить Telegram-бота."
            );
            e.printStackTrace();

        } catch (RuntimeException e) {
            System.err.println(
                    "Не удалось загрузить базу ресторанов."
            );
            e.printStackTrace();
        }
    }
}
