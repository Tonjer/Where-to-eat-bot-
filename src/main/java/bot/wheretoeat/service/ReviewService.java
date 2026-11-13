package bot.wheretoeat.service;

import bot.wheretoeat.model.Review;
import bot.wheretoeat.model.ReviewStatus;
import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.ReviewRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    public ReviewService(ReviewRepository reviewRepository, RestaurantRepository restaurantRepository) {
        this.reviewRepository = reviewRepository;
        this.restaurantRepository = restaurantRepository;
    }

    // Добавление нового отзыва TODO если нужны комментарии к методам, то пишите javadoc
    // TODO(refactor P1): Задать единый предел длины текста и проверять его здесь для любого интерфейса.
    // Ресторан получать через findById; проверки оценки/текста и начальный PENDING оставить в сервисе.
    public void addReview(
            String restaurantId,
            long telegramUserId,
            String username,
            int rating,
            String text
    ) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException( // TODO можно и в одну строку, макс длина строки до 140 символов это база
                    "Оценка должна быть от 1 до 5"
            );
        }

        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Текст отзыва не может быть пустым"
            );
        }

        boolean restaurantExists = restaurantRepository.findAll()
                .stream()
                .anyMatch(restaurant ->
                        restaurant.getId().equals(restaurantId)
                );

        if (!restaurantExists) {
            throw new IllegalArgumentException(
                    "Ресторан не найден"
            );
        }

        Review review = new Review();

        review.setId(UUID.randomUUID().toString());
        review.setRestaurantId(restaurantId);
        review.setTelegramUserId(telegramUserId);
        review.setUsername(username);
        review.setRating(rating);
        review.setText(text);
        review.setStatus(ReviewStatus.PENDING);
        review.setCreatedAt(System.currentTimeMillis());

        reviewRepository.save(review);
    }

    // Получение одобренных отзывов конкретного ресторана
    public List<Review> getApprovedReviews(String restaurantId) {
        return reviewRepository.findAll()
                .stream()
                .filter(review ->
                        review.getRestaurantId().equals(restaurantId)
                )
                .filter(review ->
                        review.getStatus() == ReviewStatus.APPROVED
                )
                .collect(Collectors.toList());
    }

    // Получение отзывов, ожидающих модерации
    public List<Review> getPendingReviews() {
        return reviewRepository.findAll()
                .stream()
                .filter(review ->
                        review.getStatus() == ReviewStatus.PENDING
                )
                .collect(Collectors.toList());
    }

    // Одобрение отзыва
    // TODO(refactor P1): Не менять объект репозитория до успешной записи: сохранять копию с новым статусом.
    // Объединить approve/reject приватным методом, сохранив проверку PENDING и общий порядок записи.
    // Проверить: отказ записи не меняет статус, повторное решение не выполняется второй раз.
    public synchronized boolean approveReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId);

        if (review == null) {
            throw new IllegalArgumentException(
                    "Отзыв не найден"
            );
        }
        if (review.getStatus() != ReviewStatus.PENDING){
            return false;
        }
        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.update(review);
        return true;
    }

    // Отклонение отзыва
    public synchronized boolean rejectReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId);

        if (review == null) {
            throw new IllegalArgumentException(
                    "Отзыв не найден"
            );
        }
        if (review.getStatus() != ReviewStatus.PENDING){
            return false;
        }
        review.setStatus(ReviewStatus.REJECTED);
        reviewRepository.update(review);
        return true;
    }

    // Расчёт средней оценки ресторана
    public double getAverageRating(String restaurantId) {
        List<Review> reviews = getApprovedReviews(restaurantId);

        if (reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}