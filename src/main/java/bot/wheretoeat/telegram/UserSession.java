package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;

import java.util.List;

public class UserSession {
    public enum Step {
        NONE,
        FILTER_CUISINE,
        FILTER_DISH,
        FILTER_PRICE,
        REVIEW_RATING,
        REVIEW_TEXT
    }

    public record SessionKey(long chatId, long userId) {
    }

    private Step step = Step.NONE;
    private long updatedAt = System.currentTimeMillis();

    private String cuisine;
    private String dish;
    private int minCheck;
    private int maxCheck;

    private List<Restaurant> lastSearchResults = List.of();

    private String reviewRestaurantId;
    private int reviewRating;
    private String reviewDraftText;

    private void touch(long now) {
        this.updatedAt = now;
    }

    public boolean isExpired(long now, long ttlMillis) {
        return now - updatedAt > ttlMillis;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step == null ? Step.NONE : step;
    }

    public boolean isWritingReview() {
        return step == Step.REVIEW_RATING || step == Step.REVIEW_TEXT;
    }

    public void startReview(String restaurantId) {
        this.reviewRestaurantId = restaurantId;
        this.reviewRating = 0;
        this.reviewDraftText = null;
        this.step = Step.REVIEW_RATING;
    }

    public void clearReview() {
        this.reviewRestaurantId = null;
        this.reviewRating = 0;
        this.reviewDraftText = null;
        if (isWritingReview()) {
            this.step = Step.NONE;
        }
    }

    public void cancelCurrentStep() {
        if (isWritingReview()) {
            clearReview();
        } else {
            this.step = Step.NONE;
        }
    }

    public void resetFilters() {
        this.cuisine = null;
        this.dish = null;
        this.minCheck = 0;
        this.maxCheck = 0;
        if (!isWritingReview()) {
            this.step = Step.NONE;
        }
    }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getDish() { return dish; }
    public void setDish(String dish) { this.dish = dish; }
    public int getMinCheck() { return minCheck; }
    public void setMinCheck(int minCheck) { this.minCheck = minCheck; }
    public int getMaxCheck() { return maxCheck; }
    public void setMaxCheck(int maxCheck) { this.maxCheck = maxCheck; }

    public List<Restaurant> getLastSearchResults() { return lastSearchResults; }
    public void setLastSearchResults(List<Restaurant> results) {
        this.lastSearchResults = results == null ? List.of() : List.copyOf(results);
    }

    public String getReviewRestaurantId() { return reviewRestaurantId; }
    public int getReviewRating() { return reviewRating; }
    public void setReviewRating(int reviewRating) { this.reviewRating = reviewRating; }
    public String getReviewDraftText() { return reviewDraftText; }
    public void setReviewDraftText(String reviewDraftText) { this.reviewDraftText = reviewDraftText; }
}
