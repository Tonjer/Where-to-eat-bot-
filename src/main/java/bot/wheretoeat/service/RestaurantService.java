
package bot.wheretoeat.service;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.repository.RestaurantRepository;

import java.util.List;

public class RestaurantService { // TODO у вас этот класс все действия делегирует репозиторию. Он полностью повторяет методы
    // репозитория. Если так происходит, значит, вы создали дубликат и бесполезный класс.
    // В идеале у вас в сервисе должна быть логика

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

    public List<String> getAllCuisines() {
        return repository.getAllCuisines();
    }

    public List<String> getAllDishes() {
        return repository.getAllDishes();
    }
}
