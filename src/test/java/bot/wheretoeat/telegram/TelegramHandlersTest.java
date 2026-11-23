package bot.wheretoeat.telegram;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.model.Review;
import bot.wheretoeat.model.ReviewStatus;
import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.ReviewRepository;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TelegramHandlersTest {
    private static final long ADMIN_ID = 100L;
    private static final long USER_ID = 200L;
    private static final long CHAT_ID = 300L;

    private MemoryRestaurants restaurantRepo;
    private MemoryReviews reviewRepo;
    private RestaurantService restaurantService;
    private ReviewService reviewService;
    private TelegramCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        restaurantRepo = new MemoryRestaurants();
        restaurantRepo.save(new Restaurant("r1", "Tokyo", "Street 1", "Japanese", List.of("Ramen", "Sushi"), 1200));
        restaurantRepo.save(new Restaurant("r2", "Roma", "Street 2", "Italian", List.of("Pizza"), 800));

        reviewRepo = new MemoryReviews();
        restaurantService = new RestaurantService(restaurantRepo);
        reviewService = new ReviewService(reviewRepo, restaurantRepo);

        TelegramHelpHandler helpHandler = new TelegramHelpHandler();
        TelegramRestaurantHandler restaurantHandler = new TelegramRestaurantHandler(restaurantService);
        TelegramFilterHandler filterHandler = new TelegramFilterHandler(restaurantService, restaurantHandler);
        TelegramReviewHandler reviewHandler = new TelegramReviewHandler(reviewService, restaurantService);
        TelegramAdminHandler adminHandler = new TelegramAdminHandler(reviewService, restaurantService, Set.of(ADMIN_ID));
        commandHandler = new TelegramCommandHandler(helpHandler, restaurantHandler, filterHandler, reviewHandler, adminHandler, "Where_ToEat_bot");
    }

    @Test
    void ignoresCommandsAddressedToAnotherBot() {
        BotResponse response = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/help@AnotherBot");
        assertTrue(response.getMessages().isEmpty());
        assertNull(response.getCallbackAnswer());
    }

    @Test
    void handlesBasicHelpStartAllAndUnknownCommands() {
        String startText = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/start").getMessages().getFirst().text();
        assertTrue(startText.contains("Добро пожаловать"));

        String helpText = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/help").getMessages().getFirst().text();
        assertTrue(helpText.contains("/filters"));

        String allText = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/all").getMessages().getFirst().text();
        assertTrue(allText.contains("Tokyo"));
        assertTrue(allText.contains("Roma"));

        String unknownText = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/abracadabra").getMessages().getFirst().text();
        assertTrue(unknownText.contains("Неизвестная команда"));
    }

    @Test
    void filterAndSearchFlowFindsRestaurantsAndAttachesReviewKeyboard() {
        BotResponse cuisinePrompt = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/cuisine");
        assertTrue(cuisinePrompt.getMessages().getFirst().text().contains("1 — Italian"));

        BotResponse cuisineSelected = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "2");
        assertTrue(cuisineSelected.getMessages().getFirst().text().contains("Japanese"));

        commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/price");
        BotResponse badPrice = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "1500-1000");
        assertTrue(badPrice.getMessages().getFirst().text().contains("Минимальная цена"));

        BotResponse validPrice = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "1000-1500");
        assertTrue(validPrice.getMessages().getFirst().text().contains("сохранён"));

        BotResponse searchResponse = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/search");
        assertEquals(2, searchResponse.getMessages().size());
        assertTrue(searchResponse.getMessages().get(0).text().contains("Найдено заведений: 1"));
        assertTrue(searchResponse.getMessages().get(1).text().contains("Tokyo"));
        assertNotNull(searchResponse.getMessages().get(1).keyboard());

        BotResponse resetResponse = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/reset");
        assertTrue(resetResponse.getMessages().getFirst().text().contains("сброшены"));
    }

    @Test
    void reviewWorkflowViaCallbackAndModerationByAdmin() {
        BotResponse prematureCallback = commandHandler.routeCallback(CHAT_ID, USER_ID, "restaurant_review:r1");
        assertTrue(prematureCallback.getCallbackAnswer().contains("/search"));

        commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/search");

        BotResponse reviewCallback = commandHandler.routeCallback(CHAT_ID, USER_ID, "restaurant_review:r1");
        assertEquals("Открываем написание отзыва.", reviewCallback.getCallbackAnswer());
        assertTrue(reviewCallback.getMessages().getFirst().text().contains("Tokyo"));

        BotResponse blockedCmd = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/all");
        assertTrue(blockedCmd.getMessages().getFirst().text().contains("/cancel"));

        BotResponse invalidRating = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "10");
        assertTrue(invalidRating.getMessages().getFirst().text().contains("от 1 до 5"));

        BotResponse validRating = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "5");
        assertTrue(validRating.getMessages().getFirst().text().contains("текст отзыва"));

        BotResponse submitted = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "Отличный рамен!");
        assertTrue(submitted.getMessages().getFirst().text().contains("модерацию"));
        assertEquals(1, reviewService.getPendingReviews().size());

        BotResponse denied = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/pending");
        assertTrue(denied.getMessages().getFirst().text().contains("нет доступа"));

        BotResponse pendingList = commandHandler.routeMessage(CHAT_ID, ADMIN_ID, "admin", "/pending");
        assertEquals(1, pendingList.getMessages().size());
        assertNotNull(pendingList.getMessages().getFirst().keyboard());

        String reviewId = reviewService.getPendingReviews().getFirst().getId();
        BotResponse approveCallback = commandHandler.routeCallback(CHAT_ID, ADMIN_ID, "approve:" + reviewId);
        assertTrue(approveCallback.getCallbackAnswer().contains("одобрен"));

        BotResponse reviewsList = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/reviews r1");
        String reviewsText = reviewsList.getMessages().getFirst().text();
        assertTrue(reviewsText.contains("5.0 из 5"));
        assertTrue(reviewsText.contains("Отличный рамен!"));
    }

    @Test
    void adminCanAddRestaurantAndCancelStopsActiveStep() {
        BotResponse added = commandHandler.routeMessage(CHAT_ID, ADMIN_ID, "admin", "/add_restaurant Tbilisi | Lenina 10 | Georgian | Khinkali, Khachapuri | 1100");
        assertTrue(added.getMessages().getFirst().text().contains("Ресторан добавлен"));
        assertEquals(3, restaurantService.findAll().size());

        commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/write_review r1");
        BotResponse cancelled = commandHandler.routeMessage(CHAT_ID, USER_ID, "user", "/cancel");
        assertTrue(cancelled.getMessages().getFirst().text().contains("отменено"));
    }

    private static class MemoryReviews implements ReviewRepository {
        private final List<Review> data = new ArrayList<>();

        @Override
        public List<Review> findAll() {
            return data.stream().map(Review::copy).toList();
        }

        @Override
        public Review findById(String id) {
            return data.stream().filter(r -> r.getId().equals(id)).findFirst().map(Review::copy).orElse(null);
        }

        @Override
        public void save(Review review) {
            data.add(review.copy());
        }

        @Override
        public void update(Review review) {
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

        @Override
        public List<Restaurant> findAll() {
            return new ArrayList<>(data);
        }

        @Override
        public List<Restaurant> findByFilter(Filter filter) {
            return data.stream().filter(filter::matches).toList();
        }

        @Override
        public Restaurant findById(String id) {
            return data.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
        }

        @Override
        public List<String> getAllCuisines() {
            return data.stream().map(Restaurant::getCuisine).distinct().sorted().toList();
        }

        @Override
        public List<String> getAllDishes() {
            return data.stream().flatMap(r -> r.getDishes().stream()).distinct().sorted().toList();
        }

        @Override
        public void save(Restaurant restaurant) {
            data.add(restaurant);
        }
    }
}
