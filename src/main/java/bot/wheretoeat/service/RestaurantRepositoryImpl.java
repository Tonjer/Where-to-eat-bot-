package bot.wheretoeat.service;

import bot.wheretoeat.model.Restaurant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class RestaurantRepositoryImpl implements RestaurantRepository {

    private final List<Restaurant> restaurants;

    public RestaurantRepositoryImpl() {
        restaurants = loadRestaurants();
    }
    @Override
    public List<Restaurant> findAll() {
        return new ArrayList<>(restaurants);
    }

    @Override
    public List<Restaurant> findByFilter(Filter filter) {
        return restaurants.stream().filter(filter::matches).collect(Collectors.toList());
    }

    @Override
    public List<String> getAllCuisines() {
        TreeSet<String> cuisines = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Restaurant restaurant : restaurants) {
            if (restaurant.getCuisine() != null) {
                cuisines.add(restaurant.getCuisine());
            }
        }
        return new ArrayList<>(cuisines);
    }
    @Override
    public List<String> getAllDishes() {
        TreeSet<String> dishes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Restaurant restaurant : restaurants) {
            if (restaurant.getDishes() != null) {
                dishes.addAll(restaurant.getDishes());
            }
        }
        return new ArrayList<>(dishes);
    }

    private List<Restaurant> loadRestaurants() {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("restaurant.json")) {

            if (inputStream == null) {
                throw new RuntimeException("Файл restaurant.json не найден!");
            }

            return mapper.readValue(inputStream, new TypeReference<List<Restaurant>>() {
                    }
            );

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке базы ресторанов", e);
        }
    }
}