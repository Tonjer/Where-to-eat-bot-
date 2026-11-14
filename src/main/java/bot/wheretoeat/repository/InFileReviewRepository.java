package bot.wheretoeat.repository;

import bot.wheretoeat.model.Review;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InFileReviewRepository implements ReviewRepository {

    private final List<Review> reviews;
    private final ObjectMapper mapper;
    private final File file;

    // TODO(refactor P2): Принимать путь данных через конструктор (из конфигурации), чтобы файловые
    // проверки использовали временную директорию. Для этого этапа оставить JSON, без обязательного переезда на БД.
    public InFileReviewRepository() {
        mapper = new ObjectMapper();

        File dataDirectory = new File("data");

        if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
            throw new RuntimeException("Не удалось создать папку data");
        }

        file = new File(dataDirectory, "reviews.json"); //

        reviews = loadReviews();
    }

    @Override
    public List<Review> findAll() {
        return new ArrayList<>(reviews);
    }

    @Override
    // TODO(refactor P1): Не отдавать внутренние изменяемые Review через findById/findAll: возвращать копии
    // либо сделать Review неизменяемым. Иначе сервис меняет хранилище в обход успешного update.
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
                    new TypeReference<List<Review>>() {
                    }
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

    // TODO(refactor P1): save/update должны сначала писать новый снимок во временный файл рядом с основным,
    // затем атомарно заменять основной и только после успеха обновлять память. Синхронизировать весь цикл
    // для одного процесса; при отказе оставлять прежние данные, повреждённый JSON не затирать пустым.
    private void saveReviews() {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, reviews);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении отзывов", e);
        }
    }
}