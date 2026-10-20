package bot.wheretoeat.service;

import bot.wheretoeat.model.Restaurant;

import java.util.List;

public interface RestaurantRepository {

    List<Restaurant> findAll();

    List<Restaurant> findByFilter(Filter filter);

    List<String> getAllCuisines();

    List<String> getAllDishes();
}