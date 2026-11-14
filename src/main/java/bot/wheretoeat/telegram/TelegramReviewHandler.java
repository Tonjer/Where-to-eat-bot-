package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.model.Review;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.service.ReviewService;

import java.util.List;

public class TelegramReviewHandler {

    private final ReviewService reviewService;
    private final RestaurantService restaurantService;

    public TelegramReviewHandler(ReviewService reviewService, RestaurantService restaurantService) {
        this.reviewService = reviewService;
        this.restaurantService = restaurantService;
    }

    // TODO(refactor P2): Показывать отзывы страницами; добавить кнопку просмотра в карточку ресторана,
    // чтобы пользователю не приходилось угадывать ID. Среднее считать без повторной загрузки списка.
    public String handleReviewsCommand(String arguments) {
        String restaurantId = arguments == null ? "" : arguments.trim();

        if (restaurantId.isEmpty()) {
            return "Укажи ID ресторана.\n" + "Формат команды: /reviews ID";
        }

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

    public String startReview(UserSession session, String arguments) {
        String restaurantId = arguments == null ? "" : arguments.trim();
        if (restaurantId.isEmpty()) {
            return "Укажи ID ресторана.\n" + "Формат: /write_review ID";
        }
        Restaurant restaurant = findRestaurant(restaurantId);
        if (restaurant == null) {
            return "Ресторан не найден. Проверь ID.";
        }
        session.startReview(restaurantId);
        return "Ты собираешься оставить отзыв о ресторане " + restaurant.getName() + ".\n\n" + "Поставь оценку от 1 до 5.";
    }

    public String handleReviewStep(UserSession session, long userId, String username, String text) {
        if (session.getStep() == UserSession.Step.REVIEW_RATING) {
            try {
                int rating = Integer.parseInt(text.trim());
                if (rating < 1 || rating > 5) {
                    return "Оценка должна быть от 1 до 5. Попробуй ещё раз.";
                }
                session.setReviewRating(rating);
                session.setStep(UserSession.Step.REVIEW_TEXT);
                return "Ты поставил оценку " + rating + " из 5.\n\n" + "Теперь напиши текст отзыва.";
            } catch (NumberFormatException e) {
                return "Пожалуйста, отправь число от 1 до 5.";
            }
        }

        if (session.getStep() == UserSession.Step.REVIEW_TEXT) {
            String comment = text.trim();
            if (comment.isEmpty()) {
                return "Текст отзыва не может быть пустым. Напиши комментарий ещё раз.";
            }

            session.setReviewDraftText(comment);
            try {
                reviewService.addReview(
                        session.getReviewRestaurantId(),
                        userId,
                        username,
                        session.getReviewRating(),
                        comment
                );
                session.clearReview();
                return "Спасибо за отзыв! ⭐\n\n"
                        + "Твой отзыв отправлен администраторам на модерацию.";
            } catch (IllegalArgumentException e) {
                return "Не удалось сохранить отзыв: " + e.getMessage()
                        + "\nИсправь текст или отправь /cancel для отмены.";
            } catch (RuntimeException e) {
                return "Ошибка записи отзыва. Черновик сохранён — попробуй отправить текст ещё раз или нажми /cancel.";
            }
        }

        return null;
    }

    private Restaurant findRestaurant(String restaurantId) {
        return restaurantService.findAll().stream()
                .filter(r -> restaurantId.equals(r.getId()))
                .findFirst()
                .orElse(null);
    }
}