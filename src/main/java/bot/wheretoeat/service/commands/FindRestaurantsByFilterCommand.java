package bot.wheretoeat.service.commands;

import bot.wheretoeat.filter.Filter;
import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantService;

import java.util.List;

public class FindRestaurantsByFilterCommand implements Command<List<Restaurant>> {
    private final RestaurantService service;
    private final Filter filter;

    public FindRestaurantsByFilterCommand(RestaurantService service, Filter filter) {
        this.filter = filter;
        this.service = service;
    }

    @Override
    public List<Restaurant> execute() {
        return service.findByFilter(filter);
    }
}
