package bot.wheretoeat.repository;

import bot.wheretoeat.model.Review;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReviewRepositoryImpl implements ReviewRepository {

    private final List<Review> reviews;
    private final ObjectMapper mapper;
    private final File file;

    public ReviewRepositoryImpl() {
        mapper = new ObjectMapper();

        File dataDirectory = new File("data");

        if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
            throw new RuntimeException("Не удалось создать папку data");
        }

        file = new File(dataDirectory, "reviews.json");

        reviews = loadReviews();
    }

    @Override
    public List<Review> findAll() {
        return new ArrayList<>(reviews);
    }

    @Override
    public Review findById(String id) {
        return reviews.stream()
                .filter(review -> review.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void save(Review review) {
        if (review == null) {
            throw new IllegalArgumentException("Отзыв не может быть null");
        }

        if (findById(review.getId()) != null) {
            throw new IllegalArgumentException("Такой отзыв уже существует");
        }

        reviews.add(review);
        saveReviews();
    }

    @Override
    public void update(Review review) {
        if (review == null) {
            throw new IllegalArgumentException("Отзыв не может быть null");
        }

        for (int i = 0; i < reviews.size(); i++) {
            if (reviews.get(i).getId().equals(review.getId())) {
                reviews.set(i, review);
                saveReviews();
                return;
            }
        }

        throw new IllegalArgumentException("Отзыв не найден");
    }

    private List<Review> loadReviews() {
        try {
            if (!file.exists()) {
                saveEmptyFile();
                return new ArrayList<>();
            }

            return mapper.readValue(
                    file,
                    new TypeReference<List<Review>>() {}
            );

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке отзывов", e);
        }
    }

    private void saveEmptyFile() {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, new ArrayList<Review>());
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать reviews.json", e);
        }
    }

    private void saveReviews() {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, reviews);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении отзывов", e);
        }
    }
}