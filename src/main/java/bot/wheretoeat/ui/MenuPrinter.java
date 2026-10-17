package bot.wheretoeat.ui;

import java.util.List;

public class MenuPrinter {

    public void printWelcome() {
        System.out.println("Бот 'Где поесть?' запущен!");
        System.out.println("Для помощи введите /help");
        System.out.println("Для закрытия программы введите /exit");
    }

    public void printMainMenu() {
        System.out.println();
        System.out.println("Выберите 1-3");
        System.out.println("1. 🍕 Фильтры: тип еды, кухня, цена");
        System.out.println("2. ⭐ Рейтинги и отзывы");
        System.out.println("3. 📜 История посещений");
        System.out.println("/exit — выход из программы");
    }

    public void printFilterOptions() {
        System.out.println();
        System.out.println("🔍 Фильтры:");
        System.out.println("1. Тип еды (блюда)");
        System.out.println("2. Кухня");
        System.out.println("3. Средний чек");
        System.out.println("4. Сбросить все фильтры");
        System.out.println("0. Назад в главное меню");
    }

    public void printCurrentFilters(String dish, String cuisine, int minCheck, int maxCheck) {
        List<String> parts = new java.util.ArrayList<>();
        if (dish != null) {
            parts.add("🍽 блюдо: " + dish);
        }
        if (cuisine != null) {
            parts.add("🌍 кухня: " + cuisine);
        }
        if (minCheck > 0 && maxCheck > 0) {
            parts.add("💰 чек: " + minCheck + "-" + maxCheck + " ₽");
        } else if (maxCheck > 0) {
            parts.add("💰 чек: до " + maxCheck + " ₽");
        } else if (minCheck > 0) {
            parts.add("💰 чек: от " + minCheck + " ₽");
        }

        if (parts.isEmpty()) {
            System.out.println("Фильтры пока не заданы.");
        } else {
            System.out.println("Текущие фильтры: " + String.join(", ", parts));
        }
    }

    public void printResultsHeader() {
        System.out.println();
        System.out.println("══════ Результаты ══════");
    }

    public void printNoResults() {
        System.out.println("😕 Ничего не найдено. Ослабьте условия (пункт 4 — сброс).");
    }

    public void printResults(List<?> restaurants) {
        System.out.println("Найдено заведений: " + restaurants.size());
        for (Object r : restaurants) {
            System.out.println(r);
            System.out.println("---------------------------");
        }
    }

    public void printResetDone() {
        System.out.println("🧹 Все фильтры сброшены.");
    }

    public void printPrompt(String text) {
        System.out.print(text);
    }

    public void printFilterMenu(List<String> values, String title) {
        System.out.println(title + ":");
        for (int i = 0; i < values.size(); i++) {
            System.out.println((i + 1) + ". " + values.get(i));
        }
        System.out.println("0. Назад");
    }

    public void printPriceMenu() {
        System.out.println("💰 Средний чек:");
        System.out.println("1. до 500 ₽");
        System.out.println("2. 500–1000 ₽");
        System.out.println("3. 1000–1500 ₽");
        System.out.println("4. свыше 1500 ₽");
        System.out.println("5. свой диапазон (введите числа, например 600-1200)");
        System.out.println("0. Назад");
    }

    public void printError(String message) {
        System.out.println("❌ " + message);
    }
}