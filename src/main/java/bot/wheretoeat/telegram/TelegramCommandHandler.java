
package bot.wheretoeat.telegram;

public class TelegramCommandHandler {

    private final TelegramHelpHandler helpHandler;
    private final TelegramRestaurantHandler restaurantHandler;
    private final TelegramFilterHandler filterHandler;

    public TelegramCommandHandler(
            TelegramHelpHandler helpHandler,
            TelegramRestaurantHandler restaurantHandler,
            TelegramFilterHandler filterHandler
    ) {
        this.helpHandler = helpHandler;
        this.restaurantHandler = restaurantHandler;
        this.filterHandler = filterHandler;
    }

    public String handleMessage(long chatId, String text) {
        if (text == null || text.isBlank()) {
            return helpHandler.getUnknownCommandMessage();
        }

        String command = text.trim();
        int spaceIndex = command.indexOf(' ');

        if (spaceIndex >= 0) {
            command = command.substring(0, spaceIndex);
        }

        int atIndex = command.indexOf('@');

        if (atIndex >= 0) {
            command = command.substring(0, atIndex);
        }

        command = command.toLowerCase();

        switch (command) {
            case "/start":
                return helpHandler.getStartMessage();

            case "/help":
                return helpHandler.getHelpMessage();

            case "/all":
                return restaurantHandler.getAllRestaurantsMessage();

            case "/filters":
            case "/cuisine":
            case "/dish":
            case "/price":
            case "/search":
            case "/reset":
            case "/cancel":
                return filterHandler.handleMessage(chatId, text);

            default:
                // Обычный текст может быть ответом на вопрос фильтра.
                return filterHandler.handleMessage(chatId, text);
        }
    }
}
