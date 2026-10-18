package bot.wheretoeat.filter;

import bot.wheretoeat.model.Restaurant;

@FunctionalInterface
public interface Filter {

    boolean matches(Restaurant restaurant);
}