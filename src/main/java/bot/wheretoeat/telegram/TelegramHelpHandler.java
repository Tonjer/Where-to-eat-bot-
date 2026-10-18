package bot.wheretoeat.telegram;

public class TelegramHelpHandler {

    public String getStartMessage() {
        return """
                🍽️ Добро пожаловать в «Где поесть»!

                Я помогу тебе найти ресторан по кухне,
                блюдам и средней стоимости заказа.

                Нажми /help, чтобы посмотреть доступные команды.
                """;
    }

    public String getHelpMessage() {
        return """
                📋 Основные команды:
                
                /start — запустить бота
                /help — показать список команд
                /all — показать все рестораны
                /filters — подобрать рестораны по фильтрам

                Чтобы начать поиск, выбери /filters.
                """;
    }

    public String getUnknownCommandMessage() {
        return """
                ❓ Неизвестная команда.

                Используй одну из основных команд:
                /start
                /help
                /all
                /filters
                """;
    }
}