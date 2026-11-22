package bot.wheretoeat.service.commands;

import bot.wheretoeat.filter.CuisineFilter;
import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RestaurantCommandsTest {
    private RestaurantService service;

    @BeforeEach
    void setUp() {
        InMemoryRestaurantRepository repository = new InMemoryRestaurantRepository();
        repository.save(new Restaurant("1", "Tokyo", "Street 1", "Japanese", List.of("Ramen", "Sushi"), 1200));
        repository.save(new Restaurant("2", "Roma", "Street 2", "Italian", List.of("Pizza", "Pasta"), 1800));
        service = new RestaurantService(repository);
    }

    @Test
    void findAllRestaurantsCommandReturnsAllRestaurants() {
        Command<List<Restaurant>> command = new FindAllRestaurantsCommand(service);
        List<Restaurant> result = command.execute();

        assertEquals(2, result.size());
        assertEquals(List.of("Tokyo", "Roma"), result.stream().map(Restaurant::getName).toList());
    }

    @Test
    void findRestaurantsByFilterCommandAppliesGivenFilter() {
        Command<List<Restaurant>> command = new FindRestaurantsByFilterCommand(service, new CuisineFilter("Italian"));
        List<Restaurant> result = command.execute();

        assertEquals(1, result.size());
        assertEquals("Roma", result.getFirst().getName());
    }

    @Test
    void getAllCuisinesCommandReturnsSortedDistinctCuisines() {
        Command<List<String>> command = new GetAllCuisinesCommand(service);
        assertEquals(List.of("Italian", "Japanese"), command.execute());
    }

    @Test
    void getAllDishesCommandReturnsAllAvailableDishes() {
        Command<List<String>> command = new GetAllDishesCommand(service);
        assertEquals(List.of("Pasta", "Pizza", "Ramen", "Sushi"), command.execute());
    }

    private static class InMemoryRestaurantRepository implements RestaurantRepository {
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
