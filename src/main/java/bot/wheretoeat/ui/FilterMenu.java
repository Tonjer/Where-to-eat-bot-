
package bot.wheretoeat.ui;

import bot.wheretoeat.filter.CompositeFilter;
import bot.wheretoeat.filter.CuisineFilter;
import bot.wheretoeat.filter.DishFilter;
import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.filter.PriceFilter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantService;

import java.util.List;

public class FilterMenu {

    private final RestaurantService service;
    private final MenuPrinter printer;

    private String dish = null;
    private String cuisine = null;
    private int minCheck = 0;
    private int maxCheck = 0;

    public FilterMenu(
            RestaurantService service,
            MenuPrinter printer
    ) {
        this.service = service;
        this.printer = printer;
    }

    public void show() {
        printer.printFilterOptions();
    }

    public void handleInput(String input) {
        switch (input) {
            case "1":
                chooseDish();
                break;
            case "2":
                chooseCuisine();
                break;
            case "3":
                choosePrice();
                break;
            case "4":
                reset();
                show();
                break;
            default:
                printer.printError("Введите 1, 2, 3, 4 или 0.");
        }
    }

    private void chooseDish() {
        List<String> dishes = service.getAllDishes();
        int choice = chooseFromList(dishes, "🍽 Выберите тип еды");

        if (choice > 0) {
            dish = dishes.get(choice - 1);
            showResults();
        }
    }

    private void chooseCuisine() {
        List<String> cuisines = service.getAllCuisines();
        int choice = chooseFromList(cuisines, "🌍 Выберите кухню");

        if (choice > 0) {
            cuisine = cuisines.get(choice - 1);
            showResults();
        }
    }

    private void choosePrice() {
        printer.printPriceMenu();

        String input = ConsoleReader.readLine();

        if (input.equals("5")) {
            chooseCustomPrice();
            return;
        }

        switch (input) {
            case "1":
                minCheck = 0;
                maxCheck = 500;
                showResults();
                break;
            case "2":
                minCheck = 500;
                maxCheck = 1000;
                showResults();
                break;
            case "3":
                minCheck = 1000;
                maxCheck = 1500;
                showResults();
                break;
            case "4":
                minCheck = 1500;
                maxCheck = 0;
                showResults();
                break;
            case "0":
                show();
                break;
            default:
                printer.printError("Введите 1-5 или 0.");
        }
    }

    private void chooseCustomPrice() {
        printer.printPrompt(
                "Введите диапазон (например 600-1200, 'до 900' или 'от 1000'): "
        );

        String input = ConsoleReader.readLine()
                .toLowerCase()
                .replace("₽", "");

        try {
            if (input.startsWith("до")) {
                minCheck = 0;
                maxCheck = Integer.parseInt(input.substring(2).trim());

            } else if (input.startsWith("от")) {
                minCheck = Integer.parseInt(input.substring(2).trim());
                maxCheck = 0;

            } else {
                String[] parts = input.split("-");

                if (parts.length == 2) {
                    minCheck = Integer.parseInt(parts[0].trim());
                    maxCheck = Integer.parseInt(parts[1].trim());
                } else {
                    printer.printError(
                            "Не понял формат. Примеры: 600-1200, до 900, от 1000."
                    );
                    return;
                }
            }

            if (minCheck < 0 || maxCheck < 0) {
                printer.printError("Цена не может быть отрицательной.");
                return;
            }

            if (minCheck > 0 && maxCheck > 0 && minCheck > maxCheck) {
                printer.printError("Минимальная цена больше максимальной.");
                return;
            }

            showResults();

        } catch (NumberFormatException e) {
            printer.printError(
                    "Нужно ввести числа. Примеры: 600-1200, до 900, от 1000."
            );
        }
    }

    private int chooseFromList(List<String> values, String title) {
        printer.printFilterMenu(values, title);

        String input = ConsoleReader.readLine();

        try {
            int choice = Integer.parseInt(input);

            if (choice == 0) {
                show();
                return 0;
            }

            if (choice < 1 || choice > values.size()) {
                printer.printError(
                        "Число вне диапазона. Попробуйте ещё раз."
                );
                return 0;
            }

            return choice;

        } catch (NumberFormatException e) {
            printer.printError("Нужно ввести число.");
            return 0;
        }
    }

    private void showResults() {
        Filter filter = new CompositeFilter(
                new DishFilter(dish),
                new CuisineFilter(cuisine),
                new PriceFilter(minCheck, maxCheck)
        );

        List<Restaurant> found = service.findByFilter(filter);

        printer.printResultsHeader();
        printCurrentFilters();

        if (found.isEmpty()) {
            printer.printNoResults();
        } else {
            printer.printResults(found);
        }

        show();
    }

    private void printCurrentFilters() {
        printer.printCurrentFilters(
                dish,
                cuisine,
                minCheck,
                maxCheck
        );
    }

    private void reset() {
        dish = null;
        cuisine = null;
        minCheck = 0;
        maxCheck = 0;

        System.out.println("🧹 Все фильтры сброшены.");
    }
}
