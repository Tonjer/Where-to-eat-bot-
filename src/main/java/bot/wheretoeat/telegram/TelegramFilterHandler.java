package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.service.commands.GetAllCuisinesCommand;
import bot.wheretoeat.service.commands.GetAllDishesCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class TelegramFilterHandler {

    private final RestaurantService service;
    private final TelegramRestaurantHandler restaurantHandler;

    public TelegramFilterHandler(
            RestaurantService service,
            TelegramRestaurantHandler restaurantHandler
    ) {
        this.service = service;
        this.restaurantHandler = restaurantHandler;
    }

    public BotResponse handleCommand(UserSession session, ParsedCommand command) {
        String cmd = command.getCommand();

        switch (cmd) {
            case "/filters":
                session.setStep(UserSession.Step.NONE);
                return BotResponse.ofText(getMenu(session));

            case "/reset":
                session.resetFilters();
                return BotResponse.ofText("🧹 Все фильтры сброшены.\n\n" + getMenu(session));

            case "/cuisine":
                session.setStep(UserSession.Step.FILTER_CUISINE);
                return BotResponse.ofText(getNumberedList(
                        "🌍 Выбери кухню, отправив её номер:",
                        new GetAllCuisinesCommand(service).execute()
                ));

            case "/dish":
                session.setStep(UserSession.Step.FILTER_DISH);
                return BotResponse.ofText(getNumberedList(
                        "🍽 Выбери блюдо, отправив его номер:",
                        new GetAllDishesCommand(service).execute()
                ));

            case "/price":
                session.setStep(UserSession.Step.FILTER_PRICE);
                return BotResponse.ofText("""
                        💰 Укажи диапазон среднего чека.

                        Примеры:
                        до 900
                        от 1000
                        600-1200

                        Чтобы отменить выбор, напиши /cancel.
                        """);

            case "/search":
                session.setStep(UserSession.Step.NONE);
                return search(session);

            default:
                return BotResponse.ofText("""
                        Не понял запрос.

                        Напиши /filters, чтобы выбрать фильтры,
                        или /help, чтобы увидеть все команды.
                        """);
        }
    }

    public BotResponse handleStepInput(UserSession session, String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            return BotResponse.ofText("Напиши /filters, чтобы открыть меню фильтров.");
        }

        if (session.getStep() == UserSession.Step.FILTER_CUISINE) {
            return BotResponse.ofText(selectFromList(
                    session,
                    input,
                    new GetAllCuisinesCommand(service).execute(),
                    "cuisine"
            ));
        }

        if (session.getStep() == UserSession.Step.FILTER_DISH) {
            return BotResponse.ofText(selectFromList(
                    session,
                    input,
                    new GetAllDishesCommand(service).execute(),
                    "dish"
            ));
        }

        if (session.getStep() == UserSession.Step.FILTER_PRICE) {
            return BotResponse.ofText(setPrice(session, input));
        }

        return BotResponse.ofText("""
                Не понял запрос.

                Напиши /filters, чтобы выбрать фильтры,
                или /help, чтобы увидеть все команды.
                """);
    }

    private String getMenu(UserSession session) {
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

    private String getNumberedList(String title, List<String> values) {
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
            UserSession session,
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
                session.setCuisine(selected);
            } else {
                session.setDish(selected);
            }

            session.setStep(UserSession.Step.NONE);

            return "Выбрано: " + selected
                    + "\n\nТекущие фильтры:\n"
                    + describeFilters(session)
                    + "\n\nНапиши /filters, чтобы продолжить, "
                    + "или /search для поиска.";

        } catch (NumberFormatException e) {
            return "Отправь номер из списка или напиши /cancel.";
        }
    }

    // TODO(refactor P2): Выделить небольшой парсер диапазона: сначала полностью проверить ввод,
    // затем менять фильтры. Отличать отсутствие границы от 0; неверный диапазон не сохранять.
    private String setPrice(UserSession session, String input) {
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

            session.setMinCheck(min);
            session.setMaxCheck(max);
            session.setStep(UserSession.Step.NONE);

            return "Диапазон среднего чека сохранён.\n\n"
                    + describeFilters(session)
                    + "\n\nНапиши /filters или /search.";

        } catch (NumberFormatException e) {
            return "Не удалось прочитать цену. Примеры: до 900, от 1000, 600-1200.";
        }
    }

    private BotResponse search(UserSession session) {
        List<Restaurant> found = service.search(
                session.getCuisine(),
                session.getDish(),
                session.getMinCheck(),
                session.getMaxCheck()
        );

        session.setLastSearchResults(found);

        if (found.isEmpty()) {
            return BotResponse.ofText("По заданным условиям заведений не найдено.\n\n"
                    + "Что будем делать дальше?\n"
                    + "/filters — вернуться к меню фильтров");
        }

        List<BotResponse.OutgoingMessage> messages = new ArrayList<>();
        messages.add(new BotResponse.OutgoingMessage(
                "🔎 Результаты поиска\n"
                        + describeFilters(session)
                        + "\n\nНайдено заведений: " + found.size()
                        + "\n\nНиже — найденные рестораны. "
                        + "У каждого можно оставить отзыв.\n\n"
                        + "/filters — вернуться к меню фильтров"
        ));

        for (int i = 0; i < found.size(); i++) {
            Restaurant restaurant = found.get(i);
            String text = (i + 1)
                    + ". " + restaurant.getName() + "\n"
                    + "📍 Адрес: " + restaurant.getAddress() + "\n"
                    + "🌍 Кухня: " + restaurant.getCuisine() + "\n"
                    + "🍽 Блюда: " + String.join(", ", restaurant.getDishes()) + "\n"
                    + "💰 Средний чек: " + restaurant.getAverageCheck() + " ₽";

            InlineKeyboardButton reviewButton = new InlineKeyboardButton("✍️ Оставить отзыв");
            reviewButton.setCallbackData("restaurant_review:" + restaurant.getId());

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(List.of(List.of(reviewButton)));

            messages.add(new BotResponse.OutgoingMessage(text, keyboard));
        }

        return BotResponse.ofMessages(messages);
    }

    private String describeFilters(UserSession session) {
        return "Кухня: "
                + (session.getCuisine() == null ? "любая" : session.getCuisine())
                + "\nБлюдо: "
                + (session.getDish() == null ? "любое" : session.getDish())
                + "\nСредний чек: "
                + (session.getMinCheck() == 0 && session.getMaxCheck() == 0
                ? "любой"
                : (session.getMinCheck() > 0 ? "от " + session.getMinCheck() : "без минимума")
                + (session.getMaxCheck() > 0 ? " до " + session.getMaxCheck() : ""));
    }

    // TODO(refactor P2): Удалить этот неиспользуемый overload и зависимость restaurantHandler;
    // в основном меню показывать текущие фильтры через существующий describeFilters.
    private String getMenu(UserSession session, boolean unused) {
        return getMenu(session);
    }
}