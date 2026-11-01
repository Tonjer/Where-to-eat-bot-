package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class TelegramCommandHandler {

    private final Map<String, TelegramCommand> commands = new HashMap<>();

    private final TelegramFilterHandler filterHandler;
    private final TelegramReviewHandler reviewHandler;
    private final TelegramAdminHandler adminHandler;
    private final Map<Long, Long> searchOwners = new HashMap<>();

    public TelegramCommandHandler(TelegramHelpHandler helpHandler, TelegramRestaurantHandler restaurantHandler, TelegramFilterHandler filterHandler, TelegramReviewHandler reviewHandler, TelegramAdminHandler adminHandler) {
        this.filterHandler = filterHandler;
        this.reviewHandler = reviewHandler;
        this.adminHandler = adminHandler;

        commands.put(
                "/start",
                (chatId, text) -> helpHandler.getStartMessage()
        );

        commands.put(
                "/help",
                (chatId, text) -> helpHandler.getHelpMessage()
        );

        commands.put(
                "/all",
                (chatId, text) ->
                        restaurantHandler.getAllRestaurantsMessage()
        );

        commands.put(
                "/reviews",
                (chatId, text) ->
                        reviewHandler.handleReviewsCommand(text)
        );

        commands.put(
                "/write_review",
                reviewHandler::startReview
        );

        commands.put("/filters", filterHandler::handleMessage);
        commands.put("/cuisine", filterHandler::handleMessage);
        commands.put("/dish", filterHandler::handleMessage);
        commands.put("/price", filterHandler::handleMessage);
        commands.put("/search", filterHandler::handleMessage);
        commands.put("/reset", filterHandler::handleMessage);
        commands.put("/cancel", filterHandler::handleMessage);
    }

    public String handleMessage(long chatId, long userId, String username, String text) {
        if (text == null || text.isBlank()) {
            return "Напиши /help для просмотра команд.";
        }
        String adminAnswer = adminHandler.handleAdminCommand(chatId, userId, text);
        if (adminAnswer != null) {
            return adminAnswer;
        }

        String reviewAnswer = reviewHandler.handleIncomingMessage(chatId, userId, username, text);

        if (reviewAnswer != null) {
            return reviewAnswer;
        }

        String command = text.trim().split("\\s+")[0];

        int atIndex = command.indexOf('@');

        if (atIndex >= 0) {
            command = command.substring(0, atIndex);
        }

        command = command.toLowerCase();

        if (command.equals("/search")) {
            searchOwners.put(chatId, userId);
        }

        TelegramCommand handler = commands.get(command);

        if (handler != null) {
            return handler.execute(chatId, text);
        }

        return filterHandler.handleMessage(chatId, text);
    }

    public java.util.List<bot.wheretoeat.model.Review> getPendingReviewsForAdmin(long userId) {
        return adminHandler.getPendingReviewsForAdmin(userId);
    }

    public String handleAdminCallback(long userId, String callbackData) {
        return adminHandler.handleCallback(userId, callbackData);
    }

    public List<Restaurant> getLastSearchResults(long chatId) {
        return filterHandler.getLastSearchResults(chatId);
    }

    public String startReviewFromSearch(long chatId, long userId, String restaurantId) {
        Long searchOwner = searchOwners.get(chatId);

        if (searchOwner == null || searchOwner != userId) {
            return "Сначала выполни поиск ресторанов командой /search.";
        }

        boolean restaurantWasFound = filterHandler.getLastSearchResults(chatId).stream().anyMatch(restaurant -> restaurantId.equals(restaurant.getId()));

        if (!restaurantWasFound) {
            return "Этот ресторан не найден в твоих последних результатах поиска. Выполни /search ещё раз.";
        }

        return reviewHandler.startReview(chatId, "/write_review " + restaurantId);
    }
}
