package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.model.Review;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.service.ReviewService;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TelegramAdminHandler {

    private final ReviewService reviewService;
    private final RestaurantService restaurantService;
    private final Set<Long> adminIds;

    public TelegramAdminHandler(ReviewService reviewService, Set<Long> adminIds) {
        this(reviewService, null, adminIds);
    }

    public TelegramAdminHandler(ReviewService reviewService, RestaurantService restaurantService, Set<Long> adminIds) {
        this.reviewService = reviewService;
        this.restaurantService = restaurantService;
        this.adminIds = adminIds;
    }

    public boolean isAdmin(long userId) {
        return adminIds.contains(userId);
    }

    public boolean isAdminCommand(String command) {
        return "/admin".equals(command)
                || "/pending".equals(command)
                || "/approve".equals(command)
                || "/reject".equals(command)
                || "/add_restaurant".equals(command);
    }

    public BotResponse handleAdminCommand(long userId, ParsedCommand parsed) {
        String command = parsed.getCommand();
        if (!isAdminCommand(command)) {
            return null;
        }

        if (!isAdmin(userId)) {
            return BotResponse.ofText("У тебя нет доступа к административной панели.");
        }

        String args = parsed.getArguments();

        switch (command) {
            case "/admin":
                return BotResponse.ofText("""
                        🛠 Административная панель
                        
                        /pending — новые отзывы
                        /approve ID — одобрить отзыв
                        /reject ID — отклонить отзыв
                        /add_restaurant Название | Адрес | Кухня | Блюдо1, Блюдо2 | СреднийЧек — добавить ресторан
                        """);

            case "/pending":
                return getPendingReviewsResponse();

            case "/approve":
                if (args.isEmpty()) {
                    return BotResponse.ofText("Укажи ID отзыва: /approve ID");
                }
                return BotResponse.ofText(approveReview(args.split("\\s+")[0]));

            case "/reject":
                if (args.isEmpty()) {
                    return BotResponse.ofText("Укажи ID отзыва: /reject ID");
                }
                return BotResponse.ofText(rejectReview(args.split("\\s+")[0]));

            case "/add_restaurant":
                if (args.isEmpty()) {
                    return BotResponse.ofText("Формат: /add_restaurant Название | Адрес | Кухня | Блюдо1, Блюдо2 | СреднийЧек");
                }
                return BotResponse.ofText(addRestaurant(args));

            default:
                return null;
        }
    }

    private BotResponse getPendingReviewsResponse() {
        List<Review> reviews = reviewService.getPendingReviews();

        if (reviews.isEmpty()) {
            return BotResponse.ofText("Новых отзывов пока нет.");
        }

        List<BotResponse.OutgoingMessage> messages = new ArrayList<>();
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

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(List.of(List.of(approveButton, rejectButton)));

            messages.add(new BotResponse.OutgoingMessage(text, keyboard));
        }

        return BotResponse.ofMessages(messages);
    }

    private String addRestaurant(String rawArgs) {
        if (restaurantService == null) {
            return "Сервис ресторанов не настроен.";
        }
        String[] fields = rawArgs.split("\\|");
        if (fields.length != 5) {
            return "Неверный формат. Используй: /add_restaurant Название | Адрес | Кухня | Блюдо1, Блюдо2 | СреднийЧек";
        }
        try {
            String name = fields[0].trim();
            String address = fields[1].trim();
            String cuisine = fields[2].trim();
            List<String> dishes = Arrays.stream(fields[3].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            int averageCheck = Integer.parseInt(fields[4].trim());

            Restaurant saved = restaurantService.addRestaurant(name, address, cuisine, dishes, averageCheck);
            return "✅ Ресторан добавлен! ID: " + saved.getId();
        } catch (NumberFormatException e) {
            return "Средний чек должен быть числом.";
        } catch (RuntimeException e) {
            return "Не удалось добавить ресторан: " + e.getMessage();
        }
    }

    public String handleCallback(long userId, String callbackData) {
        if (!isAdmin(userId)) {
            return "У тебя нет доступа к модерации.";
        }

        if (callbackData == null) {
            return "Некорректная команда.";
        }

        String[] parts = callbackData.split(":", 2);

        if (parts.length != 2) {
            return "Некорректные данные кнопки.";
        }

        String action = parts[0];
        String reviewId = parts[1];

        if (action.equals("approve")) {
            return approveReview(reviewId);
        }

        if (action.equals("reject")) {
            return rejectReview(reviewId);
        }

        return "Неизвестное действие.";
    }

    private String approveReview(String reviewId) {
        try {
            boolean approved = reviewService.approveReview(reviewId);

            if (!approved) {
                return "Этот отзыв уже был обработан другим администратором.";
            }

            return "✅ Отзыв успешно одобрен.";

        } catch (IllegalArgumentException e) {
            return "Не удалось одобрить отзыв: " + e.getMessage();
        } catch (RuntimeException e) {
            return "Не удалось сохранить решение по отзыву из-за ошибки хранилища.";
        }
    }

    private String rejectReview(String reviewId) {
        try {
            boolean rejected = reviewService.rejectReview(reviewId);

            if (!rejected) {
                return "Этот отзыв уже был обработан другим администратором.";
            }

            return "❌ Отзыв отклонён.";

        } catch (IllegalArgumentException e) {
            return "Не удалось отклонить отзыв: " + e.getMessage();
        } catch (RuntimeException e) {
            return "Не удалось сохранить решение по отзыву из-за ошибки хранилища.";
        }
    }
}