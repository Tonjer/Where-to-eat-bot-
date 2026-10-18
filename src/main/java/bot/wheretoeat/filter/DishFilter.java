package bot.wheretoeat.filter;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.util.StringUtilities;

public class DishFilter implements Filter {

    private final String dish;

    public DishFilter(String dish) {
        this.dish = StringUtilities.trimToNull(dish);
    }

    @Override
    public boolean matches(Restaurant restaurant) {
        if (dish == null) {
            return true;
        }
        return restaurant.getDishes().stream().anyMatch(d -> StringUtilities.containsIgnoreCase(d, dish));
    }
}