package bot.wheretoeat.model;

public class Review {

    private String id;
    private String restaurantId;
    private long telegramUserId;
    private String username;
    private int rating;
    private String text;
    private ReviewStatus status;
    private long createdAt;

    public Review() {
    }

    public Review(String id, String restaurantId,
                  long telegramUserId, String username,
                  int rating, String text,
                  ReviewStatus status, long createdAt) {

        this.id = id;
        this.restaurantId = restaurantId;
        this.telegramUserId = telegramUserId;
        this.username = username;
        this.rating = rating;
        this.text = text;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Review copy() {
        return new Review(
                this.id,
                this.restaurantId,
                this.telegramUserId,
                this.username,
                this.rating,
                this.text,
                this.status,
                this.createdAt
        );
    }

    public Review copyWithStatus(ReviewStatus newStatus) {
        return new Review(
                this.id,
                this.restaurantId,
                this.telegramUserId,
                this.username,
                this.rating,
                this.text,
                newStatus,
                this.createdAt
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public long getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
