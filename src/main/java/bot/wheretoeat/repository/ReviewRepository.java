
package bot.wheretoeat.repository;

import bot.wheretoeat.model.Review;

import java.util.List;

public interface ReviewRepository {

    List<Review> findAll();

    Review findById(String id);

    void save(Review review);

    void update(Review review);
}
