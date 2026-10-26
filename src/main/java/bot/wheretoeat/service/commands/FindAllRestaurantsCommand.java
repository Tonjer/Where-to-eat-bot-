package bot.wheretoeat.service.commands;

import bot.wheretoeat.model.Restaurant;
import bot.wheretoeat.service.RestaurantService;

import java.util.List;

public class FindAllRestaurantsCommand implements Command<List<Restaurant>> {
    private final RestaurantService service;
    public FindAllRestaurantsCommand(RestaurantService service){
        this.service = service;
    }

    @Override
    public List<Restaurant> execute(){
        return service.findAll();
    }
}
