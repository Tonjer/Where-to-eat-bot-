
package bot.wheretoeat.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramMain {

    public static void main(String[] args) {
        String botToken = System.getenv("Where_ToEat_bot");

        if (botToken == null || botToken.isBlank()) {
            System.err.println(
                    "Не задана переменная окружения Where_ToEat_bot"
            );
            return;
        }

        // Укажи username своего бота без символа @.
        String botUsername = "Where_ToEat_bot";

        try {
            TelegramBotsApi botsApi =
                    new TelegramBotsApi(DefaultBotSession.class);

            botsApi.registerBot(
                    new TelegramBot(botUsername, botToken)
            );

            System.out.println("Telegram-бот запущен.");

        } catch (TelegramApiException e) {
            System.err.println("Не удалось запустить Telegram-бота.");
            e.printStackTrace();
        }
    }
}
