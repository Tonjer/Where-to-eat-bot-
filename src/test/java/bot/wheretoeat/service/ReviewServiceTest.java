package bot.wheretoeat.service;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.model.Review;
import bot.wheretoeat.model.ReviewStatus;
import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ReviewServiceTest {
    private MemoryReviews reviews;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        MemoryRestaurants restaurants = new MemoryRestaurants();
        restaurants.save(new Restaurant("r1", "Cafe", "Address", "European", List.of("Soup"), 700));
        reviews = new MemoryReviews();
        service = new ReviewService(reviews, restaurants);
    }

    @Test
    void addReviewStoresTrimmedPendingReview() {
        service.addReview("r1", 42L, "tester", 5, "  Great food!  ");

        assertEquals(1, reviews.findAll().size());
        Review saved = reviews.findAll().getFirst();
        assertEquals("r1", saved.getRestaurantId());
        assertEquals(42L, saved.getTelegramUserId());
        assertEquals("tester", saved.getUsername());
        assertEquals(5, saved.getRating());
        assertEquals("Great food!", saved.getText());
        assertEquals(ReviewStatus.PENDING, saved.getStatus());
    }

    @Test
    void addReviewRejectsRatingOutsideOneToFive() {
        assertThrows(IllegalArgumentException.class, () -> service.addReview("r1", 1, "user", 0, "Text"));
        assertThrows(IllegalArgumentException.class, () -> service.addReview("r1", 1, "user", 6, "Text"));
    }

    @Test
    void addReviewRejectsEmptyOrTooLongText() {
        assertThrows(IllegalArgumentException.class, () -> service.addReview("r1", 1, "user", 4, "  "));
        assertThrows(IllegalArgumentException.class, () -> service.addReview("r1", 1, "user", 4, "x".repeat(1001)));
    }

    @Test
    void addReviewRejectsUnknownRestaurant() {
        assertThrows(IllegalArgumentException.class, () -> service.addReview("missing", 1, "user", 4, "Text"));
        assertThrows(IllegalArgumentException.class, () -> service.addReview(null, 1, "user", 4, "Text"));
    }

    @Test
    void onlyApprovedReviewsAreReturnedAndAveraged() {
        reviews.save(review("a", "r1", 4, ReviewStatus.APPROVED));
        reviews.save(review("b", "r1", 2, ReviewStatus.APPROVED));
        reviews.save(review("c", "r1", 5, ReviewStatus.PENDING));
        reviews.save(review("d", "other", 1, ReviewStatus.APPROVED));

        assertEquals(3.0, service.getAverageRating("r1"), 0.001);
    }

    @Test
    void averageRatingIsZeroWhenThereAreNoApprovedReviews() {
        reviews.save(review("pending", "r1", 5, ReviewStatus.PENDING));
        assertEquals(0.0, service.getAverageRating("r1"));
    }

    @Test
    void pendingReviewsCanBeApprovedAndRejected() {
        reviews.save(review("approve", "r1", 4, ReviewStatus.PENDING));
        reviews.save(review("reject", "r1", 2, ReviewStatus.PENDING));

        assertTrue(service.approveReview("approve"));
        assertTrue(service.rejectReview("reject"));
        assertEquals(ReviewStatus.APPROVED, reviews.findById("approve").getStatus());
        assertEquals(ReviewStatus.REJECTED, reviews.findById("reject").getStatus());
    }

    @Test
    void alreadyProcessedReviewCannotBeProcessedAgain() {
        reviews.save(review("done", "r1", 4, ReviewStatus.APPROVED));
        assertFalse(service.approveReview("done"));
        assertFalse(service.rejectReview("done"));
    }

    @Test
    void changingUnknownReviewThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.approveReview("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.rejectReview("missing"));
    }

    @Test
    void pendingReviewListContainsOnlyPendingReviews() {
        reviews.save(review("p", "r1", 3, ReviewStatus.PENDING));
        reviews.save(review("a", "r1", 4, ReviewStatus.APPROVED));

        assertEquals(List.of("p"), service.getPendingReviews().stream().map(Review::getId).toList());
    }

    private Review review(String id, String restaurantId, int rating, ReviewStatus status) {
        return new Review(id, restaurantId, 10, "user", rating, "Review", status, 1L);
    }

    private static class MemoryReviews implements ReviewRepository {
        private final List<Review> data = new ArrayList<>();
        @Override public List<Review> findAll() { return data.stream().map(Review::copy).toList(); }
        @Override public Review findById(String id) {
            return data.stream().filter(r -> r.getId().equals(id)).findFirst().map(Review::copy).orElse(null);
        }
        @Override public void save(Review review) {
            if (findById(review.getId()) != null) throw new IllegalArgumentException("duplicate");
            data.add(review.copy());
        }
        @Override public void update(Review review) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).getId().equals(review.getId())) {
                    data.set(i, review.copy());
                    return;
                }
            }
            throw new IllegalArgumentException("not found");
        }
    }

    private static class MemoryRestaurants implements RestaurantRepository {
        private final List<Restaurant> data = new ArrayList<>();
        @Override public List<Restaurant> findAll() { return new ArrayList<>(data); }
        @Override public List<Restaurant> findByFilter(Filter filter) {
            return data.stream().filter(filter::matches).toList();
        }
        @Override public Restaurant findById(String id) {
            return data.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
        }
        @Override public List<String> getAllCuisines() { return List.of(); }
        @Override public List<String> getAllDishes() { return List.of(); }
        @Override public void save(Restaurant restaurant) { data.add(restaurant); }
    }
}
