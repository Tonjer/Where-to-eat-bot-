package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TelegramCommandHandler {

    private static final long SESSION_TTL_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final TelegramHelpHandler helpHandler;
    private final TelegramRestaurantHandler restaurantHandler;
    private final TelegramFilterHandler filterHandler;
    private final TelegramReviewHandler reviewHandler;
    private final TelegramAdminHandler adminHandler;
    private final String botUsername;

    private final Map<UserSession.SessionKey, UserSession> sessions = new ConcurrentHashMap<>();

    public TelegramCommandHandler(
            TelegramHelpHandler helpHandler,
            TelegramRestaurantHandler restaurantHandler,
            TelegramFilterHandler filterHandler,
            TelegramReviewHandler reviewHandler,
            TelegramAdminHandler adminHandler
    ) {
        this(helpHandler, restaurantHandler, filterHandler, reviewHandler, adminHandler, "Where_ToEat_bot");
    }

    public TelegramCommandHandler(
            TelegramHelpHandler helpHandler,
            TelegramRestaurantHandler restaurantHandler,
            TelegramFilterHandler filterHandler,
            TelegramReviewHandler reviewHandler,
            TelegramAdminHandler adminHandler,
            String botUsername
    ) {
        this.helpHandler = helpHandler;
        this.restaurantHandler = restaurantHandler;
        this.filterHandler = filterHandler;
        this.reviewHandler = reviewHandler;
        this.adminHandler = adminHandler;
        this.botUsername = botUsername;
    }

    public BotResponse routeMessage(long chatId, long userId, String username, String text) {
        if (text == null || text.isBlank()) {
            return BotResponse.ofText("Напиши /help для просмотра команд.");
        }

        ParsedCommand parsed = ParsedCommand.parse(text);
        if (parsed.isCommand() && parsed.isAddressedToOtherBot(botUsername)) {
            return BotResponse.empty();
        }

        UserSession session = getOrCreateSession(chatId, userId);

        synchronized (session) {
            if (parsed.isCommand()) {
                String cmd = parsed.getCommand();
                if ("/help".equals(cmd)) {
                    return BotResponse.ofText(helpHandler.getHelpMessage());
                }
                if ("/cancel".equals(cmd)) {
                    boolean wasWritingReview = session.isWritingReview();
                    session.cancelCurrentStep();
                    return BotResponse.ofText(
                            wasWritingReview
                                    ? "Написание отзыва отменено."
                                    : "Текущий выбор отменён. Напиши /filters."
                    );
                }
            }

            if (parsed.isCommand() && adminHandler.isAdminCommand(parsed.getCommand())) {
                return adminHandler.handleAdminCommand(userId, parsed);
            }

            if (session.isWritingReview()) {
                if (parsed.isCommand()) {
                    return BotResponse.ofText(
                            "Сначала заверши написание отзыва или напиши /cancel для отмены."
                    );
                }
                return BotResponse.ofText(
                        reviewHandler.handleReviewStep(session, userId, username, parsed.getRawText())
                );
            }

            if (parsed.isCommand()) {
                return switch (parsed.getCommand()) {
                    case "/start" -> BotResponse.ofText(helpHandler.getStartMessage());
                    case "/all" -> BotResponse.ofText(restaurantHandler.getAllRestaurantsMessage());
                    case "/reviews" -> BotResponse.ofText(reviewHandler.handleReviewsCommand(parsed.getArguments()));
                    case "/write_review" -> BotResponse.ofText(reviewHandler.startReview(session, parsed.getArguments()));
                    case "/filters", "/cuisine", "/dish", "/price", "/search", "/reset" ->
                            filterHandler.handleCommand(session, parsed);
                    default -> BotResponse.ofText(helpHandler.getUnknownCommandMessage());
                };
            }

            return filterHandler.handleStepInput(session, parsed.getRawText());
        }
    }

    public BotResponse routeCallback(long chatId, long userId, String callbackData) {
        if (callbackData == null || callbackData.isBlank()) {
            return BotResponse.ofCallback("Некорректная команда.");
        }

        UserSession session = getOrCreateSession(chatId, userId);

        synchronized (session) {
            if (callbackData.startsWith("restaurant_review:")) {
                String restaurantId = callbackData.substring("restaurant_review:".length());
                List<Restaurant> lastResults = session.getLastSearchResults();

                if (lastResults.isEmpty()) {
                    String msg = "Сначала выполни поиск ресторанов командой /search.";
                    return BotResponse.ofCallbackAndMessages(msg, List.of(new BotResponse.OutgoingMessage(msg)));
                }

                boolean restaurantWasFound = lastResults.stream()
                        .anyMatch(r -> restaurantId.equals(r.getId()));
                if (!restaurantWasFound) {
                    String msg = "Этот ресторан не найден в твоих последних результатах поиска. Выполни /search ещё раз.";
                    return BotResponse.ofCallbackAndMessages(msg, List.of(new BotResponse.OutgoingMessage(msg)));
                }

                String startText = reviewHandler.startReview(session, restaurantId);
                return BotResponse.ofCallbackAndMessages(
                        "Открываем написание отзыва.",
                        List.of(new BotResponse.OutgoingMessage(startText))
                );
            }

            String adminCallbackAnswer = adminHandler.handleCallback(userId, callbackData);
            return BotResponse.ofCallback(adminCallbackAnswer);
        }
    }

    private UserSession getOrCreateSession(long chatId, long userId) {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now, SESSION_TTL_MILLIS));

        UserSession.SessionKey key = new UserSession.SessionKey(chatId, userId);
        UserSession session = sessions.computeIfAbsent(key, k -> new UserSession());
        session.touch(now);
        return session;
    }
}