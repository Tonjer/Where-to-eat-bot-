package bot.wheretoeat.service;

import bot.wheretoeat.model.Review;
import bot.wheretoeat.model.ReviewStatus;
import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.ReviewRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReviewService {

    public static final int MAX_REVIEW_TEXT_LENGTH = 1000;

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    public ReviewService(ReviewRepository reviewRepository, RestaurantRepository restaurantRepository) {
        this.reviewRepository = reviewRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public void addReview(
            String restaurantId,
            long telegramUserId,
            String username,
            int rating,
            String text
    ) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 5");
        }

        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Текст отзыва не может быть пустым");
        }

        String trimmedText = text.trim();
        if (trimmedText.length() > MAX_REVIEW_TEXT_LENGTH) {
            throw new IllegalArgumentException("Текст отзыва не может превышать " + MAX_REVIEW_TEXT_LENGTH + " символов");
        }
        if (restaurantId == null || restaurantRepository.findById(restaurantId) == null) {
            throw new IllegalArgumentException("Ресторан не найден");
        }

        Review review = new Review(
                UUID.randomUUID().toString(),
                restaurantId,
                telegramUserId,
                username,
                rating,
                trimmedText,
                ReviewStatus.PENDING,
                System.currentTimeMillis()
        );

        reviewRepository.save(review);
    }

    public List<Review> getApprovedReviews(String restaurantId) {
        return reviewRepository.findAll().stream()
                .filter(review -> review.getRestaurantId().equals(restaurantId))
                .filter(review -> review.getStatus() == ReviewStatus.APPROVED)
                .collect(Collectors.toList());
    }

    public List<Review> getPendingReviews() {
        return reviewRepository.findAll().stream()
                .filter(review -> review.getStatus() == ReviewStatus.PENDING)
                .collect(Collectors.toList());
    }

    // TODO(refactor P1): Не менять объект репозитория до успешной записи: сохранять копию с новым статусом.
    // Объединить approve/reject приватным методом, сохранив проверку PENDING и общий порядок записи.
    // Проверить: отказ записи не меняет статус, повторное решение не выполняется второй раз.
    public synchronized boolean approveReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId);

        if (review == null) {
            throw new IllegalArgumentException("Отзыв не найден");
        }
        if (review.getStatus() != ReviewStatus.PENDING) {
            return false;
        }
        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.update(review);
        return true;
    }

    public synchronized boolean rejectReview(String reviewId) {
        Review review = reviewRepository.findById(reviewId);

        if (review == null) {
            throw new IllegalArgumentException("Отзыв не найден");
        }
        if (review.getStatus() != ReviewStatus.PENDING) {
            return false;
        }
        review.setStatus(ReviewStatus.REJECTED);
        reviewRepository.update(review);
        return true;
    }

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