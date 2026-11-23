package bot.wheretoeat.service;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RestaurantServiceTest {
    private InMemoryRestaurantRepository repository;
    private RestaurantService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRestaurantRepository();
        repository.save(new Restaurant("1", "Tokyo", "Street 1", "Japanese", List.of("Ramen", "Sushi"), 1200));
        repository.save(new Restaurant("2", "Roma", "Street 2", "Italian", List.of("Pizza", "Pasta"), 1800));
        service = new RestaurantService(repository);
    }

    @Test
    void searchCombinesCuisineDishAndPriceFilters() {
        List<Restaurant> result = service.search("japan", "ram", 1000, 1500);

        assertEquals(1, result.size());
        assertEquals("Tokyo", result.getFirst().getName());
    }

    @Test
    void searchWithEmptyFiltersReturnsAllRestaurants() {
        assertEquals(2, service.search(null, null, 0, 0).size());
    }

    @Test
    void addRestaurantTrimsInputAndPersistsRestaurant() {
        Restaurant added = service.addRestaurant("  New place  ", "  New address ", "  Georgian ", List.of("  Khinkali ", "Khachapuri"), 900);

        assertEquals("New place", added.getName());
        assertEquals("New address", added.getAddress());
        assertEquals("Georgian", added.getCuisine());
        assertEquals(List.of("Khinkali", "Khachapuri"), added.getDishes());
        assertEquals(3, service.findAll().size());
        assertTrue(added.getId().startsWith("restaurant_"));
    }

    @Test
    void addRestaurantRemovesBlankDishes() {
        Restaurant added = service.addRestaurant("Place", "Address", "Cuisine", List.of(" ", " Dish "), 500);

        assertEquals(List.of("Dish"), added.getDishes());
    }

    @Test
    void addRestaurantRejectsMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> service.addRestaurant(" ", "Address", "Cuisine", List.of("Dish"), 500));
        assertThrows(IllegalArgumentException.class, () -> service.addRestaurant("Name", null, "Cuisine", List.of("Dish"), 500));
        assertThrows(IllegalArgumentException.class, () -> service.addRestaurant("Name", "Address", "", List.of("Dish"), 500));
    }

    @Test
    void addRestaurantRejectsInvalidPriceAndDishes() {
        assertThrows(IllegalArgumentException.class, () -> service.addRestaurant("Name", "Address", "Cuisine", List.of("Dish"), 0));
        assertThrows(IllegalArgumentException.class, () -> service.addRestaurant("Name", "Address", "Cuisine", List.of(), 500));
        assertThrows(IllegalArgumentException.class, () -> service.addRestaurant("Name", "Address", "Cuisine", java.util.Arrays.asList(" ", null), 500));
    }

    @Test
    void returnsAvailableCuisinesAndDishes() {
        assertEquals(List.of("Italian", "Japanese"), service.getAllCuisines());
        assertTrue(service.getAllDishes().containsAll(List.of("Ramen", "Sushi", "Pizza", "Pasta")));
    }

    private static class InMemoryRestaurantRepository implements RestaurantRepository {
        private final List<Restaurant> data = new ArrayList<>();

        @Override public List<Restaurant> findAll() { return new ArrayList<>(data); }
        @Override public List<Restaurant> findByFilter(Filter filter) {
            return data.stream().filter(filter::matches).toList();
        }
        @Override public Restaurant findById(String id) {
            return data.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
        }
        @Override public List<String> getAllCuisines() {
            return data.stream().map(Restaurant::getCuisine).distinct().sorted().toList();
        }
        @Override public List<String> getAllDishes() {
            return data.stream().flatMap(r -> r.getDishes().stream()).distinct().sorted().toList();
        }
        @Override public void save(Restaurant restaurant) { data.add(restaurant); }
    }
}