
package bot.wheretoeat.telegram;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantService;

import java.util.List;
import bot.wheretoeat.service.commands.FindAllRestaurantsCommand;

public class TelegramRestaurantHandler {

    private static final int MAX_MESSAGE_LENGTH = 3500;

    private final RestaurantService service;

    public TelegramRestaurantHandler(RestaurantService service) {
        this.service = service;
    }

    public String getAllRestaurantsMessage() {
        return formatRestaurants(new FindAllRestaurantsCommand(service).execute(), "Список заведений");
    }

    public String formatRestaurants(
            List<Restaurant> restaurants,
            String title
    ) {
        if (restaurants == null || restaurants.isEmpty()) {
            return "По заданным условиям заведений не найдено.";
        }

        StringBuilder result = new StringBuilder();
        result.append(title).append(":\n\n");

        int count = 0;

        for (Restaurant restaurant : restaurants) {
            String text = (count + 1) + ". "
                    + restaurant.getName() + "\n"
                    + "📍 Адрес: " + restaurant.getAddress() + "\n"
                    + "🌍 Кухня: " + restaurant.getCuisine() + "\n"
                    + "🍽 Блюда: "
                    + String.join(", ", restaurant.getDishes()) + "\n"
                    + "💰 Средний чек: "
                    + restaurant.getAverageCheck() + " ₽\n\n";

            if (result.length() + text.length() > MAX_MESSAGE_LENGTH) {
                result.append(
                        "Список сокращён: не все заведения "
                                + "поместились в одно сообщение."
                );
                break;
            }

            result.append(text);
            count++;
        }

        return result.toString();
    }
}
