package bot.wheretoeat.telegram;

import java.util.HashMap;
import java.util.Map;

public class TelegramCommandHandler {
    private final Map<String, TelegramCommand> commands = new HashMap<>();
    private final TelegramFilterHandler filterHandler;

    public TelegramCommandHandler(TelegramHelpHandler helpHandler, TelegramRestaurantHandler restaurantHandler, TelegramFilterHandler filterHandler) {
        this.filterHandler = filterHandler;
        commands.put("/start", (chatId, text) -> helpHandler.getStartMessage());
        commands.put("/help", (chatId, text) -> helpHandler.getHelpMessage());
        commands.put("/all", (chatId, text) -> restaurantHandler.getAllRestaurantsMessage());

        commands.put("/filters", filterHandler::handleMessage);
        commands.put("/cuisine", filterHandler::handleMessage);
        commands.put("/dish", filterHandler::handleMessage);
        commands.put("/price", filterHandler::handleMessage);
        commands.put("/search", filterHandler::handleMessage);
        commands.put("/reset", filterHandler::handleMessage);
        commands.put("/cancel", filterHandler::handleMessage);
    }

    public String handleMessage(long chatId, String text) {
        if (text == null || text.isBlank()) {
            return "Напиши /help для просмотра команд.";
        }

        String command = text.trim().split("\\s+")[0];

        int atIndex = command.indexOf('@');

        if (atIndex >= 0) {
            command = command.substring(0, atIndex);
        }

        command = command.toLowerCase();

        TelegramCommand handler = commands.get(command);

        if (handler != null) {
            return handler.execute(chatId, text);
        }

        return filterHandler.handleMessage(chatId, text);
    }
}
