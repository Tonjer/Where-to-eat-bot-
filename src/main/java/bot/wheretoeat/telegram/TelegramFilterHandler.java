
package bot.wheretoeat.telegram;

import bot.wheretoeat.filter.CompositeFilter;
import bot.wheretoeat.filter.CuisineFilter;
import bot.wheretoeat.filter.DishFilter;
import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.filter.PriceFilter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.service.commands.GetAllCuisinesCommand;
import bot.wheretoeat.service.commands.GetAllDishesCommand;
import bot.wheretoeat.service.commands.FindRestaurantsByFilterCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramFilterHandler {

    private final RestaurantService service;
    private final TelegramRestaurantHandler restaurantHandler;

    // Состояние фильтров отдельно для каждого чата.
    private final Map<Long, FilterState> states = new HashMap<>();
    private final Map<Long, List<Restaurant>> lastSearchResults = new HashMap<>();

    public List<Restaurant> getLastSearchResults(long chatId) {
        return lastSearchResults.getOrDefault(chatId, List.of());
    }

    public TelegramFilterHandler(
            RestaurantService service,
            TelegramRestaurantHandler restaurantHandler
    ) {
        this.service = service;
        this.restaurantHandler = restaurantHandler;
    }

    // TODO(refactor P1): После общего разбора команды не сравнивать повторно исходный text с /command.
    // Использовать общую пользовательскую сессию; не держать отдельные states/lastSearchResults.
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
                    new GetAllCuisinesCommand(service).execute()
            );
        }

        if (input.equalsIgnoreCase("/dish")) {
            state.pendingAction = "dish";
            return getNumberedList(
                    "🍽 Выбери блюдо, отправив его номер:",
                    new GetAllDishesCommand(service).execute()
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
            return search(chatId, state);
        }

        if (state.pendingAction.equals("cuisine")) {
            return selectFromList(
                    state,
                    input,
                    new GetAllCuisinesCommand(service).execute(),
                    "cuisine"
            );
        }

        if (state.pendingAction.equals("dish")) {
            return selectFromList(
                    state,
                    input,
                    new GetAllDishesCommand(service).execute(),
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

    // TODO(refactor P2): Выделить небольшой парсер диапазона: сначала полностью проверить ввод,
    // затем менять фильтры. Отличать отсутствие границы от 0; неверный диапазон не сохранять.
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

    private String search(long chatId, FilterState state) {
        List<Restaurant> found = service.search(state.cuisine, state.dish, state.minCheck, state.maxCheck);

        lastSearchResults.put(chatId, List.copyOf(found));

        if (found.isEmpty()) {
            return "По заданным условиям заведений не найдено.\n\n"
                    + "Что будем делать дальше?\n"
                    + "/filters — вернуться к меню фильтров";
        }

        return "🔎 Результаты поиска\n"
                + describeFilters(state)
                + "\n\nНайдено заведений: " + found.size()
                + "\n\nНиже — найденные рестораны. "
                + "У каждого можно оставить отзыв.\n\n"
                + "/filters — вернуться к меню фильтров";
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

    // TODO(refactor P2): Удалить этот неиспользуемый overload и зависимость restaurantHandler;
    // в основном меню показывать текущие фильтры через существующий describeFilters.
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
