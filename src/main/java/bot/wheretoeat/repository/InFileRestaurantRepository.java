package bot.wheretoeat.repository;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class InFileRestaurantRepository implements RestaurantRepository {
    private static final String DEFAULT_RESOURCE = "restaurant.json";
    private static final String ENV_RESTAURANTS_FILE = "RESTAURANTS_FILE";
    private static final String DEFAULT_FILE_PATH = "data/restaurants.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private final File file;
    private final List<Restaurant> restaurants;

    public InFileRestaurantRepository() {
        String envPath = System.getenv(ENV_RESTAURANTS_FILE);
        String targetPath = (envPath != null && !envPath.isBlank()) ? envPath.trim() : DEFAULT_FILE_PATH;
        this.file = new File(targetPath);
        this.restaurants = new ArrayList<>(loadRestaurants());
    }

    @Override
    public synchronized List<Restaurant> findAll() {
        return new ArrayList<>(restaurants);
    }

    @Override
    public List<Restaurant> findByFilter(Filter filter) {
        return restaurants.stream()
                .filter(filter::matches)
                .collect(Collectors.toList());
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

    @Override
    public synchronized void save(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Ресторан не может быть null");
        }
        restaurants.add(restaurant);
        saveRestaurantsToFile();
    }

    // TODO(refactor P2): После чтения проверять обязательные поля, уникальность ID, dishes и неотрицательный чек;
    // выдавать понятную ошибку загрузки, а не NPE при первом поиске.
    private List<Restaurant> loadRestaurants() {
        try (InputStream inputStream = openRestaurantsStream()) {
            if (inputStream == null) {
                throw new RuntimeException("Источник базы ресторанов не найден!");
            }
            return mapper.readValue(inputStream, new TypeReference<List<Restaurant>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке базы ресторанов", e);
        }
    }

    private InputStream openRestaurantsStream() throws IOException {
        if (file.exists() && file.isFile()) {
            return new FileInputStream(file);
        }
        String envSource = System.getenv(ENV_RESTAURANTS_FILE);
        if (envSource != null && !envSource.isBlank()) {
            InputStream envResource = getClass().getClassLoader().getResourceAsStream(envSource.trim());
            if (envResource != null) {
                return envResource;
            }
            throw new RuntimeException("Файл или ресурс из " + ENV_RESTAURANTS_FILE + " не найден: " + envSource);
        }
        return getClass().getClassLoader().getResourceAsStream(DEFAULT_RESOURCE);
    }

    private void saveRestaurantsToFile() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()){
                throw new RuntimeException("Не удалось создать директорию " + parent.getPath());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, restaurants);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении базы ресторанов", e);
        }
    }
}