
package bot.wheretoeat.service;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.repository.RestaurantRepository;

import java.util.List;

public class RestaurantService {

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

    public List<String> getAllCuisines() {
        return repository.getAllCuisines();
    }

    public List<String> getAllDishes() {
        return repository.getAllDishes();
    }
}
