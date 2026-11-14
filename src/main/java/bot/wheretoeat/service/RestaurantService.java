package bot.wheretoeat.service;

import bot.wheretoeat.filter.CompositeFilter;
import bot.wheretoeat.filter.CuisineFilter;
import bot.wheretoeat.filter.DishFilter;
import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.filter.PriceFilter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.util.StringUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RestaurantService {

    // TODO(refactor P2): Сохранить конкретный сервис как точку поиска; общие правила не переносить в Telegram-handler.
    // Не добавлять интерфейс сервиса, базовые классы и новые обёртки ради формального разделения слоёв.
    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    public List<Restaurant> findAll() {
        return repository.findAll();
    }

    public List<Restaurant> findByFilter(Filter filter) {
        return repository.findByFilter(filter);
    }

    public List<Restaurant> search(String cuisine, String dish, int minCheck, int maxCheck) {
        Filter filter = new CompositeFilter(
                new DishFilter(dish),
                new CuisineFilter(cuisine),
                new PriceFilter(minCheck, maxCheck)
        );
        return repository.findByFilter(filter);
    }

    public Restaurant addRestaurant(String name, String address, String cuisine, List<String> dishes, int averageCheck) {
        String cleanName = StringUtilities.trimToNull(name);
        String cleanAddress = StringUtilities.trimToNull(address);
        String cleanCuisine = StringUtilities.trimToNull(cuisine);

        if (cleanName == null || cleanAddress == null || cleanCuisine == null) {
            throw new IllegalArgumentException("Название, адрес и кухня обязательны для заполнения.");
        }
        if (averageCheck <= 0) {
            throw new IllegalArgumentException("Средний чек должен быть больше 0.");
        }
        if (dishes == null || dishes.isEmpty()) {
            throw new IllegalArgumentException("Необходимо указать хотя бы одно блюдо.");
        }

        List<String> cleanDishes = new ArrayList<>();
        for (String dish : dishes) {
            String trimmed = StringUtilities.trimToNull(dish);
            if (trimmed != null) {
                cleanDishes.add(trimmed);
            }
        }
        if (cleanDishes.isEmpty()) {
            throw new IllegalArgumentException("Список блюд не может быть пустым.");
        }
        String id = "restaurant_" + UUID.randomUUID().toString().substring(0, 8);
        Restaurant restaurant = new Restaurant(id, cleanName, cleanAddress, cleanCuisine, cleanDishes, averageCheck);
        repository.save(restaurant);
        return restaurant;
    }

    public List<String> getAllCuisines() {
        return repository.getAllCuisines();
    }

    public List<String> getAllDishes() {
        return repository.getAllDishes();
    }
}
