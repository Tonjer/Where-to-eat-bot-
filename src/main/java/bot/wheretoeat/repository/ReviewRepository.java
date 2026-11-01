
package bot.wheretoeat.repository;

import bot.wheretoeat.model.Review;

import java.util.List;

public interface ReviewRepository {

    // Получить все отзывы
    List<Review> findAll();

    // Найти отзыв по его ID
    Review findById(String id);

    // Сохранить новый отзыв
    void save(Review review);

    // Обновить существующий отзыв
    void update(Review review);
}
