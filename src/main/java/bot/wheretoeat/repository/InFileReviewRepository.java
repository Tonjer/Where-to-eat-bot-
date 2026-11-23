package bot.wheretoeat.repository;

import bot.wheretoeat.model.Review;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public synchronized List<Review> findAll() {
        return reviews.stream()
                .map(Review::copy)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Review findById(String id) {
        if (id == null) {
            return null;
        }
        return reviews.stream()
                .filter(review -> review.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public synchronized void save(Review review) {
        if (review == null) {
            throw new IllegalArgumentException("Отзыв не может быть null");
        }

        if (findById(review.getId()) != null) {
            throw new IllegalArgumentException("Такой отзыв уже существует");
        }

        List<Review> nextSnapshot = new ArrayList<>(reviews.size() + 1);
        for (Review existing : reviews) {
            nextSnapshot.add(existing.copy());
        }
        nextSnapshot.add(review.copy());

        writeSnapshotAtomically(nextSnapshot);

        reviews.clear();
        reviews.addAll(nextSnapshot);
    }

    @Override
    public synchronized void update(Review review) {
        if (review == null) {
            throw new IllegalArgumentException("Отзыв не может быть null");
        }

        int index = -1;
        for (int i = 0; i < reviews.size(); i++) {
            if (reviews.get(i).getId().equals(review.getId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("Отзыв не найден");
        }

        List<Review> nextSnapshot = new ArrayList<>(reviews.size());
        for (int i = 0; i < reviews.size(); i++) {
            if (i == index) {
                nextSnapshot.add(review.copy());
            } else {
                nextSnapshot.add(reviews.get(i).copy());
            }
        }
        writeSnapshotAtomically(nextSnapshot);

        reviews.clear();
        reviews.addAll(nextSnapshot);
    }

    private List<Review> loadReviews() {
        try {
            if (!file.exists()) {
                writeSnapshotAtomically(new ArrayList<>());
                return new ArrayList<>();
            }
            List<Review> loaded = mapper.readValue(file, new TypeReference<List<Review>>() {
            });
            if (loaded == null) {
                throw new RuntimeException("Повреждён файл отзывов: содержимое равно null");
            }
            return loaded.stream().map(Review::copy).collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке отзывов (повреждённый файл не перезаписан)", e);
        }
    }

    private void writeSnapshotAtomically(List<Review> snapshot) {
        Path targetPath = file.toPath();
        Path parentDir = targetPath.getParent() != null ? targetPath.getParent() : Path.of(".");
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(parentDir, file.getName() + ".", ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), snapshot);
            try {
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
            throw new RuntimeException("Ошибка при сохранении отзывов", e);
        }
    }
}