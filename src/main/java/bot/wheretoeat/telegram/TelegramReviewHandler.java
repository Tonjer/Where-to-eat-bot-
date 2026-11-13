package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.model.Review;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.service.ReviewService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramReviewHandler {

    private final ReviewService reviewService;
    private final RestaurantService restaurantService;

    public TelegramReviewHandler(ReviewService reviewService, RestaurantService restaurantService) {
        this.reviewService = reviewService;
        this.restaurantService = restaurantService;
    }

    private static class ReviewSession {
        private final String restaurantId;
        private int rating;

        private ReviewSession(String restaurantId) {
            this.restaurantId = restaurantId;
            this.rating = 0;
        }
    }

    // TODO(refactor P1): Перейти на общую сессию из TelegramCommandHandler; чужой пользователь чата
    // не должен продолжать/отменять отзыв. Вместо rating == 0 использовать шаги REVIEW_RATING и REVIEW_TEXT.
    private final Map<Long, ReviewSession> sessions = new HashMap<>();

    // TODO(refactor P2): Показывать отзывы страницами; добавить кнопку просмотра в карточку ресторана,
    // чтобы пользователю не приходилось угадывать ID. Среднее считать без повторной загрузки списка.
    public String handleReviewsCommand(String text) {
        String[] parts = text.trim().split("\\s+");

        if (parts.length < 2) {
            return "Укажи ID ресторана.\n" + "Формат команды: /reviews ID";
        }

        String restaurantId = parts[1];

        Restaurant restaurant = findRestaurant(restaurantId);

        if (restaurant == null) {
            return "Ресторан не найден.";
        }

        List<Review> reviews = reviewService.getApprovedReviews(restaurantId);

        double averageRating = reviewService.getAverageRating(restaurantId);

        StringBuilder result = new StringBuilder();

        result.append("⭐ Отзывы о ресторане ").append(restaurant.getName()).append("\n\n");

        if (reviews.isEmpty()) {
            result.append("Пока нет опубликованных отзывов.\n");
        } else {
            result.append(String.format(java.util.Locale.US, "Средняя оценка: %.1f из 5\n", averageRating));

            result.append("Количество отзывов: ").append(reviews.size()).append("\n\n");

            for (Review review : reviews) {
                String username = review.getUsername();

                if (username == null || username.isBlank()) {
                    username = "Пользователь";
                }

                result.append("👤 ").append(username).append("\n");

                result.append("⭐ ").append(review.getRating()).append(" из 5\n");

                result.append(review.getText()).append("\n\n");
            }
        }

        return result.toString();
    }

    public String startReview(long chatId, String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            return "Укажи ID ресторана.\n" + "Формат: /write_review ID";
        }
        String restaurantId = parts[1];
        Restaurant restaurant = findRestaurant(restaurantId);
        if (restaurant == null) {
            return "Ресторан не найден. Проверь ID.";
        }
        sessions.put(chatId, new ReviewSession(restaurantId));
        return "Ты собираешься оставить отзыв о ресторане " + restaurant.getName() + ".\n\n" + "Поставь оценку от 1 до 5.";
    }

    // TODO(refactor P1): Оставить здесь шаги диалога и разбор ввода; правила отзыва держать в ReviewService.
    // При ошибке записи сохранять черновик. Убрать обещание уведомления после модерации, пока его нет.
    public String handleIncomingMessage(long chatId, long userId, String username, String text) {
        ReviewSession session = sessions.get(chatId);
        if (session == null) {
            return null;
        }
        if (text.equalsIgnoreCase("/cancel")) {
            sessions.remove(chatId);
            return "Написание отзыва отменено.";
        }
        if (session.rating == 0) {
            try {
                int rating = Integer.parseInt(text.trim());
                if (rating < 1 || rating > 5) {
                    return "Оценка должна быть от 1 до 5. Попробуй ещё раз.";
                }
                session.rating = rating;
                return "Ты поставил оценку " + rating + " из 5.\n\n" + "Теперь напиши текст отзыва.";
            } catch (NumberFormatException e) {
                return "Пожалуйста, отправь число от 1 до 5.";
            }
        }
        String comment = text.trim();
        if (comment.isEmpty()) {
            return "Текст отзыва не может быть пустым. Напиши комментарий ещё раз.";
        }
        if (comment.startsWith("/")) {
            return "Сначала отправь текстовый комментарий или напиши /cancel для отмены.";
        }
        try {
            reviewService.addReview(session.restaurantId, userId, username, session.rating, comment);
            sessions.remove(chatId);

            return "Спасибо за отзыв! ⭐\n\n" + "Твой отзыв отправлен администраторам на модерацию.\n" + "После проверки ты получишь уведомление.";
        } catch (IllegalArgumentException e) {
            sessions.remove(chatId);
            return "Не удалось сохранить отзыв: " + e.getMessage();
        }
    }

    public boolean hasActiveSession(long chatId) {
        return sessions.containsKey(chatId);
    }

    private Restaurant findRestaurant(String restaurantId) {
        return restaurantService.findAll().stream().filter(r -> restaurantId.equals(r.getId())).findFirst().orElse(null);
    }

}
