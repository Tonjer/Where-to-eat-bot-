package bot.wheretoeat.repository;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class RestaurantRepositoryImpl implements RestaurantRepository { // TODO давайте нормально называть реализации?
    // Impl - моветон, он ни о чём не говорит. Почему бы не назвать InMemoryRestaurantRepository? Звучит приятнее и понятно,
    // что это репозиторий в памяти, а не БД, можно так же InFileRestarauntRepositry и в таком духе названия (мб и лучше, у вас ведь тут из файла загрузка).
    // Аналогично с ReviewRepository.

    private final List<Restaurant> restaurants;

    public RestaurantRepositoryImpl() {
        restaurants = loadRestaurants();
    } // TODO отделять методы строкой, либо нажимать почаще Ctrl + Alt + L для автоформатирования
    @Override
    public List<Restaurant> findAll() {
        return new ArrayList<>(restaurants);
    }

    @Override
    public List<Restaurant> findByFilter(Filter filter) {
        return restaurants.stream().filter(filter::matches).collect(Collectors.toList()); // TODO после stream() принято переносить всё на новую строку:
        //типа restaraunts.stream()
        //  .filter(...)
        //  .collect(...);
        // это добавляет читаемости
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

    // TODO(refactor P2): После чтения проверять обязательные поля, уникальность ID, dishes и неотрицательный чек;
    // выдавать понятную ошибку загрузки, а не NPE при первом поиске.
    private List<Restaurant> loadRestaurants() {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("restaurant.json")) { // TODO Лучше вынести это в env файл, чтобы можно было разные источники использовать

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