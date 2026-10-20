
package bot.wheretoeat.telegram;

import bot.wheretoeat.filter.CompositeFilter;
import bot.wheretoeat.filter.CuisineFilter;
import bot.wheretoeat.filter.DishFilter;
import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.filter.PriceFilter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramFilterHandler {

    private final RestaurantService service;
    private final TelegramRestaurantHandler restaurantHandler;

    // Состояние фильтров отдельно для каждого чата.
    private final Map<Long, FilterState> states = new HashMap<>();

    public TelegramFilterHandler(
            RestaurantService service,
            TelegramRestaurantHandler restaurantHandler
    ) {
        this.service = service;
        this.restaurantHandler = restaurantHandler;
    }

    public String handleMessage(long chatId, String text) {
        String input = text == null ? "" : text.trim();

        if (input.isEmpty()) {
            return "Напиши /filters, чтобы открыть меню фильтров.";
        }

        FilterState state = states.computeIfAbsent(
                chatId,
                id -> new FilterState()
        );

        if (input.equalsIgnoreCase("/cancel")) {
            state.pendingAction = "";
            return "Текущий выбор отменён. Напиши /filters.";
        }

        if (input.equalsIgnoreCase("/filters")) {
            state.pendingAction = "";
            return getMenu(state);
        }

        if (input.equalsIgnoreCase("/reset")) {
            state.reset();
            return "🧹 Все фильтры сброшены.\n\n" + getMenu(state);
        }

        if (input.equalsIgnoreCase("/cuisine")) {
            state.pendingAction = "cuisine";
            return getNumberedList(
                    "🌍 Выбери кухню, отправив её номер:",
                    service.getAllCuisines()
            );
        }

        if (input.equalsIgnoreCase("/dish")) {
            state.pendingAction = "dish";
            return getNumberedList(
                    "🍽 Выбери блюдо, отправив его номер:",
                    service.getAllDishes()
            );
        }

        if (input.equalsIgnoreCase("/price")) {
            state.pendingAction = "price";
            return """
                    💰 Укажи диапазон среднего чека.

                    Примеры:
                    до 900
                    от 1000
                    600-1200

                    Чтобы отменить выбор, напиши /cancel.
                    """;
        }

        if (input.equalsIgnoreCase("/search")) {
            state.pendingAction = "";
            return search(state);
        }

        if (state.pendingAction.equals("cuisine")) {
            return selectFromList(
                    state,
                    input,
                    service.getAllCuisines(),
                    "cuisine"
            );
        }

        if (state.pendingAction.equals("dish")) {
            return selectFromList(
                    state,
                    input,
                    service.getAllDishes(),
                    "dish"
            );
        }

        if (state.pendingAction.equals("price")) {
            return setPrice(state, input);
        }

        return """
                Не понял запрос.

                Напиши /filters, чтобы выбрать фильтры,
                или /help, чтобы увидеть все команды.
                """;
    }

    private String getMenu(FilterState state) {
        return """
                🔎 Меню фильтров
                
                Вы можете сгруппировать фильтры, для этого выберите по очереди каждый из доступных фильтров:
                
                (cuisine; dish; price)
                
                Если их не выбирать, то автоматический будет считаться выбор как "Любой"
                
                После этого под Ваш запрос будет подобрано соответствующее заведение питания
                
                /cuisine — выбрать кухню
                /dish — выбрать блюдо
                /price — задать средний чек
                /search — показать результаты
                /reset — сбросить фильтры

                """;
    }

    private String getNumberedList(
            String title,
            List<String> values
    ) {
        if (values == null || values.isEmpty()) {
            return "Список пока пуст.";
        }

        StringBuilder result = new StringBuilder(title).append("\n\n");

        for (int i = 0; i < values.size(); i++) {
            result.append(i + 1)
                    .append(" — ")
                    .append(values.get(i))
                    .append("\n");
        }

        result.append("\n/cancel — отменить выбор");
        return result.toString();
    }

    private String selectFromList(
            FilterState state,
            String input,
            List<String> values,
            String type
    ) {
        try {
            int choice = Integer.parseInt(input);

            if (choice < 1 || choice > values.size()) {
                return "Номер вне диапазона. Попробуй ещё раз.";
            }

            String selected = values.get(choice - 1);

            if (type.equals("cuisine")) {
                state.cuisine = selected;
            } else {
                state.dish = selected;
            }

            state.pendingAction = "";

            return "Выбрано: " + selected
                    + "\n\nТекущие фильтры:\n"
                    + describeFilters(state)
                    + "\n\nНапиши /filters, чтобы продолжить, "
                    + "или /search для поиска.";

        } catch (NumberFormatException e) {
            return "Отправь номер из списка или напиши /cancel.";
        }
    }

    private String setPrice(FilterState state, String input) {
        String value = input.toLowerCase().replace("₽", "").trim();

        try {
            int min = 0;
            int max = 0;

            if (value.startsWith("до")) {
                max = Integer.parseInt(value.substring(2).trim());
            } else if (value.startsWith("от")) {
                min = Integer.parseInt(value.substring(2).trim());
            } else {
                String[] parts = value.split("-");

                if (parts.length != 2) {
                    return "Неверный формат. Примеры: до 900, от 1000, 600-1200.";
                }

                min = Integer.parseInt(parts[0].trim());
                max = Integer.parseInt(parts[1].trim());
            }

            if (min < 0 || max < 0) {
                return "Цена не может быть отрицательной.";
            }

            if (min > 0 && max > 0 && min > max) {
                return "Минимальная цена не может превышать максимальную.";
            }

            state.minCheck = min;
            state.maxCheck = max;
            state.pendingAction = "";

            return "Диапазон среднего чека сохранён.\n\n"
                    + describeFilters(state)
                    + "\n\nНапиши /filters или /search.";

        } catch (NumberFormatException e) {
            return "Не удалось прочитать цену. Примеры: до 900, от 1000, 600-1200.";
        }
    }

    private String search(FilterState state) {
        Filter filter = new CompositeFilter(
                new DishFilter(state.dish),
                new CuisineFilter(state.cuisine),
                new PriceFilter(state.minCheck, state.maxCheck)
        );

        List<Restaurant> found = service.findByFilter(filter);

        String resultMessage = restaurantHandler.formatRestaurants(
                found,
                "Результаты поиска\n" + describeFilters(state)
        );

        return resultMessage + """


            Что будем делать дальше?
            
            /filters — вернуться к меню фильтров
            """;
    }

    private String describeFilters(FilterState state) {
        return "Кухня: "
                + (state.cuisine == null ? "любая" : state.cuisine)
                + "\nБлюдо: "
                + (state.dish == null ? "любое" : state.dish)
                + "\nСредний чек: "
                + (state.minCheck == 0 && state.maxCheck == 0
                ? "любой"
                : (state.minCheck > 0 ? "от " + state.minCheck : "без минимума")
                + (state.maxCheck > 0 ? " до " + state.maxCheck : ""));
    }

    private String getMenu(FilterState state, boolean unused) {
        return getMenu(state);
    }

    private static class FilterState {
        private String cuisine;
        private String dish;
        private int minCheck;
        private int maxCheck;
        private String pendingAction = "";

        private void reset() {
            cuisine = null;
            dish = null;
            minCheck = 0;
            maxCheck = 0;
            pendingAction = "";
        }
    }
}
