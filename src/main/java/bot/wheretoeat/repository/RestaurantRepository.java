package bot.wheretoeat.repository;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;

import java.util.List;

public interface RestaurantRepository {

    // TODO(refactor P2): Добавить findById и использовать в ReviewService/TelegramReviewHandler
    // вместо повторяющихся findAll().stream(); существующие интерфейсы хранилищ сохранить.
    List<Restaurant> findAll();

    List<Restaurant> findByFilter(Filter filter);

    List<String> getAllCuisines();

    List<String> getAllDishes();

    void save(Restaurant restaurant);
}