package bot.wheretoeat.filter;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.util.StringUtilities;

public class CuisineFilter implements Filter {

    private final String cuisine;

    public CuisineFilter(String cuisine) {
        this.cuisine = StringUtilities.trimToNull(cuisine);
    }

    @Override
    public boolean matches(Restaurant restaurant) {
        if (cuisine == null) {
            return true;
        }
        return StringUtilities.containsIgnoreCase(restaurant.getCuisine(), cuisine);
    }
}