package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.model.Review;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.service.ReviewService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TelegramAdminHandler {

    private final ReviewService reviewService;
    private final RestaurantService restaurantService;
    private final Set<Long> adminIds;
    ;

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

    public String handleAdminCommand(long chatId, long userId, String text) {
        String[] parts = text.trim().split("\\s+", 2);
        String command = parts[0].split("@")[0].toLowerCase();

        if (!command.equals("/admin")
                && !command.equals("/pending")
                && !command.equals("/approve")
                && !command.equals("/reject")
                && !command.equals("/add_restaurant")) {
            return null;
        }

        if (!isAdmin(userId)) {
            return "У тебя нет доступа к административной панели.";
        }

        switch (command) {
            case "/admin":
                return """
                        🛠 Административная панель
                        
                        /pending — новые отзывы
                        /approve ID — одобрить отзыв
                        /reject ID — отклонить отзыв
                        /add_restaurant Название | Адрес | Кухня | Блюдо1, Блюдо2 | СреднийЧек — добавить ресторан
                        """;

            case "/pending":
                return getPendingReviewsMessage();

            case "/approve":
                if (parts.length < 2 || parts[1].isBlank()) {
                    return "Укажи ID отзыва: /approve ID";
                }
                return approveReview(parts[1].trim());

            case "/reject":
                if (parts.length < 2 || parts[1].isBlank()) {
                    return "Укажи ID отзыва: /reject ID";
                }
                return rejectReview(parts[1].trim());

            case "/add_restaurant":
                if (parts.length < 2 || parts[1].isBlank()) {
                    return "Формат: /add_restaurant Название | Адрес | Кухня | Блюдо1, Блюдо2 | СреднийЧек";
                }
                return addRestaurant(parts[1]);

            default:
                return null;
        }
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
        } catch (IllegalArgumentException e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    private String getPendingReviewsMessage() {
        List<Review> reviews = reviewService.getPendingReviews();

        if (reviews.isEmpty()) {
            return "Новых отзывов пока нет.";
        }

        StringBuilder result = new StringBuilder("📋 Отзывы, ожидающие модерации:\n\n");

        for (Review review : reviews) {
            String username = review.getUsername();

            if (username == null || username.isBlank()) {
                username = "Пользователь";
            }

            result.append("🆔 ID: ").append(review.getId()).append("\n");

            result.append("🍽 ID ресторана: ").append(review.getRestaurantId()).append("\n");

            result.append("👤 Автор: ").append(username).append("\n");

            result.append("⭐ Оценка: ").append(review.getRating()).append("/5\n");

            result.append("💬 Отзыв: ").append(review.getText()).append("\n");

            result.append("Одобрить: /approve ").append(review.getId()).append("\n");

            result.append("Отклонить: /reject ").append(review.getId()).append("\n\n");
        }

        return result.toString();
    }

    public List<Review> getPendingReviewsForAdmin(long userId) {
        if (!isAdmin(userId)) {
            return null;
        }
        return reviewService.getPendingReviews();
    }

    // TODO(refactor P1): Сохранить проверку adminIds и для команд, и для callback при объединении роутера.
    // Правила перехода статуса оставить в ReviewService; ошибки записи возвращать как понятный отказ.
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
        }
    }
}