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

    public ReviewService(
            ReviewRepository reviewRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.restaurantRepository = restaurantRepository;
    }

    // Добавление нового отзыва
    public void addReview(
            String restaurantId,
            long telegramUserId,
            String username,
            int rating,
            String text
    ) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
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