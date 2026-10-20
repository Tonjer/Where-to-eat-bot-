
package bot.wheretoeat.telegram;

import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.RestaurantRepositoryImpl;
import bot.wheretoeat.service.RestaurantService;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramMain {

    public static void main(String[] args) {
        String botToken = System.getenv("Telegram_Bot_Token");

        if (botToken == null || botToken.isBlank()) {
            System.err.println(
                    "Ошибка: не задана переменная окружения "
                            + "Telegram_Bot_Token."
            );
            return;
        }

        String botUsername = "Where_ToEat_bot";

        try {
            RestaurantRepository repository =
                    new RestaurantRepositoryImpl();

            RestaurantService service =
                    new RestaurantService(repository);

            TelegramHelpHandler helpHandler =
                    new TelegramHelpHandler();

            TelegramRestaurantHandler restaurantHandler =
                    new TelegramRestaurantHandler(service);

            TelegramFilterHandler filterHandler =
                    new TelegramFilterHandler(service, restaurantHandler);

            TelegramCommandHandler commandHandler =
                    new TelegramCommandHandler(
                            helpHandler,
                            restaurantHandler,
                            filterHandler
                    );

            TelegramBot bot = new TelegramBot(
                    botUsername,
                    botToken,
                    commandHandler
            );

            TelegramBotsApi botsApi =
                    new TelegramBotsApi(DefaultBotSession.class);

            botsApi.registerBot(bot);

            System.out.println("Telegram-бот запущен.");
            System.out.println("Имя бота: @" + botUsername);

        } catch (TelegramApiException e) {
            System.err.println("Не удалось запустить Telegram-бота.");
            e.printStackTrace();

        } catch (RuntimeException e) {
            System.err.println(
                    "Ошибка при создании приложения "
                            + "или загрузке базы ресторанов."
            );
            e.printStackTrace();
        }
    }
}
